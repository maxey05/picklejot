// Purchase verifier for Picklejot Pro. One endpoint: POST /v1/verify.
//
// The app sends a Play purchase token plus (optionally) a Play Integrity token whose nonce is bound
// to that purchase token and a fresh client nonce. This worker (1) asks the Play Developer API
// whether the purchase is real, (2) decodes the integrity verdict with Google as a WARNING signal
// only, and (3) answers with an ECDSA-signed statement that echoes the client nonce, so a recorded
// answer cannot be replayed.
//
// Google's purchase confirmation is the only thing that decides a grant. Device integrity never
// blocks it: custom ROMs, uncertified devices and old Play Services still get what they paid for.
// The verdict is reported as `integrity` in the signed payload and logged.
//
// Status codes are the contract the app relies on: 200 = signed answer, 429 = rate limited and
// 5xx = could not check (the app changes nothing and retries later — Pro is never revoked by a
// failure, R58; a 429 is never a rejection), other 4xx = rejected (the app grants nothing).

export interface Env {
  PACKAGE_NAME: string;
  PRODUCT_ID: string;
  GOOGLE_CLIENT_EMAIL: string;
  GOOGLE_PRIVATE_KEY: string; // service-account key, PKCS#8 PEM
  SIGNING_PRIVATE_KEY: string; // base64 PKCS#8, ECDSA P-256 (see scripts/gen-signing-key.mjs)
  RATE_LIMITER?: RateLimit; // per client IP; wrangler.toml [[ratelimits]]; absent = unlimited
  TOKEN_LIMITER?: RateLimit; // per purchase token, so one leaked token can't burn the Play Integrity quota
}

type Fetch = typeof fetch;
type RateLimit = { limit(o: { key: string }): Promise<{ success: boolean }> };

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPES =
  "https://www.googleapis.com/auth/androidpublisher https://www.googleapis.com/auth/playintegrity";
const MAX_INTEGRITY_AGE_MS = 2 * 60_000;
const MAX_CLOCK_SKEW_MS = 10_000;

class Upstream extends Error {}

const enc = (s: string) => new TextEncoder().encode(s);
const b64 = (b: Uint8Array) => btoa(String.fromCharCode(...b));
const b64url = (b: Uint8Array) => b64(b).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const unb64 = (s: string) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0));
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

async function sha256Nonce(purchaseToken: string, clientNonce: string): Promise<string> {
  return b64url(new Uint8Array(await crypto.subtle.digest("SHA-256", enc(`${purchaseToken}\n${clientNonce}`))));
}

let cachedAuth: { email: string; token: string; expiresMs: number } | undefined;

