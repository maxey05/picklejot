import { readFileSync, writeFileSync, mkdirSync, copyFileSync, existsSync, statSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const here = dirname(fileURLToPath(import.meta.url));
const ASSETS = ["style.css", "favicon.svg", "baloo2-extrabold.woff2"];
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

const DEV_NAME = "Matthew Gabriel D. Reyes";
const DEV_PLACE = "Parañaque City, Philippines";
const DEV_EMAIL = "emgireyes.dev@gmail.com";
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

const DUCK_INNER = "<defs><linearGradient id=\"pj-duck\" x1=\"0\" y1=\"38\" x2=\"0\" y2=\"170\" gradientUnits=\"userSpaceOnUse\"><stop offset=\"0.35\" stop-color=\"#FEF4E0\"></stop><stop offset=\"1\" stop-color=\"#F8DFB8\"></stop></linearGradient></defs><path d=\"M92 42 C88 28 92 18 100 13 C99 22 102 28 106 32 C108 24 115 19 124 19 C117 25 115 33 114 42 Z\" fill=\"#FEF4E0\" stroke=\"#14231C\" stroke-width=\"3.5\" stroke-linejoin=\"round\"></path><ellipse cx=\"100\" cy=\"104\" rx=\"78\" ry=\"66\" fill=\"url(#pj-duck)\"></ellipse><path d=\"M31.8 72 L168.2 72 L175.7 88 L24.3 88 Z\" fill=\"#008148\"></path><path d=\"M29.5 77 L170.5 77\" stroke=\"#FFFFFF\" stroke-width=\"2.6\"></path><path d=\"M27 83 L173 83\" stroke=\"#CDEBD4\" stroke-width=\"2.6\"></path><path d=\"M31.8 72 L168.2 72 M24.3 88 L175.7 88\" stroke=\"#14231C\" stroke-width=\"3.5\"></path><ellipse cx=\"100\" cy=\"104\" rx=\"78\" ry=\"66\" fill=\"none\" stroke=\"#14231C\" stroke-width=\"6\"></ellipse><circle cx=\"100\" cy=\"80\" r=\"10\" fill=\"#E2F23F\" stroke=\"#14231C\" stroke-width=\"3.5\"></circle><circle cx=\"96\" cy=\"77.5\" r=\"1.9\" fill=\"#A9BA22\"></circle><circle cx=\"104\" cy=\"78\" r=\"1.9\" fill=\"#A9BA22\"></circle><circle cx=\"100\" cy=\"84\" r=\"1.9\" fill=\"#A9BA22\"></circle><ellipse cx=\"52\" cy=\"126\" rx=\"13\" ry=\"8\" fill=\"#FF9D96\" opacity=\"0.85\"></ellipse><ellipse cx=\"148\" cy=\"126\" rx=\"13\" ry=\"8\" fill=\"#FF9D96\" opacity=\"0.85\"></ellipse><ellipse cx=\"78\" cy=\"108\" rx=\"6.5\" ry=\"8.5\" fill=\"#14231C\"></ellipse><circle cx=\"76\" cy=\"105\" r=\"2.4\" fill=\"#FFFFFF\"></circle><ellipse cx=\"122\" cy=\"108\" rx=\"6.5\" ry=\"8.5\" fill=\"#14231C\"></ellipse><circle cx=\"120\" cy=\"105\" r=\"2.4\" fill=\"#FFFFFF\"></circle><path d=\"M86 131 Q100 156 114 131 Z\" fill=\"#9B2C1F\" stroke=\"#14231C\" stroke-width=\"3.5\" stroke-linejoin=\"round\"></path><ellipse cx=\"100\" cy=\"140\" rx=\"5.5\" ry=\"2.8\" fill=\"#FF8A8F\"></ellipse><rect x=\"74\" y=\"118\" width=\"52\" height=\"17\" rx=\"8.5\" fill=\"#FE9A2E\" stroke=\"#14231C\" stroke-width=\"3.5\"></rect><path d=\"M82 123 L95 123\" stroke=\"#FFC680\" stroke-width=\"2.5\" stroke-linecap=\"round\"></path>";
const ICON_TICK = "<svg viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"M5 12.5l4.5 4.5L19 7.5\"></path></svg>";
const ICON_CHEV_DOWN = "<svg class=\"chev\" viewBox=\"0 0 24 24\" width=\"20\" height=\"20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"M6 9l6 6 6-6\"></path></svg>";
const ICON_CHEV_RIGHT = "<svg viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"M9 6l6 6-6 6\"></path></svg>".replace("<svg", '<svg class="row-chev"');
const ICON_SHIELD = "<svg viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"M12 3l7 3v5c0 4.5-3 8.3-7 10-4-1.7-7-5.5-7-10V6l7-3z\"></path><path d=\"M9 12l2 2 4-4\"></path></svg>";
const ICON_DOC = "<svg viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><path d=\"M7 3h7l5 5v13H7z\"></path><path d=\"M14 3v5h5\"></path><path d=\"M10 13h6M10 17h6\"></path></svg>";
const ICON_MAIL = "<svg viewBox=\"0 0 24 24\" width=\"24\" height=\"24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\"><rect x=\"3\" y=\"5\" width=\"18\" height=\"14\" rx=\"2\"></rect><path d=\"M3 7l9 6 9-6\"></path></svg>";

function duck(id, width, height) {
  const inner = DUCK_INNER.replaceAll("pj-duck", id);
  return `<svg viewBox="0 0 200 184" width="${width}" height="${height}" aria-hidden="true" focusable="false">${inner}</svg>`;
}

function longDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return escapeHtml(value);
  const text = `${Number(match[3])} ${MONTHS[Number(match[2]) - 1]} ${match[1]}`;
  return `<time datetime="${value}">${text}</time>`;
}

