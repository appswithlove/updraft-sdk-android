![Updraft: Mobile App Distribution](updraft.png)

[![Maven Central](https://maven-badges.sml.io/sonatype-central/com.appswithlove.updraft/updraft-sdk/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.appswithlove.updraft/updraft-sdk)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)
[![Bluesky](https://img.shields.io/badge/Bluesky-@appswithlove.bsky.social-blue.svg?style=flat)]([https://twitter.com/GetUpdraft](https://bsky.app/profile/appswithlove.bsky.social))


# Updraft SDK

Updraft SDK for Android & iOS — built with Kotlin Multiplatform and Compose Multiplatform.

Updraft is a super easy app delivery tool that allows you to simply and quickly distribute your app. It's super useful during beta-testing or if you want to deliver an app without going through the app store review processes. Your users get a link and are guided through the installation process with in a comprehensive web-app. Updraft works with Android and iOS apps and easily integrates with your IDE.
The SDK adds additional features to apps that are delivered with Updraft: Auto-update for your distributed apps and most importantly the collection of user feedback (shake or screenshot to report, annotate, send).

One codebase serves every kind of app: native Android (Views or Compose), Kotlin/Compose Multiplatform, and pure-Swift iOS apps via an XCFramework. This repository was formerly `updraft-sdk-android`; since 2.0.0 it is the single home of the Updraft SDK for both platforms and supersedes [`updraft-sdk-ios`](https://github.com/appswithlove/updraft-sdk-ios).

Updraft is built by App Agencies [Apps with love](https://appswithlove.com/) and [Moqod](https://moqod.com/). Learn more at [getupdraft.com](https://getupdraft.com/) or follow the latest news on [twitter](https://twitter.com/GetUpdraft).


## Requirements

- minSdkVersion >=23
- iOS 14.0+ (for `updraft-core` / `updraft-ui-compose` iOS targets)

## Installation

Updraft 2.0.0 ships as three Kotlin Multiplatform artifacts. Pick the one that matches your app:

| Artifact | Use case |
| --- | --- |
| `updraft-sdk` | Android (Views or Compose) apps that want everything out of the box: auto-update, feedback, and the built-in Compose UI, wired up automatically. |
| `updraft-core` | Logic only, no UI. Common for KMP apps with their own native UI per platform, or size-sensitive Android Views apps that don't want to pull in Compose. |
| `updraft-core` + `updraft-ui-compose` | Compose Multiplatform (CMP) apps that want the built-in feedback UI but want to host it themselves inside their own Compose tree. |

```kotlin
// libs.versions.toml
[versions]
updraft-sdk = "2.0.0"

[libraries]
updraft-sdk = { module = "com.appswithlove.updraft:updraft-sdk", version.ref = "updraft-sdk" }
updraft-core = { module = "com.appswithlove.updraft:updraft-core", version.ref = "updraft-sdk" }
updraft-ui-compose = { module = "com.appswithlove.updraft:updraft-ui-compose", version.ref = "updraft-sdk" }
```

```kotlin
// build.gradle.kts (Android all-in-one)
dependencies {
    implementation(libs.updraft.sdk)
}
```

```kotlin
// build.gradle.kts (KMP commonMain, logic only)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.updraft.core)
        }
    }
}
```

```kotlin
// build.gradle.kts (Compose Multiplatform, own UI hosting)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.updraft.core)
            implementation(libs.updraft.ui.compose)
        }
    }
}
```

`updraft-core` and `updraft-ui-compose` publish `iosArm64`/`iosSimulatorArm64`/`iosX64` targets alongside `androidTarget`, so the same dependency works from a KMP or Compose Multiplatform `commonMain` source set on both platforms. Pure-Swift consumers that don't want Kotlin/Compose tooling use the `UpdraftCore.xcframework` instead — see [Swift integration](#swift-integration) below. `updraft-sdk` (the all-in-one artifact) is Android-only.

`updraft-sdk-ios` is superseded by this SDK once 2.0.0 ships. Archiving that repo and adding a final README banner there are release-time actions, tracked in [`docs/kmp-migration-m1-status.md`](docs/kmp-migration-m1-status.md).

## Setup

`updraft-sdk` requires no manual initialization beyond starting the SDK. Auto-update alerts, the feedback shake-to-report flow, and the feedback screen are all wired up for you via `androidx.startup`.

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Updraft.start(
            UpdraftSettings(
                appKey = APP_KEY,
                sdkKey = SDK_KEY,
                // baseUrl defaults to the Updraft production API; override only for self-hosted instances
                logLevel = LogLevel.Debug, // Optional log level
                showFeedbackAlert = true, // Optional set if should show start alert
                feedbackEnabled = true, // force disabling feedback if needed
            ),
        )
    }
}
```

### Parameters
- <b>appKey</b>: Your app key obtained on [Updraft](https://getupdraft.com)
- <b>sdkKey</b>: Your sdk key obtained on [Updraft](https://getupdraft.com)

## AutoUpdate
Auto Update functionality can be enabled/disabled on [getupdraft.com](https://getupdraft.com/) in your app edit menu.

AutoUpdate work by comparing the build number of the app installed on the user's device and the app uploaded on GetUpdraft.

A prompt is displayed to the user if his installed version is lower than the version on Updraft.
Thus, the build number must be incremented for each new build release to trigger the auto-update process.

Micro version comparison is supported, for example version 1.2.3.2018080**4** is greater than version 1.2.3.2018080**3**

## Feedback

Feedback functionality can be enabled/disabled on [getupdraft.com](https://getupdraft.com/) in your app edit menu.

A prompt is shown to the user to inform him of the change of state of the feedback functionality.

If enabled, the user is explained how he can give feedback.
User can take a screenshot to give a feedback.

### Navigation stack

Each feedback upload includes the app's navigation stack, captured at the moment
feedback is triggered (e.g. on shake). By default the SDK reports the Android
activity stack or the iOS view controller chain. Single-activity apps (Compose,
Navigation Component) should plug in their navigation library via
`Updraft.navigationStackProvider` — return screen names ordered root to top:

**Jetpack Compose Navigation**

```kotlin
val navController = rememberNavController()
LaunchedEffect(navController) {
    Updraft.navigationStackProvider = {
        listOfNotNull(navController.currentBackStackEntry?.destination?.route)
    }
}
```

Or track the full back stack yourself with a destination listener:

```kotlin
val breadcrumbs = mutableListOf<String>()
navController.addOnDestinationChangedListener { _, destination, _ ->
    destination.route?.let { route ->
        breadcrumbs.remove(route)
        breadcrumbs.add(route)
    }
}
Updraft.navigationStackProvider = { breadcrumbs.toList() }
```

**Fragments (Navigation Component)**

```kotlin
Updraft.navigationStackProvider = {
    navController.currentBackStack.value.mapNotNull { it.destination.label?.toString() }
}
```

**SwiftUI (iOS)**

```swift
UpdraftKt.navigationStackProvider = { ["Home", "Settings"] } // from your router state
```

Setting the provider to `null` restores the platform default. Updraft's own
screens are always excluded.

The whole feature is optional: to not send any navigation information at all
(e.g. for privacy reasons), disable it in the settings — the provider and the
platform default are then never invoked:

```kotlin
UpdraftSettings(appKey = ..., sdkKey = ..., sendNavigationStack = false)
```

## Advanced setup: Logging

To check if data is send properly to Updraft and also see some additional SDK log data in the console, you can set different log levels.

Pass the desired level when constructing `UpdraftSettings`:

```kotlin
UpdraftSettings(
    appKey = APP_KEY,
    sdkKey = SDK_KEY,
    logLevel = LogLevel.Debug,
)
```

`LogLevel` is one of `None`, `Error`, `Debug`. Default level: <b>LogLevel.Error</b> => Only warnings and errors will be printed.

## Migrating from 1.x

Version 2.0.0 rebuilds the SDK on Kotlin Multiplatform. `updraft-sdk` is still a drop-in Android dependency and behaves the same at runtime, but the setup API changed:

| 1.x | 2.0.0 |
| --- | --- |
| `Settings().apply { appKey = ...; sdkKey = ... }` | `UpdraftSettings(appKey = ..., sdkKey = ..., ...)` — a constructor with named parameters instead of a mutable builder object |
| `Settings.LOG_LEVEL_DEBUG` / `LOG_LEVEL_ERROR` | `LogLevel.Debug` / `LogLevel.Error` / `LogLevel.None` |
| `Updraft.initialize(this, settings)` followed by `Updraft.getInstance()?.start()` | `Updraft.start(settings)` — a single call, no `initialize`/`getInstance()` step |
| `settings.isStoreRelease` | `UpdraftSettings(..., storeRelease = ...)` |
| `ScreenshotProvider` (custom screenshot capture) | Not supported yet in 2.0.0. Feedback screenshots are captured automatically; a custom-screenshot hook is planned for a follow-up release. |

Other things to know:
- Context is no longer passed manually — `updraft-sdk` picks it up via `androidx.startup`, so there's no `Application` context argument to `start()`.
- `updraft-sdk` now pulls in the Compose runtime to render its feedback UI. For a release build with R8/minification this adds roughly 2 MB to a non-Compose app (already-Compose apps see close to zero extra size, since the runtime is shared). If that's not acceptable for a size-sensitive Views app, depend on `updraft-core` directly instead (see below) — it's roughly 200–400 KB and has no UI dependency at all.

## KMP / Compose Multiplatform

### `updraft-core` only (bring your own UI)

Apps with their own native UI per platform (or Views apps avoiding the Compose dependency) depend on `updraft-core` in `commonMain` and hook into the feedback flow directly:

```kotlin
Updraft.start(UpdraftSettings(appKey = APP_KEY, sdkKey = SDK_KEY))

Updraft.setFeedbackUiPresenter { screenshotPng ->
    // Show your own feedback UI, e.g. navigate to a screen or present a sheet.
}

// From your own UI, once the user submits feedback:
Updraft.sendFeedback(screenshot, type, description, email)
    .collect { progress -> /* upload progress, 0.0..1.0 */ }

// Once your feedback UI closes (submitted, cancelled, or dismissed), you MUST call this to
// re-arm shake-to-report detection. Without it, shaking the device again won't trigger feedback.
Updraft.onFeedbackUiClosed()
```

Subscribe to `Updraft.events: SharedFlow<UpdraftEvent>` to react to update prompts, feedback hints, and errors in your own UI layer.

### `updraft-core` + `updraft-ui-compose` (embed the built-in UI)

Compose Multiplatform apps that want Updraft's built-in feedback UI, but hosted inside their own Compose tree, add `updraft-ui-compose` and use `UpdraftEventHost` plus `FeedbackScreen`:

```kotlin
Updraft.start(UpdraftSettings(appKey = APP_KEY, sdkKey = SDK_KEY))

@Composable
fun App() {
    // Renders update/feedback-hint dialogs as an overlay; call anywhere in your Compose tree.
    UpdraftEventHost(
        events = Updraft.events,
        onFeedbackRequested = {
            // Obtain the screenshot captured for this request, then navigate to your feedback screen/route.
            val screenshotPng = Updraft.takePendingScreenshot()
            navigateToFeedback(screenshotPng)
        },
    )

    // your app content
}

@Composable
fun FeedbackRoute(screenshotPng: ByteArray?, onClose: () -> Unit) {
    FeedbackScreen(
        screenshotPng = screenshotPng,
        onClose = {
            onClose()
            // Re-arms shake-to-report detection.
            Updraft.onFeedbackUiClosed()
        },
    )
}
```

### iOS

Both setups above work unchanged in `iosMain` — `Updraft.start`, `Updraft.events`, `Updraft.setFeedbackUiPresenter`, `Updraft.sendFeedback`, etc. are the same `commonMain` API on iOS as on Android. There's no iOS-specific initialization step; call `Updraft.start(...)` once from your shared Kotlin entry point (e.g. the function your `iOSApp.swift` calls on launch).

Compose Multiplatform apps that added `updraft-ui-compose` get the built-in feedback UI wired up for free on iOS via `UpdraftIos.autoWire()` — it presents the update/feedback dialogs and the feedback screen on top of the current key window, no `UpdraftEventHost` needed:

```kotlin
// commonMain or iosMain, called once from your app's iOS entry point
fun startUpdraft() {
    Updraft.start(UpdraftSettings(appKey = APP_KEY, sdkKey = SDK_KEY))
    UpdraftIos.autoWire()
}
```

Shake detection uses `CMMotionManager` (accelerometer), which needs no `Info.plist` permission entry.

### Swift integration

`updraft-core` builds an `UpdraftCore.xcframework` for pure-Swift consumers that don't want to pull in Kotlin/Compose tooling:

```
./gradlew :updraft-core:assembleUpdraftCoreXCFramework
```

The framework is written to `updraft-core/build/XCFrameworks/release/UpdraftCore.xcframework`. Drag it into an Xcode project or wrap it as an SPM binary target; how it's distributed (e.g. published alongside a release, hosted as a zip) is a release-time decision, out of scope for M2.

`UpdraftCore.xcframework` exposes the same `commonMain` API as above — `Updraft`, `UpdraftSettings`, `Updraft.events`, etc. — callable from Swift:

```swift
import UpdraftCore

Updraft.shared.start(
    settings: UpdraftSettings(
        appKey: APP_KEY,
        sdkKey: SDK_KEY,
        baseUrl: UpdraftSettings.companion.BASE_URL_PROD,
        logLevel: .error,
        showFeedbackAlert: true,
        feedbackEnabled: true,
        storeRelease: false
    )
)
```

Kotlin default parameter values aren't exposed to the generated Objective-C/Swift header, so every `UpdraftSettings` argument must be passed explicitly from Swift (unlike Kotlin callers, which can rely on the defaults shown in [Setup](#setup)).

Since this framework only wraps `updraft-core`, it does not include the built-in Compose feedback UI (`updraft-ui-compose`, incl. `UpdraftFeedbackViewController`) — a pure-Swift, non-Compose app builds its own feedback screen and drives it via `Updraft.setFeedbackUiPresenter` / `Updraft.sendFeedback`, the same way an `updraft-core`-only Android/KMP app does. `UpdraftFeedbackViewController` and `UpdraftIos.autoWire()` are only reachable from a Kotlin/Compose Multiplatform app's own shared Kotlin code, as in the [`iOS` section](#ios) above.

## Local Development

In order to locally develop this plugin, the sample project can be used for easy testing. Additionally, the gradle task `publishToMavenLocal` allows to install the current version to Maven Local.

## Release

Pushing to the `production` branch triggers the [publish workflow](.github/workflows/publish.yml), which publishes `updraft-core`, `updraft-ui-compose`, and `updraft-sdk` to Maven Central.
