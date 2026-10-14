import { readFileSync, writeFileSync, mkdirSync, copyFileSync, existsSync, statSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const here = dirname(fileURLToPath(import.meta.url));
const ASSETS = ["style.css", "favicon.svg"];
const RESERVED_IDS = new Set(["top", "content", "toc", "main"]);
const PAGE_ALIASES = {
  "privacy-policy.html": "privacy.html",
  "privacy-policy.md": "privacy.html",
  "terms-of-service.html": "terms.html",
  "terms-of-service.md": "terms.html",
};

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    privacy: { type: "string" },
    terms: { type: "string" },
    out: { type: "string" },
    "play-url": { type: "string" },
    strict: { type: "boolean", default: false },
    help: { type: "boolean", default: false },
  },
});

if (values.help) {
  console.log(
    [
      "Usage: node build.mjs [privacy.md] [terms.md] [options]",
      "  --privacy <file>   privacy policy markdown (default ../legal/privacy-policy.md)",
      "  --terms <file>     terms of service markdown (default ../legal/terms-of-service.md)",
      "  --out <dir>        output directory (default ./site)",
      "  --play-url <url>   optional Google Play listing URL shown on the landing page",
      "  --strict           exit with an error if [PLACEHOLDERS] remain in the legal text",
    ].join("\n"),
  );
  process.exit(0);
}

