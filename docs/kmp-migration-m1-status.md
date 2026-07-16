# KMP Migration M1 — Status & Follow-ups

Date: 2026-07-16 · Branch: `feature/kmp-migration-m1` (21 commits, unmerged) · Issue: [#13](https://github.com/appswithlove/updraft-sdk-android/issues/13)
Spec: [2026-07-16-kmp-migration-design.md](superpowers/specs/2026-07-16-kmp-migration-design.md) · Plan: [2026-07-16-kmp-migration-m1.md](superpowers/plans/2026-07-16-kmp-migration-m1.md)

## What was built

| Module | Coords | Content |
|---|---|---|
| `:updraft-core` | `com.appswithlove.updraft:updraft-core:2.0.0` | KMP (androidTarget in M1). Ktor client, coroutines/Flow, `Updraft.start(UpdraftSettings)`, `Updraft.events: SharedFlow<UpdraftEvent>`, interactors, expect/actual seams (KeyValueStore, AppInfo, shake, screenshot, url, foreground observer). Replaces Retrofit + RxJava2. |
| `:updraft-ui-compose` | `com.appswithlove.updraft:updraft-ui-compose:2.0.0` | Compose Multiplatform feedback UI: `FeedbackScreen` (annotate + form), `DrawingCanvas` (FreeDrawView port), dialogs, `UpdraftEventHost`. Strings en/de via compose-resources. |
| `:updraft-sdk` | `com.appswithlove.updraft:updraft-sdk:2.0.0` (coords unchanged) | Thin Android wrapper: 2 host activities + androidx.startup auto-wire. Entire legacy Views/Fragments/FreeDrawView/Retrofit stack deleted (~3500 lines). |

Breaking change: 2.0.0. `Settings` → `UpdraftSettings`, `Updraft.initialize(context, settings)` + `getInstance()?.start()` → `Updraft.start(settings)`. Migration table in README.

Full build + all unit tests green. Final adversarial whole-branch review: READY TO MERGE. Each task individually spec- and quality-reviewed with fix loops (ledger: `.superpowers/sdd/progress.md`, gitignored).

## Release checklist (merge ≠ release — these BLOCK a 2.0.0 release)

- [ ] **Fix `.github/workflows/publish.yml`**: it publishes only `:updraft-sdk`. The `updraft-sdk` POM references `updraft-core`/`updraft-ui-compose` via `api(project(...))` → publishing only one module ships a 2.0.0 whose dependencies don't exist on Maven Central; every consumer build breaks. Add publish tasks for all three modules.
- [ ] **Manual device verification** (needs real APP_KEY/SDK_KEY — currently empty strings in `app/App.kt`, real keys stripped from repo in 2024):
  1. Launch → feedback hint dialog on first start
  2. Shake → annotate screen; **draw near screen corners**; undo/redo; OK → form; select type; send → progress → closes
  3. Inspect uploaded image on staging dashboard — verify annotation positions align (coordinate mapping was the critical fix; stroke *width* scaling consciously skipped, see follow-ups)
  4. Rotate mid-form — screenshot must survive, no stacked activities
  5. Lower build number installed → update dialog; "Open" opens browser
  6. Toggle feedback-enabled on dashboard → disabled/how-to dialog on next foreground
  7. Shake twice in a row (second after closing feedback UI) — must work both times
- [ ] README `Release` section says "create a GitHub release" but workflow triggers on push to `production` — reconcile.

## Follow-ups (non-blocking, ordered roughly by impact)

1. **Stroke width not rescaled in `renderAnnotated`** — positions map correctly to bitmap space, but a 12px screen stroke stays 12px on the full-res bitmap → annotations look thinner in uploads. Fix: divide `strokeWidthPx` by the fit scale.
2. **Rotation loses form/drawing state** — `remember` not `rememberSaveable` in `FeedbackScreen`; typed text + drawn paths gone, in-flight upload cancelled silently. Screenshot itself survives (fixed).
3. **`UpdraftOverlayActivity.pendingEvent` static race** — two dialog events in quick succession can overwrite before first `onCreate` consumes; one dialog dropped. Proper fix: intent extras instead of static holder.
4. **Double feedback hint on fresh install** — sync `onForeground` hint + async state-transition hint can both fire in one cycle (legacy-inherited); dedupe via `feedbackHintShown` on the async path.
5. **`updateAlertShown` race** — two rapid foregrounds → concurrent update checks both pass the once-guard → double dialog. Window = network RTT.
6. **Late `Updraft.start()` drops events** — if called after first foreground (not in `Application.onCreate`), the auto-wire collector isn't subscribed yet (`replay=0`) and the hint is permanently lost. Document the `Application.onCreate` requirement prominently, or add replay.
7. **`decodePng` NPE on invalid bytes** — `BitmapFactory.decodeByteArray` returns null on garbage; `FeedbackScreen(screenshotPng, ...)` is public API. Add null handling.
8. **Spec's `FeedbackSent` event unimplemented** — consumers can't observe successful sends via `Updraft.events`.
9. **Empty-screenshot upload** — `screenshotPng == null` path uploads `ByteArray(0)` as image part; server acceptance untested.
10. **Unbounded 100ms poll** in `UpdraftAndroid.autoWire()` if `start()` never called. Cosmetic.
11. **Stale Retrofit keep rule** in `updraft-sdk/proguard-rules.pro` (inert, minify off).
12. **Compose UI tests missing** — spec called for Robolectric-based UI tests (form validation, drawing); only state/controller unit tests exist. An annotate→render UI test would have caught the coordinate-mapping bug.
13. **Feedback hint copy** — form labels localized (en/de) in final fix wave; verify German copy with a native reader before release.

## Deferred to M2 (per spec)

- iOS targets + `iosMain` actuals (motionEnded shake, UIWindow screenshot, itms-services install, NSUserDefaults)
- XCFramework export (SPM/CocoaPods), iosApp sample, deprecate [updraft-sdk-ios](https://github.com/appswithlove/updraft-sdk-ios)
- `app/` → `sample/` rename (CMP structure with androidApp + iosApp)

## Post-migration reminder (from spec)

- Revisit options for Views-based Android apps: the ~2 MB Compose cost — decide between a documented `updraft-core`-only recipe with minimal Views feedback screen, an `updraft-ui-views` artifact, or confirming no consumer needs it.

## Dev environment note

Git commits are SSH-signed via 1Password; the signing agent intermittently fails (`1Password: failed to fill whole buffer`). Unlock/approve 1Password and retry — no config change needed.
