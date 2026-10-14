# Picklejot Privacy Policy

**Last updated:** 2026-10-14

This policy explains how Picklejot, an Android app for logging your pickleball matches, handles information. It is written in plain English.

## At a glance

- **No account.** You never sign up or sign in.
- **Your match log is never sent to us.** Picklejot does not send your matches, scores, notes, photos, names or location to us or to any server we run. Our one small server only checks Pro purchases (section 6).
- **No ads, no analytics, no crash reporting, no tracking.** Picklejot contains no advertising or analytics software and no third-party software that collects your data for its own purposes.
- **Your data lives on your phone.** Your match log is stored on your device.
- **One network feature: the Pro purchase.** The only network activity is buying, restoring and verifying the optional one-time "Pro" purchase. Google handles the payment, and Picklejot never sees your card details. A small verification service we run checks the purchase with Google (section 6).
- **Android may back up your data to your Google Account.** This is Android's own feature, not ours. We cannot access those backups. Photos are not included in cloud backup.
- **Delete anytime.** Use "Erase all data" in Settings to delete your log from your device, or uninstall the app.
- **Nothing is sold or shared** by us.

## 1. Who we are

Picklejot is made by Matthew Gabriel D. Reyes, an individual developer based in Parañaque City, Philippines ("we", "us", "the developer").

Contact: emgireyes.dev@gmail.com

We do not receive or store your match data, and we hold no copy of it. That means we are not the "controller" or "processor" of your match data in the way a company that runs servers would be. The one exception is the limited purchase-check information in section 6. It sits on your device, under your control. We still publish this policy and give you the contact and rights information that privacy laws require, so you can see what happens.

## 2. What Picklejot stores on your device

Everything in this list is stored locally on your device, in the app's private storage, and nowhere else by us.

| What | Why it is stored |
| --- | --- |
| Match records: dates, scores, results, notes | This is the app's purpose: your match journal |
| Player and opponent names that you type in | So you can record who you played with and against |
| Match location, as free text that you type in | So you can remember where you played. It is not GPS. Picklejot does not read your device location |
| Photos that you add to a match | Attached to the match. Stored as compressed copies in the app's private storage, separate from your original photo |
| Your display name | Shown in the app and on share cards |
| Reminder settings | To schedule the optional streak reminder |
| Pro entitlement and Google Play purchase token | So the app knows Pro is unlocked, including offline. The token is a code issued by Google Play that proves a purchase. It is also sent to our verification service when you buy or restore Pro (section 6) |
| App preferences | For example, settings you choose in the app |

**Other people's names.** If you type the names of other players, you are recording information about them. Please keep that in mind if you share exports or share cards. Names in your own private log kept for personal purposes are usually outside the scope of privacy laws, but sharing them publicly may not be.

**Photos.** Picklejot saves a re-encoded, compressed copy of each photo. Re-encoding is designed not to carry over embedded metadata such as the GPS position stored in the original file, but your original photo in your gallery or camera roll is unaffected and may contain such metadata.

## 3. What we do not collect or do

- We do not collect your name, email address, phone number, address, or any account details.
- We do not collect your device location or GPS data.
- We do not collect advertising IDs, device IDs, or usage analytics.
- We do not read your contacts, calendar, messages, microphone recordings, or your wider photo library. The photo picker only gives the app the specific photos you select.
- We do not use cookies or tracking technologies.
- We do not show ads.
- We do not sell, rent, or share your personal data, and we do not use it for profiling or automated decisions.
- We do not use your data to train AI models.
- We do not knowingly receive any copy of your match log.

## 4. Where your data lives

Your match log never leaves your device. The only server of ours that Picklejot talks to is the purchase verification service in section 6, and it never receives your matches, photos or names. The share-card renderer inside the app can load only files bundled with the app. It blocks all other web requests.

## 5. Android backup and moving to a new phone

Android has built-in backup features that run outside Picklejot's control. Picklejot allows them, with these settings:

| Feature | What is included |
| --- | --- |
| Cloud backup to your Google Account (Android Auto Backup) | The app's database and settings files. That means your matches, scores, notes, player names, match locations, your display name, reminder settings and the cached purchase token. **Photos are excluded.** |
| Device-to-device transfer (when you set up a new phone from an old one) | Everything, **including photos** |