const privacyPath = resolve(values.privacy ?? positionals[0] ?? join(here, "..", "legal", "privacy-policy.md"));
const termsPath = resolve(values.terms ?? positionals[1] ?? join(here, "..", "legal", "terms-of-service.md"));
const outDir = resolve(values.out ?? join(here, "site"));
const assetDir = join(here, "site");
const playUrl = values["play-url"] ?? "";

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function safeUrl(raw) {
  const url = raw.replace(/[\u0000- \u007f]/g, "");
  if (/^[a-z][a-z0-9+.-]*:/i.test(url)) {
    return /^(https?|mailto|tel):/i.test(url) ? url : null;
  }
  const aliased = /^(?:\.\/)?([^#?]+)([#?].*)?$/.exec(url);
  if (aliased && PAGE_ALIASES[aliased[1]]) return PAGE_ALIASES[aliased[1]] + (aliased[2] ?? "");
  return url;
}

function linkHtml(href, innerHtml, title) {
  const external = /^https?:/i.test(href);
  const rel = external ? ' rel="noopener noreferrer"' : "";
  const titleAttr = title ? ` title="${escapeHtml(title)}"` : "";
  return `<a href="${escapeHtml(href)}"${titleAttr}${rel}>${innerHtml}</a>`;
}

function renderInline(source, stash = [], top = true) {
  const keep = (html) => {
    stash.push(html);
    return `\u0000${stash.length - 1}\u0000`;
  };
  let text = top ? source.replace(/\u0000/g, "") : source;

  text = text.replace(/(`+)([\s\S]*?[^`])\1(?!`)/g, (_, _ticks, code) => keep(`<code>${escapeHtml(code.trim())}</code>`));
  text = text.replace(/(?: {2,}|\\)\n/g, () => keep("<br>\n"));
  text = text.replace(/\\([\\`*_{}\[\]()#+\-.!|>~])/g, (_, ch) => keep(escapeHtml(ch)));

  text = text.replace(
    /\[((?:[^\[\]]|\[[^\[\]]*\])*)\]\(\s*((?:[^\s()]|\([^\s()]*\))+)(?:\s+"([^"]*)")?\s*\)/g,
    (_, label, url, title) => {
      const href = safeUrl(url);
      const inner = renderInline(label, stash, false);
      return keep(href === null ? inner : linkHtml(href, inner, title));
    },
  );

  text = text.replace(/<((?:https?:\/\/|mailto:)[^\s<>]+)>/gi, (_, url) => keep(linkHtml(url, escapeHtml(url))));
  text = text.replace(/<([^\s<>@]+@[^\s<>@]+\.[^\s<>@]+)>/g, (_, mail) =>
    keep(linkHtml(`mailto:${mail}`, escapeHtml(mail))),
  );
  text = text.replace(/(^|[\s(])(https?:\/\/[^\s<>()]*[^\s<>().,;:!?'"])/g, (_, lead, url) =>
    lead + keep(linkHtml(url, escapeHtml(url))),
  );

  text = escapeHtml(text);
  text = text.replace(/\*\*(?=\S)([\s\S]*?\S)\*\*/g, "<strong>$1</strong>");
  text = text.replace(/(^|[^\w])__(?=\S)([\s\S]*?\S)__(?!\w)/g, "$1<strong>$2</strong>");
  text = text.replace(/\*(?=\S)([^*]*?\S)\*/g, "<em>$1</em>");
  text = text.replace(/(^|[^\w])_(?=\S)([^_]*?\S)_(?!\w)/g, "$1<em>$2</em>");

  if (!top) return text;

  let previous;
  do {
    previous = text;
    text = text.replace(/\u0000(\d+)\u0000/g, (_, index) => stash[Number(index)]);
  } while (text !== previous);
  return text;
}

function plainHeadingText(raw) {
  return raw
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/[`*_]/g, "")
    .replace(/\\(.)/g, "$1")
    .trim();
}

function slugify(raw) {
  const slug = plainHeadingText(raw)
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[^\p{L}\p{N}\s_-]/gu, "")
    .trim()
    .replace(/\s/g, "-");
  return slug || "section";
}

const FENCE_RE = /^\s{0,3}(```+|~~~+)/;
const HEADING_RE = /^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$/;
const HR_RE = /^\s{0,3}([-*_])(?:\s*\1){2,}\s*$/;
const QUOTE_RE = /^\s{0,3}>\s?/;
const LIST_RE = /^(\s*)([-*+]|\d{1,9}[.)])\s+(.*)$/;
const TABLE_SEP_RE = /^\s*\|?\s*:?-+:?\s*(?:\|\s*:?-+:?\s*)*\|?\s*$/;

function splitRow(line) {
  let row = line.trim();
  if (row.startsWith("|")) row = row.slice(1);
  if (row.endsWith("|") && !row.endsWith("\\|")) row = row.slice(0, -1);
  return row.split(/(?<!\\)\|/).map((cell) => cell.trim());
}

function isBlank(line) {
  return line.trim() === "";
}

function startsBlock(lines, i) {
  const line = lines[i];
  if (FENCE_RE.test(line) || HEADING_RE.test(line) || HR_RE.test(line) || QUOTE_RE.test(line)) return true;
  const item = LIST_RE.exec(line);
  if (item && (/^[-*+]$/.test(item[2]) || /^1[.)]$/.test(item[2]))) return true;
  return isTableStart(lines, i);
}

function isTableStart(lines, i) {
  if (i + 1 >= lines.length) return false;
  if (!lines[i].includes("|") || !lines[i + 1].includes("|") || !TABLE_SEP_RE.test(lines[i + 1])) return false;
  return splitRow(lines[i]).length === splitRow(lines[i + 1]).length;
}

function parseBlocks(lines, state, tight = false) {
  const html = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    if (isBlank(line)) {
      i += 1;
      continue;
    }

    const fence = FENCE_RE.exec(line);
    if (fence) {
      const marker = fence[1][0];
      const body = [];
      i += 1;
      while (i < lines.length && !new RegExp(`^\\s{0,3}${marker}{${fence[1].length},}\\s*$`).test(lines[i])) {
        body.push(lines[i]);
        i += 1;
      }
      i += 1;
      html.push(`<pre><code>${escapeHtml(body.join("\n"))}</code></pre>`);
      continue;
    }

    const heading = HEADING_RE.exec(line);
    if (heading) {
      html.push(state.heading(heading[1].length, heading[2]));
      i += 1;
      continue;
    }

    if (HR_RE.test(line)) {
      html.push("<hr>");
      i += 1;
      continue;
    }

    if (QUOTE_RE.test(line)) {
      const inner = [];
      while (i < lines.length && QUOTE_RE.test(lines[i])) {
        inner.push(lines[i].replace(QUOTE_RE, ""));
        i += 1;
      }
      html.push(`<blockquote>${parseBlocks(inner, state)}</blockquote>`);
      continue;
    }

    if (isTableStart(lines, i)) {
      const header = splitRow(lines[i]);
      const aligns = splitRow(lines[i + 1]).map((cell) => {
        const left = cell.startsWith(":");
        const right = cell.endsWith(":");
        if (left && right) return "align-center";
        if (right) return "align-right";
        return "";
      });
      i += 2;
      const rows = [];
      while (i < lines.length && !isBlank(lines[i]) && lines[i].includes("|")) {
        rows.push(splitRow(lines[i]));
        i += 1;
      }
      const cell = (tag, content, index) => {
        const cls = aligns[index] ? ` class="${aligns[index]}"` : "";
        const scope = tag === "th" ? ' scope="col"' : "";
        return `<${tag}${scope}${cls}>${renderInline(content ?? "")}</${tag}>`;
      };
      const head = `<thead><tr>${header.map((c, n) => cell("th", c, n)).join("")}</tr></thead>`;
      const body = rows
        .map((row) => `<tr>${header.map((_, n) => cell("td", row[n], n)).join("")}</tr>`)
        .join("");
      html.push(`<div class="table-wrap"><table>${head}<tbody>${body}</tbody></table></div>`);
      continue;
    }

    const item = LIST_RE.exec(line);
    if (item) {
      const result = parseList(lines, i, state);
      html.push(result.html);
      i = result.next;
      continue;
    }

    const paragraph = [line];
    i += 1;
    while (i < lines.length && !isBlank(lines[i]) && !startsBlock(lines, i)) {
      paragraph.push(lines[i]);
      i += 1;
    }
    const inner = renderInline(paragraph.map((l, n) => (n === paragraph.length - 1 ? l.trimEnd() : l)).join("\n").trim());
    html.push(tight ? inner : `<p>${inner}</p>`);
  }
  return html.join("\n");
}

function parseList(lines, start, state) {
  const first = LIST_RE.exec(lines[start]);
  const baseIndent = first[1].length;
  const ordered = /\d/.test(first[2]);
  const items = [];
  let i = start;

  while (i < lines.length) {
    const match = LIST_RE.exec(lines[i]);
    if (!match || match[1].length > baseIndent + 1 || match[1].length < baseIndent) break;
    if (/\d/.test(match[2]) !== ordered) break;

    const offset = match[0].length - match[3].length;
    const itemLines = [match[3]];
    i += 1;
    let sawBlank = false;
    while (i < lines.length) {
      const current = lines[i];
      if (isBlank(current)) {
        let peek = i + 1;
        while (peek < lines.length && isBlank(lines[peek])) peek += 1;
        if (peek < lines.length && /^\s+\S/.test(lines[peek]) && lines[peek].search(/\S/) > baseIndent + 1) {
          sawBlank = true;
          itemLines.push("");
          i += 1;
          continue;
        }
        break;
      }
      const indent = current.search(/\S/);
      if (indent > baseIndent + 1) {
        itemLines.push(current.slice(Math.min(indent, offset)));
        i += 1;
        continue;
      }
      if (LIST_RE.test(current) || startsBlock(lines, i)) break;
      itemLines.push(current.trim());
      i += 1;
    }
    items.push(`<li>${parseBlocks(itemLines, state, !sawBlank)}</li>`);

    while (i < lines.length && isBlank(lines[i])) {
      const peek = lines.slice(i).findIndex((l) => !isBlank(l));
      if (peek === -1) break;
      const next = LIST_RE.exec(lines[i + peek]);
      if (next && next[1].length >= baseIndent && next[1].length <= baseIndent + 1 && /\d/.test(next[2]) === ordered) {
        i += peek;
      }
      break;
    }
  }

  const tag = ordered ? "ol" : "ul";
  const startNumber = ordered ? parseInt(first[2], 10) : 1;
  const attr = ordered && startNumber !== 1 ? ` start="${startNumber}"` : "";
  return { html: `<${tag}${attr}>${items.join("\n")}</${tag}>`, next: i };
}

function convertMarkdown(source) {
  const lines = source.replace(/\r\n?/g, "\n").replace(/^﻿/, "").split("\n");
  const headings = [];
  const usedIds = new Map();
  let title = "";
  let lastUpdated = "";

  const lastUpdatedRe = /^[\s>*_]*last\s+updated\**\s*[:–-]?\s*\**\s*(.+?)[\s*_]*$/i;
  const filtered = [];
  for (const line of lines) {
    const match = !lastUpdated ? lastUpdatedRe.exec(line) : null;
    if (match) {
      lastUpdated = match[1].trim();
    } else {
      filtered.push(line);
    }
  }

  const state = {
    heading(level, raw) {
      if (level === 1 && !title) {
        title = plainHeadingText(raw);
        return "";
      }
      const effective = level === 1 ? 2 : Math.min(level, 4);
      let id = slugify(raw);
      if (RESERVED_IDS.has(id)) id = `${id}-section`;
      const seen = usedIds.get(id) ?? 0;
      usedIds.set(id, seen + 1);
      if (seen > 0) id = `${id}-${seen}`;
      headings.push({ level: effective, id, text: plainHeadingText(raw) });
      return `<h${effective} id="${id}">${renderInline(raw)}</h${effective}>`;
    },
  };

  const body = parseBlocks(filtered, state);
  return { body, headings, title, lastUpdated };
}

function buildToc(headings) {
  const entries = headings.filter((h) => h.level <= 3);
  if (entries.length < 2) return "";
  let out = "";
  let depth = 0;
  for (const entry of entries) {
    const level = entry.level === 2 ? 1 : 2;
    if (level > depth) {
      out += "<ul>";
      depth = level;
    } else if (level < depth) {
      out += "</li></ul></li>";
      depth = level;
    } else {
      out += "</li>";
    }
    out += `<li><a href="#${entry.id}">${escapeHtml(entry.text)}</a>`;
  }
  out += "</li>";
  if (depth === 2) out += "</ul></li>";
  out += "</ul>";
  return `<details class="toc"><summary>On this page</summary><nav aria-label="Table of contents">${out}</nav></details>`;
}

const DUCK_SVG =
  '<svg viewBox="0 0 64 64" width="32" height="32" aria-hidden="true" focusable="false">' +
  '<circle cx="31" cy="33" r="26" fill="#ffd23f"/>' +
  '<ellipse cx="22" cy="39" rx="11" ry="7.5" fill="#f2b705"/>' +
  '<path d="M52 30c7 0 11 2.5 11 5.5S59 41 52 41z" fill="#f28c28"/>' +
  '<circle cx="40" cy="25" r="3.2" fill="#12281c"/></svg>';

function layout({ title, description, current, main }) {
  const nav = [
    ["index.html", "Home", "home"],
    ["privacy.html", "Privacy", "privacy"],
    ["terms.html", "Terms", "terms"],
  ]
    .map(([href, label, key]) => {
      const aria = key === current ? ' aria-current="page"' : "";
      return `<li><a href="${href}"${aria}>${label}</a></li>`;
    })
    .join("");

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'self'; img-src 'self'; base-uri 'none'; form-action 'none'">
<meta name="referrer" content="no-referrer">
<meta name="color-scheme" content="light dark">
<title>${escapeHtml(title)}</title>
<meta name="description" content="${escapeHtml(description)}">
<link rel="icon" href="favicon.svg" type="image/svg+xml">
<link rel="stylesheet" href="style.css">
</head>
<body>
<a class="skip-link" href="#content">Skip to content</a>
<header class="site-header">
<div class="wrap">
<a class="brand" href="index.html">${DUCK_SVG}<span>Picklejot</span></a>
<nav class="site-nav" aria-label="Site"><ul>${nav}</ul></nav>
</div>
</header>
<main id="content" tabindex="-1">
<div class="wrap">
${main}
</div>
</main>
<footer class="site-footer">
<div class="wrap">
<ul><li><a href="index.html">Home</a></li><li><a href="privacy.html">Privacy policy</a></li><li><a href="terms.html">Terms of service</a></li></ul>
<p>Picklejot is a pickleball match log for Android. This site uses no cookies, no analytics and no external requests.</p>
</div>
</footer>
</body>
</html>
`;
}

function formatUpdated(value) {
  const iso = /^\d{4}-\d{2}-\d{2}$/.test(value);
  return iso ? `<time datetime="${value}">${value}</time>` : escapeHtml(value);
}

function readSource(path, label) {
  if (!existsSync(path)) {
    console.error(`Missing ${label} markdown: ${path}`);
    process.exit(1);
  }
  const text = readFileSync(path, "utf8");
  if (text.trim() === "") {
    console.error(`${label} markdown is empty: ${path}`);
    process.exit(1);
  }
  return text;
}

function buildDocument({ path, label, fallbackTitle, current, description }) {
  const source = readSource(path, label);
  const result = convertMarkdown(source);
  const title = result.title || fallbackTitle;
  let updated = result.lastUpdated;
  if (!updated) {
    updated = statSync(path).mtime.toISOString().slice(0, 10);
    console.warn(`No "Last updated" line found in ${path}; using file date ${updated}.`);
  }
  if (result.headings.length === 0) console.warn(`No section headings found in ${path}.`);

  const main = [
    `<h1>${escapeHtml(title)}</h1>`,
    `<p class="page-meta">Last updated: ${formatUpdated(updated)}</p>`,
    buildToc(result.headings),
    `<article>`,
    result.body,
    `</article>`,
  ]
    .filter(Boolean)
    .join("\n");

  const placeholders = [...new Set(source.match(/\[[A-Z][A-Z0-9 \/_.-]{2,}\]/g) ?? [])];
  return {
    html: layout({ title: /picklejot/i.test(title) ? title : `${title} - Picklejot`, description, current, main }),
    placeholders,
  };
}

function buildIndex() {
  const store = playUrl
    ? `<p><a class="cta" href="${escapeHtml(playUrl)}" rel="noopener noreferrer">Get Picklejot on Google Play</a></p>`
    : "";
  const main = `<section class="hero" aria-labelledby="hero-title">
${DUCK_SVG.replace('width="32" height="32"', 'width="80" height="80"')}
<div>
<h1 id="hero-title">Picklejot</h1>
<p>A simple pickleball match log for Android. Your matches stay on your phone.</p>
${store}
</div>
</section>
<h2>Legal</h2>
<ul class="card-grid">
<li><a class="card" href="privacy.html"><strong>Privacy policy</strong><span>What Picklejot stores, what it shares, and your choices.</span></a></li>
<li><a class="card" href="terms.html"><strong>Terms of service</strong><span>The rules for using the app, Pro purchases and refunds.</span></a></li>
</ul>
<h2>In short</h2>
<ul class="promises">
<li>No account, no sign-in and no developer server.</li>
<li>No ads, no analytics and no tracking.</li>
<li>Your matches, scores and photos are stored on your device.</li>
<li>Purchases are handled by Google Play.</li>
</ul>
<p>The privacy policy is the complete and authoritative description. Contact details are listed there.</p>`;
  return layout({
    title: "Picklejot - Pickleball match log",
    description: "Picklejot is a simple, private pickleball match log for Android. Read the privacy policy and terms of service.",
    current: "home",
    main,
  });
}

const privacy = buildDocument({
  path: privacyPath,
  label: "privacy policy",
  fallbackTitle: "Privacy Policy",
  current: "privacy",
  description: "Picklejot privacy policy: what the app stores on your device, what it shares, and your rights.",
});
const terms = buildDocument({
  path: termsPath,
  label: "terms of service",
  fallbackTitle: "Terms of Service",
  current: "terms",
  description: "Picklejot terms of service: licence, Pro purchase and refunds, your data, and legal terms.",
});

const unresolved = [...new Set([...privacy.placeholders, ...terms.placeholders])];
if (unresolved.length > 0) {
  console.warn(`Unresolved placeholders: ${unresolved.join(", ")}`);
  if (values.strict) {
    console.error("Strict mode: nothing was written.");
    process.exit(1);
  }
}

mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, "privacy.html"), privacy.html);
writeFileSync(join(outDir, "terms.html"), terms.html);
writeFileSync(join(outDir, "index.html"), buildIndex());
writeFileSync(join(outDir, ".nojekyll"), "");

if (outDir !== assetDir) {
  for (const asset of ASSETS) copyFileSync(join(assetDir, asset), join(outDir, asset));
}

console.log(`Wrote index.html, privacy.html, terms.html to ${outDir}`);
