# Google Play Data safety walkthrough for Picklejot

Prepared from the repo `maxey05/picklelog` (package `com.maxeydev.picklejot`, targetSdk 36) and the draft policy in `legal/privacy-policy.md`. This is a map from each Play Console question to an answer and the code behind it. It is not legal advice, and Google does not verify your answers: you are responsible for them.

Google's own definitions (Play Console Help, "Provide information for Google Play's Data safety section"):

- Collect means transmitting data from your app off the user's device.
- Share means transferring user data collected from your app to a third party.
- Data that is only processed on the device does not need to be disclosed.
- A transfer the user starts through a specific action they reasonably expect (the Android share sheet, a file save) is not "sharing".
- Payment details that Google Play's own billing system collects directly, which your app never accesses, do not need to be declared. Purchase history is a separate data type to think about.

## 1. Recommended answers at a glance

Updated 14 October 2026 against the working tree in `C:\emgiStuff\codingProjects\picklelog`, which is newer than the GitHub copy this map was first written from. The working tree adds a purchase verifier (`server/`, a Cloudflare Worker) and Play Integrity (`data/.../billing/ServerPurchaseValidator.kt`, `PlayIntegrityTokenProvider.kt`), and declares `INTERNET`. That changes the main answer from "No data collected" to "Yes, purchase data".

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Data types | Financial info > **Purchase history** (the Google Play purchase token). Also consider **Device or other IDs** for the Play Integrity token, see 2.2 |
| Collected or shared | **Collected, not shared** (the Cloudflare Worker and Google act as service providers on your behalf) |
| Processed ephemerally | **Yes**, only if you confirm that Workers Logs and Logpush are off in the Cloudflare dashboard, see 2.2 |
| Required or optional | **Optional** (only when a user buys or restores Pro) |
| Purposes | App functionality; Fraud prevention, security, and compliance |
| Is all of the user data collected by your app encrypted in transit? | **Yes**. `LicenseServerConfig.isUsable` requires an `https://` URL |
| Do you provide a way for users to request that their data is deleted? | **Yes**: Erase all data and uninstall for on-device data; email for anything else. The verifier keeps no copy, so there is nothing to delete on the server side |
| Independent security review badge | Do not claim |
| Families Policy commitment | Do not claim |
| Privacy policy URL | `https://maxey05.github.io/picklelog/privacy.html` (confirm after publishing, see `PUBLISHING.md`) |

Match data, photos, names, notes and location text are still never sent anywhere. The form must say so in the descriptions where it allows free text.

## 2. Section by section

### 2.1 Privacy policy (App content > Privacy policy)

- Answer: the hosted URL, which must be public, not a PDF, not geo-fenced, and must name the developer and a contact.
- Depends on legal text: developer name and contact come from the filled policy. Run the site build with `--strict` so a page with placeholders cannot be published by mistake.
- Evidence: policy sections 1 and 15. In the app, `ui/src/main/kotlin/com/maxeydev/picklejot/ui/settings/PrivacyPolicyScreen.kt` shows the policy offline.

### 2.2 Data collection and security: "Does your app collect or share any of the required user data types?"

Answer: **Yes**, for the purchase check only.

What leaves the device, with the code behind it:

| Item | Where it goes | Evidence |
| --- | --- | --- |
| Google Play purchase token | Your verifier on Cloudflare (`POST /v1/verify`), then to Google's Play Developer API | `ServerPurchaseValidator.attempt` builds `Request(purchaseToken, clientNonce, integrityToken)`; `server/src/index.ts` |
| Play Integrity token | Same request. The verifier decodes it with Google and requires `PLAY_RECOGNIZED` and `MEETS_DEVICE_INTEGRITY` | `PlayIntegrityTokenProvider`, `server/README.md` |
| A random one-time nonce | Same request | `ServerPurchaseValidator.attempt` |
| IP address and connection metadata | Seen by Cloudflare as the host, and by Google when Google Play services runs billing | Normal network behaviour |
| Matches, photos, names, notes, location text | **Nowhere** | No other HTTP code exists. `CardWebView` and `LockedDownWebViewClient` still block every non-asset request |

Judgement calls to settle before you submit:

