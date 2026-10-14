# Legal pages source

`legal/privacy-policy.md` and `legal/terms-of-service.md` are the single source for three things:

- the website in `docs/` (served by GitHub Pages from `main:/docs`)
- the in-app Privacy policy and Terms of service screens (the `:ui` module bundles these two files as assets, see `ui/build.gradle.kts`)
- the Play Console privacy policy URL

Only the two policy files belong in `legal/`. Everything in that folder ships inside the APK, so never put developer notes there.

## Rebuild the website after editing the markdown

```powershell
node site-src\build.mjs --privacy site-src\legal\privacy-policy.md --terms site-src\legal\terms-of-service.md --out docs --strict
```

Update the "Last updated" line in both files whenever the text changes, rebuild, then commit `site-src/` and `docs/` together so the app and the site stay in step.

## Preview locally

```powershell
cd docs
python -m http.server 8080
```

Then open http://localhost:8080/.

## Pages URLs

- https://maxey05.github.io/picklelog/privacy.html
- https://maxey05.github.io/picklelog/terms.html

The same URLs are set in `LegalLinks.kt`. GitHub Pages must be enabled (Settings, Pages, Deploy from a branch, `main`, `/docs`) for them to resolve.
