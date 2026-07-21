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

- [x] **Fix `.github/workflows/publish.yml`** — DONE in M2: now publishes all three modules from a macOS runner. (Was: published only `:updraft-sdk`, whose POM references the other two via `api(project(...))`, so a one-module publish would ship a broken 2.0.0.)
- [ ] **Manual device verification** (needs real APP_KEY/SDK_KEY — currently empty strings in `app/App.kt`, real keys stripped from repo in 2024):
  1. Launch → feedback hint dialog on first start
  2. Shake → annotate screen; **draw near screen corners**; undo/redo; OK → form; select type; send → progress → closes
  3. Inspect uploaded image on staging dashboard — verify annotation positions align (coordinate mapping was the critical fix; stroke *width* scaling consciously skipped, see follow-ups)
  4. Rotate mid-form — screenshot must survive, no stacked activities
  5. Lower build number installed → update dialog; "Open" opens browser
  6. Toggle feedback-enabled on dashboard → disabled/how-to dialog on next foreground
  7. Shake twice in a row (second after closing feedback UI) — must work both times
- [x] README `Release` section reconciled with the `production`-branch push trigger — DONE in M2.

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

## M2 status (2026-07-21)

Branch: `feature/kmp-migration-m2` (unmerged).

### What M2 delivered

- **iOS targets + actuals** on `:updraft-core` (`iosArm64`, `iosSimulatorArm64`, `iosX64`): `IosShakeDetector` (`CMMotionManager` accelerometer, no permission entry needed), `IosScreenshotGrabber` (key-window render to PNG), `IosForegroundObserver` (`UIApplicationDidBecomeActive`/`DidEnterBackground`), `KeyValueStore`/`AppInfo` actuals.
- **`:updraft-ui-compose` iOS target**: same Compose feedback UI (`FeedbackScreen`, dialogs) compiled for iOS via Compose Multiplatform; `com.appswithlove.updraft.ui.ios.UpdraftIos.autoWire()` auto-presents dialogs/feedback screen on the topmost view controller, and `UpdraftFeedbackViewController(screenshotPng, onClose)` is exposed for manual hosting.
- **Sample**: `app/` renamed to `sample/` — `sample/composeApp` (shared Kotlin, `MainViewController()` + `startUpdraft()`) and `sample/iosApp` (xcodegen `project.yml`, `iOSApp.swift` calling `MainViewControllerKt.startUpdraft()`, iOS 14.0 deployment target).
- **XCFramework**: `:updraft-core:assembleUpdraftCoreXCFramework` builds `UpdraftCore.xcframework` (logic-only; does not include `updraft-ui-compose`).
- **CI**: `.github/workflows/build.yml` adds an `ios` job (macos-15) running both modules' `iosSimulatorArm64Test`, the XCFramework assemble task, and an `xcodebuild` of `sample/iosApp`. Triggers on `main` and `feature/**` branches. **Verified green on CI** (both android + ios jobs) on branch HEAD.
- README: real iOS setup replacing the "M2, not yet available" placeholder — Installation, iOS quickstart, and extended Swift integration sections; `updraft-sdk-ios` superseded note added.

### Verification findings (fixed on-branch)

- **Runner bumped macos-14 → macos-15**: the xcodegen-produced `project.pbxproj` uses `objectVersion 77` (Xcode 16 format); macos-14 ships Xcode 15 and fails with `Unable to read project 'iosApp.xcodeproj'` (exit 74). Fixed in both `build.yml` and `publish.yml`.
- **`CADisableMinimumFrameDurationOnPhone` added to `sample/iosApp/iosApp/Info.plist`**: without it, Compose Multiplatform's `PlistSanityCheck` throws `IllegalStateException` and the sample **crashes on launch**. Any CMP-hosting iOS app (including consumer apps) needs this key — documented as a requirement.
- **End-to-end confirmed**: sample launched on an iPhone 16 simulator; `startUpdraft()` → iOS foreground observer → controller event → `UpdraftIos.autoWire()` → `ComposeUIViewController` renders the CMP feedback hint dialog ("Feedback geben"). Full shared chain works on iOS.

### Remains release-time (blocks a 2.0.0 release, not this branch)

- [ ] Run the publish workflow once (`.github/workflows/publish.yml`, all three modules, macos-15, triggered by push to `production`) and verify all three land on Maven Central.
- [ ] Manual device verification on **both** platforms with real `APP_KEY`/`SDK_KEY` (still empty strings in the sample) — Android checklist above, plus iOS: launch → hint dialog; shake → annotate → form → send; rotate mid-form; update dialog against staging; feedback-enabled toggle.
- [ ] Archive [`updraft-sdk-ios`](https://github.com/appswithlove/updraft-sdk-ios) and add its final README deprecation banner.

### M2-specific follow-ups (non-blocking, ordered roughly by impact)

1. **iOS shake has no auto-pause on background** — `Updraft.start`'s `createAppForegroundObserver(... onBackground = { })` is a no-op; unlike a real pause, `IosShakeDetector` keeps listening to the accelerometer while backgrounded (Android's equivalent stops the sensor). Low impact (CoreMotion is cheap while backgrounded, and `presentViewController` would just no-op with no key window), but inconsistent with Android behavior.
2. **`UpdraftIos.presentDialog`/`presentFeedback` can silently drop events** — `topmostViewController() ?: return` and the `presentViewController` call have no retry/queueing; an event that arrives while `rootViewController` is nil (e.g. very early launch) or while another `presentViewController` transition is still in flight is dropped rather than deferred.
3. **`keyWindow()` helper duplicated** — near-identical `UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }` exists in both `updraft-core`'s `IosScreenshotGrabber` (`Platform.ios.kt`) and `updraft-ui-compose`'s `UpdraftIos.kt`. No shared internal utility between the two iOS source sets yet.
4. **Sample `UpdraftSettings` construction duplicated** — Android (`sample/composeApp` androidMain/App) and iOS (`MainViewController.kt#startUpdraft`) each hardcode their own `UpdraftSettings(...)` call; no shared `commonMain` config object.
5. **CI runner memory tuning** — `ios` job on `macos-15` (7 GB total) overrides Gradle/Kotlin daemon heap to `-Xmx3g` each (`GRADLE_OPTS` in `build.yml`), vs. defaults elsewhere; worth revisiting if iOS builds get memory-constrained as the project grows.
6. **`sample/iosApp/iosApp/Info.plist` has a stale `UIRequiredDeviceCapabilities: [armv7]`** — meaningless for an arm64-only/simulator build target (armv7 was 32-bit); harmless but should be dropped in a cleanup pass.

## Deferred to M3 (per spec, unchanged from M1)

- Fresh-repo/rename decision, SPM packaging polish, further platform targets.
