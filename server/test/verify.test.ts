import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash, createPublicKey, generateKeyPairSync, verify } from "node:crypto";
import { handle, ipKey, type Env } from "../src/index.ts";

const NOW = 1_800_000_000_000;
const PKG = "com.maxeydev.picklejot";
const TOKEN = "purchase-token-1";
const CLIENT_NONCE = "AAAAAAAAAAAAAAAAAAAAAAAA";
const IT = "a.b.c.d.e";

const signing = generateKeyPairSync("ec", { namedCurve: "P-256" });
const google = generateKeyPairSync("rsa", { modulusLength: 2048 });
const env: Env = {
  PACKAGE_NAME: PKG,
  PRODUCT_ID: "picklejot_pro",
  GOOGLE_CLIENT_EMAIL: "sa@example.iam.gserviceaccount.com",
  GOOGLE_PRIVATE_KEY: google.privateKey.export({ type: "pkcs8", format: "pem" }).toString(),
  SIGNING_PRIVATE_KEY: signing.privateKey.export({ type: "pkcs8", format: "der" }).toString("base64"),
};

const nonceFor = (token: string, clientNonce: string) =>
  createHash("sha256").update(`${token}\n${clientNonce}`).digest("base64url");

const goodIntegrity = () => ({
  requestDetails: { requestPackageName: PKG, nonce: nonceFor(TOKEN, CLIENT_NONCE), timestampMillis: String(NOW - 1000) },
  appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED", packageName: PKG },
  deviceIntegrity: { deviceRecognitionVerdict: ["MEETS_DEVICE_INTEGRITY"] },
});

const calls: string[] = [];

type Google = { integrity?: object | number; purchase?: object | number };

