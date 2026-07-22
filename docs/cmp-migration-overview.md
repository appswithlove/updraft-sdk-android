# Updraft SDK 2.0 — Migration Overview

One-page map of what changed between the legacy Android SDK (1.x) and the
Kotlin/Compose Multiplatform SDK (2.0). Companion to
[`kmp-migration-m1-status.md`](kmp-migration-m1-status.md) (detailed status &
open items).

## Big picture

```
BEFORE (1.x)                              AFTER (2.0)
─────────────────────────────             ─────────────────────────────────────────
updraft-sdk-android  (Android)            updraft-sdk  (this repo, rebranded)
updraft-sdk-ios      (Swift)                ├── updraft-core        KMP  (android + ios)
                                            ├── updraft-ui-compose  CMP  (android + ios)
2 codebases, 2 feature sets                 └── updraft-sdk         Android wrapper
                                          1 codebase, identical features both platforms
                                          + UpdraftCore.xcframework for pure-Swift apps
```

- ~3500 lines of legacy Views/Fragments/Retrofit code deleted (commit `dd72439`).
- `updraft-sdk-ios` superseded; archived at release.
- Same Maven coordinates (`com.appswithlove.updraft:updraft-sdk:2.0.0`), plus two
  new artifacts with iOS klibs (`iosArm64`, `iosSimulatorArm64`, `iosX64`).

## Library replacements

| Concern | Before (1.x, Android) | After (2.0, common code) |
|---|---|---|
| HTTP | Retrofit 2 + OkHttp + logging-interceptor | **Ktor client 3.3** (OkHttp engine on Android, Darwin on iOS) |
| Async | RxJava 2 (+ rxjava2 adapter) | **kotlinx.coroutines 1.10** — `Flow` for upload progress, `SharedFlow` for events |
| JSON | kotlinx.serialization via Retrofit converter | **kotlinx.serialization** via Ktor content-negotiation |
| UI framework | XML layouts, Fragments, AppCompat, Material Components | **Compose Multiplatform** (material3) — one UI for Android + iOS |
| Drawing/annotation | FreeDrawView port (`com.rm.freedrawview`) + ink lib | **Custom `DrawingCanvas` + `DrawingController`** (~100 lines: path list, undo/redo stacks, Compose `Canvas`) |
| Lifecycle | lifecycle-extensions, ViewModel | plain state holders (`FeedbackScreenState`), no ViewModel dependency |
| Persistence | SharedPreferences | `KeyValueStore` expect/actual → SharedPreferences (Android) / NSUserDefaults (iOS) |
| Auto-init | androidx.startup | androidx.startup kept (Android); `UpdraftIos.autoWire()` (iOS) |
| Strings | Android resources + Loco | compose-resources, en/de, shared across platforms |

## Architecture: shared core + thin platform seams

Everything lives in `commonMain`; platforms only implement small `expect/actual` seams:

| Seam (`expect`) | Android `actual` | iOS `actual` |
|---|---|---|
| `createShakeDetector` | `SensorManager` accelerometer (g-force math shared) | `UIWindow.motionEnded` runtime hook (simulator + device) + `CMMotionManager` + screenshot notification (legacy iOS trigger) |
| `createScreenshotGrabber` | root view → `Canvas`/`Bitmap` → PNG | key `UIWindow` → `UIGraphicsImageRenderer` → `UIImagePNGRepresentation` |
| `createAppForegroundObserver` | `ProcessLifecycleOwner`-style callbacks | `UIApplicationDidBecomeActive` / `DidEnterBackground` notifications |
| `openUrl` | `Intent.ACTION_VIEW` | `UIApplication.openURL` |
| `createKeyValueStore` | SharedPreferences | NSUserDefaults |
| `currentAppInfo` | PackageManager + `ANDROID_ID` | NSBundle (`CFBundleVersion`) + `identifierForVendor` |
| `currentNavigationStack` | activity stack (`ActivityLifecycleCallbacks`) | view-controller chain walk (nav/tab aware) |
| `decodePng` (ui-compose) | `BitmapFactory` | Skia image codec |

Event flow (identical on both platforms):

