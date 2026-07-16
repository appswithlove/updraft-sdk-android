# Updraft SDK — KMP/CMP Migration Design

Date: 2026-07-16
Issue: [#13](https://github.com/appswithlove/updraft-sdk-android/issues/13)

## Goal

One SDK codebase serving three consumer types:

1. Android-only native apps (existing consumers)
2. KMP apps without shared UI (shared logic, native feedback UI per platform)
3. CMP apps (fully shared, including feedback UI)

The KMP SDK replaces `updraft-sdk-ios` once iOS support ships (M2).

## Decisions

- **Layered artifacts** (core / compose UI / Android wrapper) — not one fat artifact, not separate SDKs.
- **Compose-first UI**: existing Views UI (Fragments, ViewBinding, `FreeDrawView`) is deleted and replaced by a Compose Multiplatform UI rendered via `ComposeView` on Android. Accepted cost: ~2 MB (release, R8) for Views-only consumer apps; ~0 for Compose apps. Escape hatch: `updraft-core` + own UI (~300 KB).
- **In-place migration** of this repo. Fresh-repo/rename decision deferred (moving later is a plain push, no lock-in).
- **M1 scope**: core + CMP UI with Android target only. iOS actuals in M2.

## Module architecture

```
updraft-sdk-android/ (repo)
├── updraft-core/         KMP library
│   ├── commonMain/       UpdraftSettings, Updraft entry, interactors,
│   │                     Ktor api client + models, version comparison,
│   │                     expect: ScreenshotProvider, ShakeDetector,
│   │                     UpdateInstaller, KeyValueStore, AppInfo
│   ├── androidMain/      actuals: SensorManager shake, Activity screenshot,
│   │                     APK/link install, SharedPrefs, androidx.startup
│   │                     init, current-activity tracking
│   └── iosMain/          (M2) motionEnded shake, UIWindow screenshot,
│                         itms-services install, NSUserDefaults
├── updraft-ui-compose/   CMP module
│   └── commonMain/       FeedbackForm, DrawingCanvas (FreeDrawView rewrite
│                         in Compose Canvas + pointerInput), theme,
│                         strings via compose-resources (en/de)
├── updraft-sdk/          Android artifact, existing Maven coords
│   └── thin wrapper: core + ui-compose, one Activity hosting ComposeView.
│       Views UI, Fragments, ViewBinding, FreeDrawView, ink lib deleted.
└── sample/               replaces app/ — CMP sample (androidApp in M1,
                          iosApp in M2)
```

Published artifacts: `updraft-core`, `updraft-ui-compose`, `updraft-sdk`
(coords `com.appswithlove.updraft:updraft-sdk` unchanged).

Tech swaps in core: Retrofit → Ktor (OkHttp engine on Android), RxJava2 → coroutines/Flow, SharedPrefs → `KeyValueStore` expect/actual, Loco gradle plugin → compose-resources.

## Public API (commonMain)

```kotlin
class UpdraftSettings(
    val appKey: String,
    val sdkKey: String,
    val baseUrl: String = PROD,
    val logLevel: LogLevel = Error,
    val showFeedbackAlert: Boolean = true,
    val feedbackEnabled: Boolean = true,
)

object Updraft {
    fun start(settings: UpdraftSettings)
    fun checkForUpdate()
    fun showFeedback()
    fun setFeedbackUiPresenter(presenter: FeedbackUiPresenter) // KMP-no-UI hook
    fun sendFeedback(screenshot: ByteArray?, text: String, type: FeedbackType)
    val events: Flow<UpdraftEvent> // UpdateAvailable, FeedbackSent, Error
}
```

Android `Context` acquired internally via androidx.startup — no context parameter in common API.

### Consumer wiring

- **Android-only**: `updraft-sdk` dep, `Updraft.start(settings)` in `Application.onCreate`. Shake→feedback and update dialog automatic, as today.
- **KMP no shared UI**: `updraft-core` in commonMain. `setFeedbackUiPresenter` for own UI; submit via `sendFeedback`.
- **CMP**: core + `updraft-ui-compose`. Default: SDK opens own host (Activity / UIViewController) with the Compose UI. Also exposes embeddable `UpdraftFeedbackDialog()` composable.

### Flows

- **Update**: start → Ktor `getLastVersion` → version compare (micro-version logic ported as-is) → `UpdateAvailable` event → default dialog or app-handled → `UpdateInstaller.install(url)`.
- **Feedback**: shake (expect/actual) → screenshot capture → Compose form + drawing canvas → multipart upload via Ktor (`onUpload` progress → Flow).

## Migration steps (in-place)

1. Gradle: KMP + CMP plugins in version catalog; create `updraft-core` (android target first); `git mv` non-UI classes; split commonMain/androidMain as classes lose Android deps.
2. Port order in core: models/Settings → version compare → Ktor ApiWrapper (remove Retrofit + RxJava2) → interactors as suspend/Flow → managers behind expect/actual.
3. `updraft-ui-compose`: FeedbackForm + DrawingCanvas fresh in CMP; port FreeDrawView logic (paths, undo/redo, color/width) 1:1.
4. `updraft-sdk`: gut to wrapper; delete Views stack.
5. `app/` → `sample/` (CMP structure).
6. M2: ios targets + actuals, XCFramework export (SPM/CocoaPods), iosApp sample, deprecate `updraft-sdk-ios`.

## Compatibility

- `updraft-sdk` 2.0.0 = breaking (API rename `Settings`→`UpdraftSettings`, no `initialize(context, …)`, Compose dependency). Major bump.
- 1.x branch frozen, hotfixes only.
- README migration table 1.x → 2.0.

## Testing

- commonTest: version comparison (existing cases + micro-version), interactors with Ktor MockEngine, settings validation. Kotest + Turbine.
- ui-compose: Compose UI tests (form validation, drawing) via Robolectric on Android.
- Manual: sample app on device — shake, annotate, submit, update dialog against staging.

## Milestones

- **M1** (releasable): core KMP (android target) + ui-compose + rewrapped `updraft-sdk` 2.0.0. Android feature parity on new stack.
- **M2**: iOS targets + actuals + XCFramework + iosApp sample; replaces `updraft-sdk-ios`.
- **M3** (optional): fresh-repo/rename decision, SPM polish, further targets.

## Risks

- Compose runtime forced on `updraft-sdk` consumers (accepted, see Decisions).
- Drawing canvas rewrite fidelity — mitigate by porting FreeDraw math 1:1.
- Loco → compose-resources string pipeline swap (en/de carried over).
- Multipart upload progress in Ktor — supported via `onUpload`.

## Post-migration reminder

- [ ] **After M1 ships: revisit options for Views-based Android apps.** Evaluate whether the ~2 MB Compose cost needs a dedicated answer — e.g. documented `updraft-core`-only recipe with a minimal Views feedback screen, a community `updraft-ui-views` artifact, or confirmation that no consumer actually needs it.

## Size impact (reference)

| Consumer | Deps | Added size (release, R8) |
|---|---|---|
| Android app already using Compose | `updraft-sdk` | ~100–300 KB |
| Views-only Android app | `updraft-sdk` | ~2 MB |
| Views-only app, minimal | `updraft-core` + own UI | ~200–400 KB |
| KMP no shared UI | `updraft-core` | as above per platform |
| CMP app | core + ui-compose | ~0 marginal |

Precedent: Sentry, RevenueCat, Firebase, Coil all ship core + optional UI artifacts; Instabug/Shake bake UI in at 3–5 MB.
