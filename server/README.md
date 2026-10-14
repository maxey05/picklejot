# Picklejot purchase verifier

One Cloudflare Worker, one endpoint: `POST /v1/verify`. The app will not grant Pro until this
returns a signed "granted" for the exact purchase and attempt (`data/billing/ServerPurchaseValidator.kt`).

## What it checks

1. **Play Integrity** — decodes the app's integrity token with Google and requires: our package, a nonce equal
   to `base64url(sha256(purchaseToken + "\n" + clientNonce))`, a token under 2 minutes old,
   `PLAY_RECOGNIZED` (genuine, Play-distributed APK) and `MEETS_DEVICE_INTEGRITY`.
2. **Purchase** — asks the Play Developer API whether `purchaseToken` is a real purchase of `picklejot_pro`.
3. **Answer** — returns `{payload, sig}`: the verdict plus the client's nonce, signed ECDSA P-256. The app checks
   the signature against the public key baked into the build, so a fake server or a replayed answer fails.

| HTTP | Meaning | App does |
|---|---|---|
| 200 | signed `granted` / `refunded` | grants / records the refund |
| 4xx | integrity failed, or Google does not know the token | grants nothing, changes nothing |
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
   Blank = the app grants no new Pro purchases (fails closed; existing Pro is untouched).

`npm test` runs the worker against mocked Google endpoints. No network, no credentials.

## Testing on a device

Integrity requires an install Google Play recognises. Sideloaded debug builds are rejected by design. Use the
**internal testing track** (or an app bundle installed from Play) with a licensed tester account.

## Known limits

- A rooted device can patch the APK to skip the check. This stops fake-purchase tools and forged Billing
  responses, not a repackaged app (which Play Integrity's `PLAY_RECOGNIZED` makes unable to *obtain* a grant).
- A purchase token that leaks can be verified by a second genuine device. Closing that needs accounts.
- Classic Play Integrity quota is 10,000 requests/day. The app verifies a token once, then trusts the cached
  entitlement, so this is spent per *purchase/restore*, not per launch.
- No rate limiting here; put Cloudflare rate-limit rules on `/v1/verify` if abused.
- Play Data Safety: the purchase token and integrity token are sent to this server. Declare it.