1. **Is the Play Integrity token a "Device or other ID"?** Google's examples do not list Play Integrity. The token carries a verdict about the device and app, not an identifier you can track across apps. The cautious choice is to declare Device or other IDs as collected. Declaring more than you collect is safer than declaring less, and costs little here.
2. **Ephemeral processing.** `server/src/index.ts` has no database, KV, D1 or log statements, and `wrangler.toml` enables no observability. Cloudflare can still keep request logs if you turn Workers Logs or Logpush on. Check both in the Cloudflare dashboard before ticking "processed ephemerally", and tell me if you turn either on, because the policy says the service "does not store the tokens in a database".
3. **Purchase history.** The token is also kept on the device (`user-profile.json`) and included in Android backups, as the policy says.
4. **Android Auto Backup.** `allowBackup="true"` with rules in `app/src/main/res/xml/data_extraction_rules.xml` (cloud backup excludes `photos/`; device transfer includes everything). Google's Data safety help does not address Auto Backup, so re-check the live Console wording. The policy discloses it in section 5.
5. **Google Play and Android diagnostics.** Crash and performance data that Google collects through Android vitals for users who opted in is Google's collection, not a library in the app. The policy says so in section 6.

Other claims that still hold:

| Claim | Evidence |
| --- | --- |
| No analytics, crash reporting, ads or other data-collecting SDKs | `gradle/libs.versions.toml` lists AndroidX, Compose, Room, DataStore, WorkManager, Coil, kotlinx libraries, `billing-ktx` and the Play Integrity library. No Firebase, Crashlytics, Sentry, ad or attribution libraries |
| Match location is typed text, not device location | No `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` in any manifest |
| Photos come from the system picker and the camera | `ui/.../match/edit/PhotoPickerSection.kt` uses `PickVisualMedia` and `TakePicture`. No `READ_MEDIA_*` permission |
| Exports and share cards leave only through the user's own action | `ExportShareLauncher.kt`, `ShareIntentLauncher.kt` use `Intent.ACTION_SEND`. Google does not treat user-started transfers as developer sharing |
| Reminders are local | WorkManager and local notifications. No push service |

### 2.3 "Is all of the user data collected by your app encrypted in transit?"

Yes. `LicenseServerConfig.isUsable` is false unless the URL starts with `https://`, and the Worker only answers over HTTPS on `workers.dev`.

### 2.4 "Do you provide a way for users to request that their data is deleted?"

Yes. The facts:

| Mechanism | Evidence |
| --- | --- |
| Erase all data (on-device data) | `data/src/main/kotlin/com/maxeydev/picklejot/data/erase/RoomLocalDataEraser.kt`; string `settings_erase_dialog_body` |
| Delete a single match | `delete_match_body` in `strings.xml` |
| Uninstall | Android removes app-private storage |
| Cloud backup | Separate. User deletes it in their Google Account (policy sections 5 and 9) |
| Verifier | Keeps no database of tokens. Purchase records stay with Google |

### 2.5 Data types, purposes, optional or required

See the table in section 1.

### 2.6 Security practices badges

- Encrypted in transit: yes, for the verification request.
- Deletion request: yes, see 2.4.
- Independent security review: **no**. Do not tick it.
- The local database is not encrypted at rest. The policy says so in section 10. Do not describe the app as "secure" or a "vault" in the listing.

## 3. Other App content declarations that must agree

| Declaration | Answer | Evidence or note |
| --- | --- | --- |
| Ads | No ads | No ad SDK. In-app text: `onboarding_privacy_body`. |
| Advertising ID | No | Check the merged manifest has no `com.google.android.gms.permission.AD_ID` (command in section 6). If a library adds it, either remove it with `tools:node="remove"` or answer the Advertising ID question honestly. |
| App access | All functionality available without special access | No account or login. |
| Target audience and content | Not directed to children. Choose 13 and over or 18 and over, not Families | Policy section 11. The content rating questionnaire must be consistent. |
| Financial features | None | An in-app one-time purchase is not a financial feature. |
| Sensitive permissions | None to declare | `CAMERA` and `POST_NOTIFICATIONS` are not restricted. No SMS, call log, location, `MANAGE_EXTERNAL_STORAGE` or `READ_MEDIA_*`. |
| In-app purchases | Yes, one-time Pro | Price is shown by Google Play. The free-tier match limit is 50 in code (`domain/.../entitlement/FreeTier.kt`) but older notes said 100. The listing, the screenshots and `strings.xml` must state the number the code enforces. |
| Government, health, news | No | |