function tocList(entries) {
  const items = entries.map((entry) => `<li><a href="#${entry.id}">${escapeHtml(entry.text)}</a></li>`).join("");
  return `<details><summary>On this page${ICON_CHEV_DOWN}</summary><ol>${items}</ol></details>`;
}

function buildToc(headings) {
  const entries = headings.filter((h) => h.level === 2);
  if (entries.length < 2) return "";
  const wide = tocList(entries).replace("<details>", "<details open>");
  const compact = tocList(entries);
  return `<nav class="toc toc--wide" aria-label="On this page">${wide}</nav>
<nav class="toc toc--compact" aria-label="On this page">${compact}</nav>`;
}

function decorateBody(body) {
  let out = body.replace(
    /<h2 id="([^"]+)">(\d+)\.\s+([\s\S]*?)<\/h2>/g,
    '<h2 id="$1"><span class="n">$2</span><span>$3</span></h2>',
  );
  out = out.replace(/(<h2 id="at-a-glance">[\s\S]*?<\/h2>\s*)<ul>([\s\S]*?)<\/ul>/, (_, head, items) => {
    const rows = items.replace(/<li>([\s\S]*?)<\/li>/g, `<li><span class="tick">${ICON_TICK}</span><span>$1</span></li>`);
    return `${head}<ul class="glance">${rows}</ul>`;
  });
  return out;
}