function fakeGoogle({ integrity = goodIntegrity(), purchase = { purchaseState: 0 } }: Google = {}): typeof fetch {
  const reply = (v: object | number) =>
    typeof v === "number" ? new Response("{}", { status: v }) : Response.json(v);
  return (async (input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    calls.push(url);
    if (url.startsWith("https://oauth2.googleapis.com/token")) return Response.json({ access_token: "at" });
    assert.equal((init?.headers as Record<string, string>)?.authorization, "Bearer at", `no auth on ${url}`);
    if (url === `https://playintegrity.googleapis.com/v1/${PKG}:decodeIntegrityToken`)
      return reply(typeof integrity === "number" ? integrity : { tokenPayloadExternal: integrity });
    if (
      url ===
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PKG}/purchases/products/picklejot_pro/tokens/${encodeURIComponent(TOKEN)}`
    )
      return reply(purchase);
    throw new Error(`unexpected fetch ${url}`);
  }) as typeof fetch;
}

const call = (g: Google = {}, body: object = { purchaseToken: TOKEN, clientNonce: CLIENT_NONCE, integrityToken: IT }) =>
  handle(new Request("https://v/v1/verify", { method: "POST", body: JSON.stringify(body) }), env, fakeGoogle(g), NOW);

test("a real purchase on a recognised device gets a signed, nonce-bound grant", async () => {
  const res = await call();
  assert.equal(res.status, 200);
  const { payload, sig } = (await res.json()) as { payload: string; sig: string };

  const ok = verify(
    "sha256",
    Buffer.from(payload),
    createPublicKey(signing.publicKey.export({ type: "spki", format: "pem" })),
    Buffer.from(sig, "base64url"),
  );
  assert.ok(ok, "DER ECDSA signature must verify (this is the format Java expects)");
  assert.deepEqual(JSON.parse(Buffer.from(payload, "base64url").toString()), {
    status: "granted",
    integrity: "ok",
    purchaseToken: TOKEN,
    clientNonce: CLIENT_NONCE,
    productId: "picklejot_pro",
    packageName: PKG,
  });
});

test("a canceled purchase is reported as refunded, still signed", async () => {
  const res = await call({ purchase: { purchaseState: 1 } });
  const { payload } = (await res.json()) as { payload: string };
  assert.equal(JSON.parse(Buffer.from(payload, "base64url").toString()).status, "refunded");
});

const claimsOf = async (res: Response) =>
  JSON.parse(Buffer.from(((await res.json()) as { payload: string }).payload, "base64url").toString());

test("a Google-confirmed purchase is granted whatever the device integrity says (warning only)", async () => {
  const cases: Record<string, object> = {
    "nonce for a different purchase": { ...goodIntegrity(), requestDetails: { ...goodIntegrity().requestDetails, nonce: nonceFor("other", CLIENT_NONCE) } },
    "stale token": { ...goodIntegrity(), requestDetails: { ...goodIntegrity().requestDetails, timestampMillis: String(NOW - 3 * 60_000) } },
    "future-dated token": { ...goodIntegrity(), requestDetails: { ...goodIntegrity().requestDetails, timestampMillis: String(NOW + 60_000) } },
    "wrong package": { ...goodIntegrity(), requestDetails: { ...goodIntegrity().requestDetails, requestPackageName: "evil.app" } },
    "app not recognised": { ...goodIntegrity(), appIntegrity: { appRecognitionVerdict: "UNRECOGNIZED_VERSION", packageName: PKG } },
    "rooted/emulated device": { ...goodIntegrity(), deviceIntegrity: { deviceRecognitionVerdict: ["MEETS_BASIC_INTEGRITY"] } },
    "no device verdict": { ...goodIntegrity(), deviceIntegrity: {} },
    "app package mismatch": { ...goodIntegrity(), appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED", packageName: "evil.app" } },
    "missing timestamp": { ...goodIntegrity(), requestDetails: { ...goodIntegrity().requestDetails, timestampMillis: undefined } },
  };
  for (const [name, integrity] of Object.entries(cases)) {
    const res = await call({ integrity });
    assert.equal(res.status, 200, name);
    const c = await claimsOf(res);
    assert.equal(c.status, "granted", name);
    assert.equal(c.integrity, "failed", name);
  }
  const bad = await claimsOf(await call({ integrity: 400 })); // google says the token is malformed
  assert.deepEqual([bad.status, bad.integrity], ["granted", "failed"]);
});

test("integrity we could not check or were not sent still grants a confirmed purchase", async () => {
  for (const integrity of [500, 403, 401]) {
    const c = await claimsOf(await call({ integrity }));
    assert.deepEqual([c.status, c.integrity], ["granted", "unverifiable"], String(integrity));
  }
  const missing = { purchaseToken: TOKEN, clientNonce: CLIENT_NONCE };
  for (const body of [missing, { ...missing, integrityToken: null }, { ...missing, integrityToken: "" }]) {
    const c = await claimsOf(await call({}, body));
    assert.deepEqual([c.status, c.integrity], ["granted", "missing"], JSON.stringify(body));
  }
});

test("integrity never rescues a purchase Google does not confirm", async () => {
  for (const purchase of [400, 404, 410, { purchaseState: 2 }]) {
    assert.equal((await call({ purchase, integrity: goodIntegrity() })).status, 403, JSON.stringify(purchase));
  }
});

test("a token Google does not know, or a pending one, is rejected", async () => {
  for (const purchase of [400, 404, 410, { purchaseState: 2 }]) {
    assert.equal((await call({ purchase })).status, 403, JSON.stringify(purchase));
  }
});

test("our own upstream trouble is a 5xx, never a rejection of the buyer", async () => {
  for (const g of [{ purchase: 500 }, { purchase: 403 }, { purchase: 401 }]) {
    assert.equal((await call(g)).status, 502, JSON.stringify(g));
  }
});

test("malformed requests never reach Google", async () => {
  const never = (() => {
    throw new Error("must not call Google");
  }) as unknown as typeof fetch;
  const bad = [
    {},
    { purchaseToken: "", clientNonce: CLIENT_NONCE, integrityToken: IT },
    { purchaseToken: TOKEN, clientNonce: "short", integrityToken: IT },
    { purchaseToken: TOKEN, clientNonce: "has spaces not allowed!!", integrityToken: IT },
    { purchaseToken: "x".repeat(3000), clientNonce: CLIENT_NONCE, integrityToken: IT },
  ];
  for (const body of bad) {
    const res = await handle(new Request("https://v/v1/verify", { method: "POST", body: JSON.stringify(body) }), env, never, NOW);
    assert.equal(res.status, 400, JSON.stringify(body));
  }
  assert.equal((await handle(new Request("https://v/v1/verify", { method: "POST", body: "not json" }), env, never, NOW)).status, 400);
  assert.equal((await handle(new Request("https://v/v1/verify"), env, never, NOW)).status, 404);
  assert.equal((await handle(new Request("https://v/other", { method: "POST", body: "{}" }), env, never, NOW)).status, 404);
});

test("an unknown purchase never reaches the integrity quota, and junk integrity tokens never reach Google", async () => {
  calls.length = 0;
  await call({ purchase: 404 });
  assert.ok(!calls.some((u) => u.includes("decodeIntegrityToken")), "integrity decoded for an unknown purchase");

  calls.length = 0;
  const res = await call({}, { purchaseToken: TOKEN, clientNonce: CLIENT_NONCE, integrityToken: "not-a-jwe" });
  assert.equal(res.status, 200);
  assert.equal((await claimsOf(res)).integrity, "failed");
  assert.ok(!calls.some((u) => u.includes("decodeIntegrityToken")), "junk integrity token reached Google");
});

const limited = (success: boolean, keys: string[] = []): Env => ({
  ...env,
  RATE_LIMITER: { limit: async ({ key }) => (keys.push(key), { success }) },
});
const tokenLimited = (success: boolean, keys: string[] = []): Env => ({
  ...env,
  TOKEN_LIMITER: { limit: async ({ key }) => (keys.push(key), { success }) },
});
const post = (e: Env, f: typeof fetch, headers: Record<string, string> = {}) =>
  handle(
    new Request("https://v/v1/verify", {
      method: "POST",
      headers,
      body: JSON.stringify({ purchaseToken: TOKEN, clientNonce: CLIENT_NONCE, integrityToken: IT }),
    }),
    e, f, NOW,
  );

test("rate limiter: allowed requests take the normal flow, keyed by client IP", async () => {
  const keys: string[] = [];
  assert.equal((await post(limited(true, keys), fakeGoogle(), { "CF-Connecting-IP": "1.2.3.4" })).status, 200);
  assert.equal((await post(limited(true, keys), fakeGoogle())).status, 200);
  assert.deepEqual(keys, ["1.2.3.4", "unknown"]);
});

test("rate limiter: a denied request is a 429 with Retry-After and touches nothing upstream", async () => {
  calls.length = 0;
  const res = await post(limited(false), fakeGoogle());
  assert.equal(res.status, 429);
  assert.equal(res.headers.get("Retry-After"), "60");
  assert.deepEqual(await res.json(), { error: "rate_limited" });
  assert.deepEqual(calls, []);
  // 404s are answered before the limiter, so they never spend quota
  const keys: string[] = [];
  assert.equal((await handle(new Request("https://v/other", { method: "POST" }), limited(false, keys), fakeGoogle(), NOW)).status, 404);
  assert.deepEqual(keys, []);
});

test("rate limiter: no binding means no limiting", async () => {
  assert.equal(env.RATE_LIMITER, undefined);
  assert.equal((await post(env, fakeGoogle())).status, 200);
});

test("rate limiter: IPv6 keys on the /64, and a throwing binding fails open", async () => {
  assert.equal(ipKey("2001:DB8:0:1::5"), "2001:db8:0:1");
  assert.equal(ipKey("2001:db8:0:1:ffff:ffff:ffff:ffff"), ipKey("2001:db8::1:0:0:0:1"));
  assert.equal(ipKey("::1"), "0:0:0:0");
  assert.equal(ipKey("1.2.3.4"), "1.2.3.4");
  const boom = { ...env, RATE_LIMITER: { limit: async () => { throw new Error("down"); } } };
  assert.equal((await post(boom, fakeGoogle())).status, 200);
});

test("token limiter: keyed on the token hash, denied = 429 with nothing upstream, throwing = fail open", async () => {
  const keys: string[] = [];
  assert.equal((await post(tokenLimited(true, keys), fakeGoogle())).status, 200);
  assert.deepEqual(keys, [createHash("sha256").update(TOKEN).digest("base64url")]);
  calls.length = 0;
  assert.equal((await post(tokenLimited(false), fakeGoogle())).status, 429);
  assert.deepEqual(calls, []);
  const boom = { ...env, TOKEN_LIMITER: { limit: async () => { throw new Error("down"); } } };
  assert.equal((await post(boom, fakeGoogle())).status, 200);
});

test("body cap: oversized bodies get 413 (declared or streamed) before any parsing or Google call", async () => {
  calls.length = 0;
  const big = "x".repeat(50_000);
  const declared = new Request("https://v/v1/verify", { method: "POST", body: big });
  assert.equal((await handle(declared, env, fakeGoogle(), NOW)).status, 413);
  const streamed = new Request("https://v/v1/verify", {
    method: "POST",
    body: new ReadableStream({ start: (c) => (c.enqueue(new TextEncoder().encode(big)), c.close()) }),
    duplex: "half",
  } as RequestInit);
  assert.equal((await handle(streamed, env, fakeGoogle(), NOW)).status, 413);
  assert.deepEqual(calls, []);
  assert.equal((await post(env, fakeGoogle())).status, 200);
});