async function googleAccessToken(env: Env, f: Fetch, nowMs: number): Promise<string> {
  if (cachedAuth && cachedAuth.email === env.GOOGLE_CLIENT_EMAIL && cachedAuth.expiresMs - 60_000 > nowMs) {
    return cachedAuth.token;
  }
  const iat = Math.floor(nowMs / 1000);
  const header = b64url(enc(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = b64url(
    enc(JSON.stringify({ iss: env.GOOGLE_CLIENT_EMAIL, scope: SCOPES, aud: TOKEN_URL, iat, exp: iat + 3600 })),
  );
  const pem = env.GOOGLE_PRIVATE_KEY.replace(/\\n/g, "\n").replace(/-----[A-Z ]+-----|\s/g, "");
  const key = await crypto.subtle.importKey(
    "pkcs8",
    unb64(pem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc(`${header}.${claims}`));
  const res = await f(TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${b64url(new Uint8Array(sig))}`,
    }),
  });
  if (!res.ok) throw new Upstream("oauth");
  const { access_token } = (await res.json()) as { access_token: string };
  cachedAuth = { email: env.GOOGLE_CLIENT_EMAIL, token: access_token, expiresMs: nowMs + 3_600_000 };
  return access_token;
}

interface IntegrityPayload {
  requestDetails?: { requestPackageName?: string; nonce?: string; timestampMillis?: string };
  appIntegrity?: { appRecognitionVerdict?: string; packageName?: string };
  deviceIntegrity?: { deviceRecognitionVerdict?: string[] };
}

// null = Google says the token itself is bad (malformed, expired, wrong app).
async function decodeIntegrity(env: Env, f: Fetch, auth: string, token: string): Promise<IntegrityPayload | null> {
  const res = await f(`https://playintegrity.googleapis.com/v1/${env.PACKAGE_NAME}:decodeIntegrityToken`, {
    method: "POST",
    headers: { authorization: `Bearer ${auth}`, "content-type": "application/json" },
    body: JSON.stringify({ integrity_token: token }),
  });
  if (res.status === 400) return null;
  if (!res.ok) throw new Upstream("integrity");
  return ((await res.json()) as { tokenPayloadExternal: IntegrityPayload }).tokenPayloadExternal;
}

function integrityOk(p: IntegrityPayload, env: Env, nonce: string, nowMs: number): boolean {
  const issuedAt = Number(p.requestDetails?.timestampMillis);
  return (
    p.requestDetails?.requestPackageName === env.PACKAGE_NAME &&
    p.requestDetails?.nonce === nonce &&
    Number.isFinite(issuedAt) &&
    nowMs - issuedAt <= MAX_INTEGRITY_AGE_MS &&
    issuedAt - nowMs <= MAX_CLOCK_SKEW_MS &&
    p.appIntegrity?.appRecognitionVerdict === "PLAY_RECOGNIZED" &&
    p.appIntegrity?.packageName === env.PACKAGE_NAME &&
    (p.deviceIntegrity?.deviceRecognitionVerdict ?? []).includes("MEETS_DEVICE_INTEGRITY")
  );
}

// Warning signal only, never a gate. ok = fully recognised; failed = token bad or verdict weak;
// missing = app sent no token; unverifiable = Google could not be asked.
export type Integrity = "ok" | "failed" | "missing" | "unverifiable";

async function checkIntegrity(
  env: Env, f: Fetch, auth: string, token: unknown, purchaseToken: string, clientNonce: string, nowMs: number,
): Promise<Integrity> {
  if (token === undefined || token === null || token === "") return "missing";
  if (typeof token !== "string" || token.length > 32768 || token.split(".").length !== 5) return "failed"; // junk never reaches Google
  try {
    const verdict = await decodeIntegrity(env, f, auth, token);
    const nonce = await sha256Nonce(purchaseToken, clientNonce);
    return verdict && integrityOk(verdict, env, nonce, nowMs) ? "ok" : "failed";
  } catch {
    return "unverifiable";
  }
}

// purchaseState: 0 purchased, 1 canceled/refunded, 2 pending. null = Google does not know this token.
async function purchaseState(env: Env, f: Fetch, auth: string, token: string): Promise<number | null> {
  const res = await f(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${env.PACKAGE_NAME}` +
      `/purchases/products/${env.PRODUCT_ID}/tokens/${encodeURIComponent(token)}`,
    { headers: { authorization: `Bearer ${auth}` } },
  );
  if ([400, 404, 410].includes(res.status)) return null;
  if (!res.ok) throw new Upstream("publisher"); // includes 401/403: our own misconfig must not reject buyers
  return ((await res.json()) as { purchaseState: number }).purchaseState;
}

// WebCrypto emits ECDSA as r||s; Java's SHA256withECDSA wants DER.
function p1363ToDer(sig: Uint8Array): Uint8Array {
  const int = (raw: Uint8Array) => {
    let i = 0;
    while (i < raw.length - 1 && raw[i] === 0) i++;
    const v = raw[i] & 0x80 ? Uint8Array.of(0, ...raw.slice(i)) : raw.slice(i);
    return Uint8Array.of(0x02, v.length, ...v);
  };
  const r = int(sig.slice(0, 32));
  const s = int(sig.slice(32));
  return Uint8Array.of(0x30, r.length + s.length, ...r, ...s);
}

async function signed(env: Env, claims: object): Promise<Response> {
  const payload = b64url(enc(JSON.stringify(claims)));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    unb64(env.SIGNING_PRIVATE_KEY),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  const raw = new Uint8Array(await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, enc(payload)));
  return json({ payload, sig: b64url(p1363ToDer(raw)) });
}

// Key on the /64 for IPv6 (one host owns 2^64 addresses); IPv4 as-is.
export function ipKey(ip: string): string {
  if (!ip.includes(":")) return ip;
  const [head, tail] = ip.toLowerCase().split("::");
  const h = head ? head.split(":") : [];
  const t = tail ? tail.split(":") : [];
  const groups = ip.includes("::") ? [...h, ...Array(Math.max(0, 8 - h.length - t.length)).fill("0"), ...t] : h;
  return groups.slice(0, 4).map((g) => g.replace(/^0+(?=.)/, "")).join(":");
}

// Cost control, not authorization: if the binding errors, fail open (the 502/Play checks still guard grants).
async function withinRateLimit(limiter: RateLimit | undefined, key: string): Promise<boolean> {
  if (!limiter) return true;
  try {
    return (await limiter.limit({ key })).success;
  } catch (e) {
    console.warn("verify: rate limiter failed, allowing", e);
    return true;
  }
}

const MAX_BODY_BYTES = 40_000; // purchaseToken <= 2048 chars + integrityToken <= 32 KB, plus JSON framing

// Reads the body but stops (null) once it passes the cap, so an oversized upload never fills the isolate's memory.
async function readCapped(req: Request): Promise<string | null> {
  if (Number(req.headers.get("content-length")) > MAX_BODY_BYTES) return null;
  const reader = req.body?.getReader();
  if (!reader) return "";
  const chunks: Uint8Array[] = [];
  let size = 0;
  for (let r = await reader.read(); !r.done; r = await reader.read()) {
    size += r.value.length;
    if (size > MAX_BODY_BYTES) {
      await reader.cancel();
      return null;
    }
    chunks.push(r.value);
  }
  const all = new Uint8Array(size);
  let at = 0;
  for (const c of chunks) (all.set(c, at), (at += c.length));
  return new TextDecoder().decode(all);
}

const rateLimited = () => {
  const res = json({ error: "rate_limited" }, 429);
  res.headers.set("Retry-After", "60");
  return res;
};

export async function handle(req: Request, env: Env, f: Fetch = fetch, nowMs = Date.now()): Promise<Response> {
  if (req.method !== "POST" || new URL(req.url).pathname !== "/v1/verify") return json({ error: "not_found" }, 404);

  // Before any body parsing, Google call or signing: those are the quota-burning paths.
  if (!(await withinRateLimit(env.RATE_LIMITER, ipKey(req.headers.get("CF-Connecting-IP") ?? "unknown")))) {
    return rateLimited();
  }

  const raw = await readCapped(req);
  if (raw === null) return json({ error: "too_large" }, 413);
  let body: { purchaseToken?: unknown; clientNonce?: unknown; integrityToken?: unknown } | null;
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: "bad_request" }, 400);
  }
  const { purchaseToken, clientNonce, integrityToken } = body ?? {};
  if (
    typeof purchaseToken !== "string" || purchaseToken.length < 1 || purchaseToken.length > 2048 ||
    typeof clientNonce !== "string" || !/^[A-Za-z0-9_-]{16,128}$/.test(clientNonce)
  ) {
    return json({ error: "bad_request" }, 400);
  }
  // After validation, before any Google call: caps repeat verification of a single (possibly leaked) token.
  if (!(await withinRateLimit(env.TOKEN_LIMITER, b64url(new Uint8Array(await crypto.subtle.digest("SHA-256", enc(purchaseToken))))))) {
    return rateLimited();
  }

  try {
    const auth = await googleAccessToken(env, f, nowMs);
    // Purchase first: random tokens must not be able to burn the Play Integrity quota.
    const state = await purchaseState(env, f, auth, purchaseToken);
    if (state !== 0 && state !== 1) return json({ error: "not_purchased" }, 403);

    const integrity = await checkIntegrity(env, f, auth, integrityToken, purchaseToken, clientNonce, nowMs);
    if (integrity !== "ok") console.warn(`verify: integrity=${integrity} state=${state} (not blocking)`);

    return await signed(env, {
      status: state === 0 ? "granted" : "refunded",
      integrity,
      purchaseToken,
      clientNonce,
      productId: env.PRODUCT_ID,
      packageName: env.PACKAGE_NAME,
    });
  } catch (e) {
    if (e instanceof Upstream) return json({ error: "upstream" }, 502);
    throw e;
  }
}

export default { fetch: (req: Request, env: Env) => handle(req, env) };
