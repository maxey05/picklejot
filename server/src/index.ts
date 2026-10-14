// Purchase verifier for Picklejot Pro. One endpoint: POST /v1/verify.
//
// The app sends a Play purchase token plus a Play Integrity token whose nonce is bound to that
// purchase token and a fresh client nonce. This worker (1) decodes the integrity verdict with
// Google, (2) asks the Play Developer API whether the purchase is real, and (3) answers with an
// ECDSA-signed statement that echoes the client nonce, so a recorded answer cannot be replayed.
//
// Status codes are the contract the app relies on: 200 = signed answer, other 4xx = rejected
// (the app grants nothing), 5xx = could not check (the app changes nothing — Pro is never
// revoked by a failure, R58).

export interface Env {
  PACKAGE_NAME: string;
  PRODUCT_ID: string;
  GOOGLE_CLIENT_EMAIL: string;
  GOOGLE_PRIVATE_KEY: string; // service-account key, PKCS#8 PEM
  SIGNING_PRIVATE_KEY: string; // base64 PKCS#8, ECDSA P-256 (see scripts/gen-signing-key.mjs)
}

type Fetch = typeof fetch;

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

// null = Google says the token itself is bad (malformed, expired, wrong app) -> reject.
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

export async function handle(req: Request, env: Env, f: Fetch = fetch, nowMs = Date.now()): Promise<Response> {
  if (req.method !== "POST" || new URL(req.url).pathname !== "/v1/verify") return json({ error: "not_found" }, 404);

  let body: { purchaseToken?: unknown; clientNonce?: unknown; integrityToken?: unknown };
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad_request" }, 400);
  }
  const { purchaseToken, clientNonce, integrityToken } = body;
  if (
    typeof purchaseToken !== "string" || purchaseToken.length < 1 || purchaseToken.length > 2048 ||
    typeof clientNonce !== "string" || !/^[A-Za-z0-9_-]{16,128}$/.test(clientNonce) ||
    typeof integrityToken !== "string" || integrityToken.length > 32768 || integrityToken.split(".").length !== 5
  ) {
    return json({ error: "bad_request" }, 400);
  }

  try {
    const auth = await googleAccessToken(env, f, nowMs);
    // Cheapest check first: random tokens must not be able to burn the Play Integrity quota.
    const state = await purchaseState(env, f, auth, purchaseToken);
    if (state !== 0 && state !== 1) return json({ error: "not_purchased" }, 403);

    const verdict = await decodeIntegrity(env, f, auth, integrityToken);
    const nonce = await sha256Nonce(purchaseToken, clientNonce);
    if (!verdict || !integrityOk(verdict, env, nonce, nowMs)) return json({ error: "integrity" }, 403);

    return await signed(env, {
      status: state === 0 ? "granted" : "refunded",
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