- Backups are stored in your Google Account storage and are governed by your Google settings and [Google's Privacy Policy](https://policies.google.com/privacy). We cannot see, access, restore or delete them.
- Backups may be paused if your Google storage is full, or if backup is switched off on your device. Do not rely on them as your only copy. See "Exports" below.
- Because photos are excluded from cloud backup, restoring from a Google backup brings back your match records but not your photos.
- If you want to stop cloud backup of Picklejot, you can switch off backup in your Android system settings, or delete the backup through your Google Account. Backups of already-deleted data may remain in Google's systems until you delete them there. "Erase all data" in Picklejot does not delete backups in your Google Account.

## 6. Google Play Billing and purchase verification

Picklejot offers an optional one-time "Pro" purchase through Google Play Billing. Pro is not a subscription.

- Google processes the payment and holds payment details. Picklejot never sees your card or payment details.
- To complete or restore a purchase, the app talks to Google Play services on your device.
- **Purchase verification.** Before unlocking Pro, the app sends a small verification request over HTTPS to a service we run on Cloudflare. The request contains your Google Play purchase token, a Play Integrity token (a Google-issued check that the app is the genuine Play version running on a real device) and a random one-time code. It contains none of your matches, photos, names or locations.
- The service passes the purchase token and Play Integrity token to Google to confirm the purchase is real, then replies with a signed yes or no. It does not store the tokens in a database. Cloudflare, which hosts the service, handles ordinary connection data such as IP addresses under Cloudflare's own privacy policy.
- If the service cannot be reached, Pro is not unlocked yet and the app tries again later. Anything already unlocked stays unlocked.
- Picklejot stores the resulting Pro status and the Google Play purchase token on your device (see section 2).
- Google shows developers some purchase details in the Google Play Console, such as order information, the product bought, and the country or region of the purchase. It may include other limited details for tax, refund and support purposes. We use these details only to run the app, handle refund and support requests, keep accounts and meet legal obligations. We do not link them to your match log.
- Google may separately collect diagnostic and usage information as part of Google Play and Android. It may give developers aggregated reports, such as crash and performance statistics from users who chose to share them with Google. That is Google's data collection under Google's policies. Picklejot's code does not do it.
- "Rate us" and "Share app" open Google Play or the Android share sheet. Any review you post is public, is handled by Google, and is governed by Google's terms and privacy policy.

Google's use of your information is governed by [Google's Privacy Policy](https://policies.google.com/privacy) and the Google Play Terms of Service.

## 7. Permissions

| Permission | When it is used | Can I say no? |
| --- | --- | --- |
| Camera | Only when you tap "Take photo" on a match. The app asks at that moment | Yes. Everything else works. You can add photos from your gallery instead |
| Notifications | Only if you turn on the streak reminder | Yes. Reminders stay off |

Gallery photos are added through the Android photo picker, which shares only the photos you choose and needs no storage permission. You can withdraw any permission at any time in your Android system settings.

## 8. Exports and share cards

- **Exports.** If you choose to export, Picklejot creates a JSON file containing your matches, opponents and scores. It does not include photos. The file is handed to the Android share sheet or file saver. Where it goes next (your files, email, cloud storage, another app) is your choice and subject to that service's own policy.
- **Imports.** You can import a Picklejot export file. Picklejot checks and size-limits imported files, and processes them on your device.
- **Share cards.** A share card is an image generated on your device when you ask for one. It is passed to the Android share sheet so you can send it to apps you choose, such as Instagram or a messaging app. On the free tier the card carries a Picklejot wordmark. We receive nothing.

Once you share something with another app or person, we cannot recall or control it. Check what a card or file contains, including other players' names, before you share.

## 9. How long we keep data, and deleting it

We keep nothing. On your device, your data stays until you remove it.

| Action | Result |
| --- | --- |
| Delete a match in the app | That match is removed from the app |
| Erase all data (Settings) | Permanently deletes your matches, photos and name from the device. This cannot be undone. Your Pro purchase is kept |
| Uninstall the app | Removes the app's local data from your device |
| Backups in your Google Account | Separate from the app. Delete them through your Google Account or Android backup settings |
| Exports and shared cards | Copies you made remain wherever you sent them until you delete them there |
| Purchase records | Held by Google (and, as transaction records, in the Play Console) under Google's retention rules and legal requirements |
| Purchase verification requests | Not stored by us in a database. Cloudflare and Google handle them under their own policies |

If a backup is restored after erasing or reinstalling, deleted data can come back from that backup. Delete the backup as well if you want it gone.

## 10. Security

Your data is kept in the app's private storage, which other apps cannot normally read on an unmodified, up-to-date Android device, and the only server we run never receives your match log.

The limits:

- The local database is **not encrypted at rest** by Picklejot. Anyone who can unlock your phone, or who can access it with root or forensic tools, or who gets hold of an unencrypted device backup, may be able to read it. Android may encrypt your device's storage as a whole, depending on your device and settings.
- Use a screen lock, keep your phone updated, and protect your Google Account.
- Exports and share cards are ordinary files once created. Protect them as you would any other file.

No system is perfectly secure.

## 11. Children

Picklejot is not directed to children under 13 and is not designed for or targeted at children (including under Google Play's Families program). We do not collect personal information from anyone, including children. If you are a parent or guardian and have concerns, contact us at emgireyes.dev@gmail.com. The practical step is to erase the app's data or uninstall the app on the child's device.

In some places the age at which a person can agree to online services is higher than 13 (for example, up to 16 in parts of the EEA). If you are under that age in your country, please use Picklejot only with a parent or guardian's involvement.

## 12. Your rights

Privacy laws give people rights over their personal data. Most of these rights are met by features you control directly. Where a right depends on data held by someone else, such as Google, we point you there.


| Right | How it works here |
| --- | --- |
| Access and portability | Your data is on your device. Use Export to get a copy of your matches in a JSON file |
| Correction | Edit any match, name or setting in the app |
| Deletion / erasure | Use Erase all data, delete individual matches, or uninstall |
| Object, restrict, withdraw consent | Turn off the camera or notification permission in system settings. Switch off reminders in the app |
| No sale or sharing | Nothing is sold or shared by us |
| No automated decisions | We do not make any |

If you contact emgireyes.dev@gmail.com with a privacy request, we will reply, tell you that we hold no copy of your match data, and explain how to use the controls above. We may need to confirm that you are the person asking, but we will not ask for more personal data than is needed. Purchase-related requests (refunds, receipts, payment details) are handled by Google Play, and we may need to coordinate with Google.

**Philippines (Data Privacy Act of 2012, RA 10173, and its Implementing Rules).** If you are in the Philippines, you have the rights of a data subject, including to be informed, to access, to object, to correct, to erasure or blocking, to data portability, to damages, and to complain. You may complain to the National Privacy Commission (privacy.gov.ph). The Act's rules apply to the processing of personal information. Because we do not process your match data on any server, this policy explains what is processed on your own device and the limited purchase information described in section 6.

**European Economic Area and United Kingdom (GDPR / UK GDPR).** If you are in the EEA or UK, you have the rights of access, rectification, erasure, restriction, portability and objection, and the right to complain to your local data protection authority (in the UK, the Information Commissioner's Office). Because we hold no personal data from the app, the lawful bases are not engaged for the match log in the usual way. The limited purchase information described in section 6 is handled on the basis of contract (to deliver your purchase), legal obligation (for example, tax and accounting records) and our legitimate interest in preventing fake purchases. If a legal basis for storing data on your device is needed, it is your own request to use the app (contract) and, for camera and notifications, your consent through the system permission prompt, which you can withdraw at any time.

**California (CCPA / CPRA).** We do not sell or share personal information (including for cross-context behavioral advertising), we do not collect sensitive personal information, and we do not use or disclose it for purposes that would trigger the right to limit. California residents have the rights to know, delete, correct, and not be discriminated against for using their rights. As above, we hold no match data to disclose or delete, and the controls are in the app. You may also use an authorized agent. We do not respond to or need a "Do Not Track" signal because we do not track.

**Other places.** If you live somewhere else with privacy rights, you can contact us and we will do what we reasonably can.

## 13. International considerations

We are based in Parañaque City, Philippines. Because your match data stays on your device, it is not transferred internationally by Picklejot. Purchase verification requests travel over Cloudflare's network, which may route them through other countries. Google Play, Google Play Billing, and Android backup are operated by Google, which may process information in countries other than yours under Google's terms and safeguards. If you share an export or card, the receiving service may be in another country.

## 14. Changes to this policy

If our practices change, we will update this policy. For example, if we add a feature that sends data off your device, we will update it before that feature is released. We will change the "Last updated" date, and for significant changes we will tell you in the app or on the Google Play listing. Please check this page from time to time.

## 15. Contact

Questions, requests or complaints: emgireyes.dev@gmail.com

Developer: Matthew Gabriel D. Reyes, Parañaque City, Philippines

## 16. Data safety summary

This table is a plain-language summary that matches what we declare in the Google Play Data safety form. The sections above are the full policy.

| Question | Answer |
| --- | --- |
| Does the developer collect any user data? | Only for purchase checks. Your match log never leaves your device. The purchase token and a Play Integrity token are sent to our verification service when you buy or restore Pro |
| Does the developer share any user data with third parties? | Only Google, to confirm a purchase |
| Is data processed only on the device? | Yes, for matches, scores, notes, names, location text, photos, and settings. Purchase checks go to our verification service and Google |
| Data encrypted in transit? | Yes. Purchase verification requests use HTTPS, and Google Play traffic uses Google's secure channels |
| Can you request data deletion? | Yes. Use Erase all data or uninstall. Backups in your Google Account are deleted through Google |
| Account required? | No |
| Ads? | No |
| Analytics or crash reporting? | None in the app |
| Financial info | Payments are processed by Google Play. Picklejot never receives card or payment details |
| Optional permissions | Camera (only for taking photos) and notifications (only for the streak reminder) |
| Directed at children? | No |
| Independent security review? | No |

This summary is accurate as of the date at the top of this policy.