```
shake / screenshot / button
        │
        ▼
Updraft (commonMain) ── captures screenshot + navigation stack
        │
        ▼  UpdraftEvent via SharedFlow
        │
   ┌────┴─────────────────────────────┐
   │ Android: androidx.startup        │  iOS: UpdraftIos.autoWire()
   │ auto-wire → UpdraftFeedback/     │  → presents ComposeUIViewController
   │ OverlayActivity (Compose host)   │  on topmost view controller
   └────┬─────────────────────────────┘
        ▼
FeedbackScreen (commonMain CMP) → annotate → form → send
        │
        ▼
Ktor multipart POST feedback-mobile/ (progress via onUpload → callbackFlow<Double>)
```

## Compose feedback UI — how the design was done

- Legacy XML layouts, colors, styles and PNG assets were **recovered from git
  history** (`git show dd72439^:updraft-sdk/src/main/res/...`) and rebuilt
  pixel-faithfully in Compose: charcoal `#38383C` chrome, Updraft logo header,
  "Feedback geben" title, close X, 4 color swatches (black/white/`#F6B33E`
  yellow/`#EB6764` red) with blue `#4A90E2` selection halo, dashed-circle
  undo button, yellow-italic "Draw something here…" overlay, white box form
  fields, yellow action buttons.
- Assets (`updraft_logo_white.png`, exit icons) extracted from history into
  compose-resources; dashed circle drawn with `PathEffect.dashPathEffect`.
- Screen is inset via `WindowInsets.safeDrawing`; dark background fills behind
  the system bars.
- The legacy "eraser" turned out to be undo-last-stroke — replicated as such.
- Annotation strokes are recorded in canvas coordinates and re-projected onto
  the full-resolution screenshot bitmap at send time (`renderAnnotated`), so
  drawings align on the dashboard regardless of screen density.

## Screenshot → upload pipeline

1. Trigger fires → platform `ScreenshotGrabber` captures the **host app** as PNG bytes (before any Updraft UI exists).
2. `FeedbackScreen` shows the PNG with a `DrawingCanvas` overlay; paths + color + 4dp stroke tracked by `DrawingController` (undo/redo).
3. On send: paths rendered onto the bitmap → PNG re-encoded → Ktor `MultiPartFormDataContent` with `image`, `app_key`, `sdk_key`, `tag`, `description`, `email`, `build_version`, `system_version`, `device_name`, `device_uudid` (sic — server field), `navigation_stack`.
4. Upload progress: Ktor `onUpload` → `callbackFlow<Double>` → progress bar; HTTP errors fail the flow (`expectSuccess = true`).

## Public API (2.0)

```kotlin
Updraft.start(UpdraftSettings(appKey, sdkKey,
    /* baseUrl, logLevel, showFeedbackAlert, feedbackEnabled, storeRelease, sendNavigationStack */))
Updraft.events: SharedFlow<UpdraftEvent>      // ShowFeedbackHint, UpdateAvailable, …
Updraft.checkForUpdate(); Updraft.showFeedback()
Updraft.navigationStackProvider = { listOf("Home", "Detail") }  // plug in any nav library
```

Breaking changes vs 1.x are listed in the README migration table
(`Settings` → `UpdraftSettings`, `initialize()+getInstance()` → `start()`).

## Found & fixed during device verification (2026-07-22)

1. `:updraft-sdk` missing Compose compiler plugin → `NoSuchMethodError` crash on any dialog.
2. Sample lacked the `:updraft-sdk` dependency → events silently dropped on Android.
3. `BASE_URL_STAGING` (u2.mqd.me) dead → removed; sample & default now production.
4. `device_uuid` vs server's `device_uudid` (legacy typo, load-bearing) → UUID now arrives.
5. Navigation stack: never existed on Android → implemented + pluggable provider + opt-out.
6. iOS shake: responder-chain event, not accelerometer → `UIWindow.motionEnded` hook (`staticCFunction` IMP; Kotlin block bridging would segfault on the `NSInteger` arg).
7. Feedback UI restyled to legacy design (was plain default material3).

## Repo rebrand

Formerly `updraft-sdk-android` → **`updraft-sdk`**, single home for both
platforms. Code-side done (rootProject name, POM URLs, README); GitHub rename +
archiving `updraft-sdk-ios` are manual release steps.
