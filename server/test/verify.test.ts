import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash, createPublicKey, generateKeyPairSync, verify } from "node:crypto";
import { handle, type Env } from "../src/index.ts";

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

test("integrity verdicts that must be rejected", async () => {
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
    assert.equal((await call({ integrity })).status, 403, name);
  }
  assert.equal((await call({ integrity: 400 })).status, 403, "google says the token is malformed");
});

test("a token Google does not know, or a pending one, is rejected", async () => {
  for (const purchase of [400, 404, 410, { purchaseState: 2 }]) {
    assert.equal((await call({ purchase })).status, 403, JSON.stringify(purchase));
  }
});

test("our own upstream trouble is a 5xx, never a rejection of the buyer", async () => {
  for (const g of [{ purchase: 500 }, { purchase: 403 }, { purchase: 401 }, { integrity: 500 }, { integrity: 403 }]) {
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
    { purchaseToken: TOKEN, clientNonce: CLIENT_NONCE },
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
  assert.equal(res.status, 400);
  assert.equal(calls.length, 0);
});