## 4. If Google pushes back on backup

Auto Backup is the answer most worth re-checking. The backup is created by Android and stored in the user's Google Account, and you cannot read it. If the Console or a reviewer says it must be declared, declare the types stored in the backed-up database (App activity, Personal info such as names; photos are excluded from cloud backup) as collected, not shared, optional, for app functionality, encrypted in transit. Read the live Console help or ask Play support first, because this would make the app look more data-hungry than it is. Changing the form also means updating policy sections 3 and 16 so they match.

## 5. Things that change these answers

Re-do the form, the policy, the in-app text and the Terms before releasing any of these:

| Change | Why | Where to look |
| --- | --- | --- |
| Any analytics, crash reporting or ad SDK | Collects data off device | `gradle/libs.versions.toml`, every `build.gradle.kts` |
| Any new network call besides the purchase verifier | Policy sections 4 and 6 describe exactly one | `app/src/main/AndroidManifest.xml` (declares `INTERNET`), `data/.../billing/*` |
| Billing changes (subscriptions, donations or tips, anything new the verifier receives or stores, turning on Cloudflare logging) | More purchase data would reach you. The "Support the creator" row (`settings_support`, `SupportScreen`) must not ship unless it uses Play Billing | `data/.../billing/*`, `ui/.../settings/SupportScreen.kt` |
| Backup rules change (for example including `photos/`, excluding `user-profile.json`, or `allowBackup="false"`) | Policy section 5 and the in-app `privacy_backup_body` state exactly which files go to the cloud | `data_extraction_rules.xml`, `full_backup_content.xml` |
| WebView loading remote content, or relaxing `LockedDownWebViewClient` | The card renderer is only safe because it is locked to bundled assets | `ui/.../share/CardWebView.kt`, `LockedDownWebViewClient.kt` |
| Real device location (GPS) | Location is currently free text | New permissions in the manifest |
| Home-screen widget (Glance) | Local only is fine, but the policy should mention that match data can show on the home screen | none yet |
| Accounts, cloud sync, sharing links, feedback forms collecting email | Data reaches you | n/a |
| Photo metadata handling | Policy says re-encoding is designed not to carry EXIF/GPS. Only orientation is read in `data/.../photo/ImageCompressor.kt`. Test with a GPS-tagged photo before keeping the sentence | `ImageCompressor.kt` |
| Targeting children or Families program | Different rules apply | Play Console target audience |

## 6. Checks to run before submitting (PowerShell, from the repo root)

Dependency scan for collecting libraries:

```powershell
.\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath | Select-String -Pattern 'firebase|crashlytics|sentry|analytics|admob|ads|appsflyer|adjust|mixpanel|segment|amplitude'
```

An empty result is what you want.

Merged manifest permissions (build a release or bundle first):

```powershell
.\gradlew.bat :app:processReleaseMainManifest
Get-ChildItem app\build\intermediates -Recurse -Filter AndroidManifest.xml | Where-Object { $_.FullName -match 'merged_manifests' -and $_.FullName -match 'release' } | ForEach-Object { Select-String -Path $_.FullName -Pattern 'uses-permission' }
```

Expected: `CAMERA`, `POST_NOTIFICATIONS`, `INTERNET` (needed for the verifier), and Billing's `com.android.vending.BILLING`, plus any dynamic receiver permission that AndroidX adds. If `AD_ID` or anything else unexpected appears, remove it with `tools:node="remove"` or answer the Advertising ID question honestly.

Not verified in this review: transitive dependencies, the Cloudflare dashboard logging settings, and the exact wording of the live Play Console form, which Google changes from time to time. The merged release manifest was read from `app/build/intermediates`: it contains `INTERNET`.

## 7. Policy and form consistency

| Policy section (`legal/privacy-policy.md`) | Must match |
| --- | --- |
| 3 What we do not collect or do | Section 1 table: match data never collected |
| 4 Where your data lives | Only the verifier is contacted; WebView lockdown |
| 5 Android backup | `data_extraction_rules.xml`, `full_backup_content.xml`, in-app `privacy_backup_body` |
| 6 Google Play Billing and purchase verification | `ServerPurchaseValidator`, `server/`, local purchase token |
| 7 Permissions | Manifest permissions and the Play permissions declarations |
| 16 Data safety summary | Every row of this document |