function layout({ title, description, current, hero, main, mainClass, docPage }) {
  const nav = [
    ["privacy.html", "Privacy", "privacy"],
    ["terms.html", "Terms", "terms"],
  ]
    .map(([href, label, key]) => {
      const aria = key === current ? ' aria-current="page"' : "";
      return `<li><a class="chip-link" href="${href}"${aria}>${label}</a></li>`;
    })
    .join("");
  const year = new Date().getFullYear();

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'self'; img-src 'self'; font-src 'self'; base-uri 'none'; form-action 'none'">
<meta name="referrer" content="no-referrer">
<meta name="color-scheme" content="light dark">
<title>${escapeHtml(title)}</title>
<meta name="description" content="${escapeHtml(description)}">
<link rel="icon" href="favicon.svg" type="image/svg+xml">
<link rel="stylesheet" href="style.css">
</head>
<body>
<a class="skip-link" href="#content">Skip to content</a>
<header class="hd${docPage ? " hd--doc" : ""}">
<div class="hd-inner">
<div class="topbar">
<a class="brand" href="index.html" aria-label="Picklejot home">${duck("pj-duck-brand", 44, 40)}<span>Picklejot</span></a>
<nav aria-label="Site"><ul class="chips">${nav}</ul></nav>
</div>
${hero}
</div>
</header>
<main id="content" tabindex="-1" class="${mainClass}">
${main}
</main>
<footer class="site-footer">
<div class="site-footer-inner">
<span>© ${year} ${DEV_NAME} · ${DEV_PLACE}</span>
<nav aria-label="Footer"><ul><li><a href="index.html">Home</a></li><li><a href="privacy.html">Privacy policy</a></li><li><a href="terms.html">Terms of service</a></li><li><a href="mailto:${DEV_EMAIL}">Contact</a></li></ul></nav>
</div>
</footer>
</body>
</html>
`;
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

function buildDocument({ path, label, fallbackTitle, heading, lede, current, description }) {
  const source = readSource(path, label);
  const result = convertMarkdown(source);
  const title = result.title || fallbackTitle;
  let updated = result.lastUpdated;
  if (!updated) {
    updated = statSync(path).mtime.toISOString().slice(0, 10);
    console.warn(`No "Last updated" line found in ${path}; using file date ${updated}.`);
  }
  if (result.headings.length === 0) console.warn(`No section headings found in ${path}.`);

  const hero = `<div class="doc-head">
<h1 class="hero-title">${escapeHtml(heading)}</h1>
<p class="lede">${escapeHtml(lede)}</p>
<ul class="chip-row"><li class="chip">Last updated ${longDate(updated)}</li></ul>
</div>`;

  const main = [buildToc(result.headings), `<article class="doc">`, decorateBody(result.body), `</article>`]
    .filter(Boolean)
    .join("\n");

  const placeholders = [...new Set(source.match(/\[[A-Z][A-Z0-9 \/_.-]{2,}\]/g) ?? [])];
  return {
    html: layout({
      title: /picklejot/i.test(title) ? title : `${title} - Picklejot`,
      description,
      current,
      hero,
      main,
      mainClass: "doc-main",
      docPage: true,
    }),
    placeholders,
  };
}

function row({ href, icon, title, sub }) {
  return `<li><a class="row" href="${href}"><span class="row-icon">${icon}</span><span class="row-text"><span class="row-title">${title}</span><span class="row-sub">${sub}</span></span>${ICON_CHEV_RIGHT}</a></li>`;
}

function buildIndex() {
  const store = playUrl
    ? `<a class="cta" href="${escapeHtml(playUrl)}" rel="noopener noreferrer">Get Picklejot on Google Play</a>`
    : "";
  const hero = `<div class="hero">
${duck("pj-duck-hero", 152, 140)}
<div class="hero-text">
<h1 class="hero-title">Picklejot</h1>
<p class="lede">A pickleball match journal for Android. Log a match in seconds, keep your streak going, and keep every match on your own phone.</p>
<ul class="chip-row"><li class="chip">No account</li><li class="chip">No ads</li><li class="chip">Works offline</li></ul>
${store}
</div>
</div>`;
  const main = `<h2 class="label label--first">Legal</h2>
<ul class="list">
${row({ href: "privacy.html", icon: ICON_SHIELD, title: "Privacy policy", sub: "What Picklejot stores on your phone, and the one purchase check it sends" })}
${row({ href: "terms.html", icon: ICON_DOC, title: "Terms of service", sub: "The rules for using the app and the one-time Pro purchase" })}
</ul>
<h2 class="label label--gap">Contact</h2>
<ul class="list">
${row({ href: `mailto:${DEV_EMAIL}`, icon: ICON_MAIL, title: "Email the developer", sub: DEV_EMAIL })}
</ul>
<p class="note">Picklejot is an Android app made by one developer in ${DEV_PLACE}.</p>`;
  return layout({
    title: "Picklejot - Pickleball match log",
    description: "Picklejot is a simple, private pickleball match log for Android. Read the privacy policy and terms of service.",
    current: "home",
    hero,
    main,
    mainClass: "home-main",
    docPage: false,
  });
}

const privacy = buildDocument({
  path: privacyPath,
  label: "privacy policy",
  fallbackTitle: "Privacy Policy",
  heading: "Privacy policy",
  lede: "How Picklejot, an Android app for logging your pickleball matches, handles information.",
  current: "privacy",
  description: "Picklejot privacy policy: what the app stores on your device, what it shares, and your rights.",
});
const terms = buildDocument({
  path: termsPath,
  label: "terms of service",
  fallbackTitle: "Terms of Service",
  heading: "Terms of service",
  lede: "The agreement between you and the developer about using Picklejot for Android. By installing or using the app, you agree to these terms.",
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
