# Picklejot purchase verifier

One Cloudflare Worker, one endpoint: `POST /v1/verify`. The app will not grant Pro until this
returns a signed "granted" for the exact purchase and attempt (`data/billing/ServerPurchaseValidator.kt`).

## What it checks

1. **Purchase** — asks the Play Developer API whether `purchaseToken` is a real purchase of `picklejot_pro`.
   This is the only thing that decides a grant: purchased = `granted`, canceled/refunded = `refunded`,
   pending/unknown = rejected.
2. **Play Integrity (warning only)** — if the app sent an integrity token, decodes it with Google and compares: our
   package, nonce equal to `base64url(sha256(purchaseToken + "
" + clientNonce))`, token under 2 minutes old,
   `PLAY_RECOGNIZED` and `MEETS_DEVICE_INTEGRITY`. The result is reported as `integrity` in the signed payload and
   logged, and **never blocks a grant** (custom ROMs, uncertified devices and old Play Services still get Pro).
   Values: `ok`, `failed` (bad token or weak verdict), `missing` (no token sent), `unverifiable` (Google could not be asked).
3. **Answer** — returns `{payload, sig}`: the verdict (`status`, `integrity`, ...) plus the client's nonce, signed ECDSA P-256. The app checks
   the signature against the public key baked into the build, so a fake server or a replayed answer fails.

| HTTP | Meaning | App does |
|---|---|---|
| 200 | signed `granted` / `refunded` | grants / records the refund |
| 429 | rate limited (`RATE_LIMITER`, 10 requests / 60 s per IP, `Retry-After: 60`) | **could not check**: changes nothing, retries later. Never a rejection |
| 413 | body over 40 KB (the app never sends that) | grants nothing, changes nothing |
| other 4xx | Google does not confirm the purchase (unknown, pending), or malformed request | grants nothing, changes nothing |
| 5xx | our trouble or Google's | changes nothing, retried on the next launch (R58) |

## One-time setup (you do these; they need your accounts)

1. **Play Console → Setup → App integrity**: link a Google Cloud project, turn on **Play Integrity API**.
   In that project also enable **Google Play Android Developer API**.
2. **Service account**: create one in the same project, download its JSON key. In Play Console →
   Users and permissions, invite its email with **View financial data** + **Manage orders and subscriptions**
   for this app. (Grants can take a few hours to apply.)
3. **Signing key**: `npm run gen-key`. Keep the private half secret; the public half goes in the app.
4. **Deploy**:
   ```
   npm install
   npx wrangler secret put GOOGLE_CLIENT_EMAIL     # client_email from the JSON key
   npx wrangler secret put GOOGLE_PRIVATE_KEY      # private_key from the JSON key (PEM, \n escapes are fine)
   npx wrangler secret put SIGNING_PRIVATE_KEY     # from step 3
   npx wrangler deploy
   ```
5. **App**: in `gradle.properties` (or `~/.gradle/gradle.properties`) set
   `licenseServerUrl=https://<your-worker>.workers.dev` and `licenseServerPublicKey=<public key from step 3>`.
   Release builds (and the release-derived benchmark build) refuse blank or malformed values: `verifyReleaseLicenseServer`
   requires a lowercase `https://` origin (no path) and the standard-base64 X.509 P-256 public key, and runs on every build.
   `-PallowUnverifiedRelease=true` downgrades the failure to a warning, for CI and builds that are never published. Such a
   build cannot grant Pro: the app treats an unusable config as unreachable, and `PlayEntitlementRepository` still
   acknowledges the purchase, so a buyer would be charged with no Pro and no automatic refund. Existing Pro is untouched.

`npm test` runs the worker against mocked Google endpoints. No network, no credentials.

## Testing on a device

A purchase needs a licensed tester account. Sideloaded debug builds still get Pro but report `integrity: failed`;
use the **internal testing track** to see `ok`.

## Known limits

- Integrity is advisory, so a repackaged app on a rooted device can still obtain a grant for a purchase Google
  confirms. Fake-purchase tools and forged Billing responses still fail: Google must confirm the token.
- A purchase token that leaks can be verified by a second genuine device. Closing that needs accounts.
- Classic Play Integrity quota is 10,000 requests/day. The app verifies a token once, then trusts the cached
  entitlement, so this is spent per *purchase/restore*, not per launch.
- Rate limiting is the Workers Rate Limiting binding `RATE_LIMITER` (`[[ratelimits]]` in `wrangler.toml`, 10 requests / 60 s
  per `CF-Connecting-IP`), checked before any body parsing or Google call. The binding is optional in code, so
  tests and `wrangler dev` without it are unlimited. Counters are per Cloudflare location, so it is approximate.
  IPv6 is keyed on the /64. A second binding, `TOKEN_LIMITER` (5 / 60 s per SHA-256 of the purchase token), runs after body
  validation and before any Google call. Both fail open if the binding errors. Bodies over 40 KB get 413 before parsing.
- Play Data Safety: the purchase token and integrity token are sent to this server. Declare it.
