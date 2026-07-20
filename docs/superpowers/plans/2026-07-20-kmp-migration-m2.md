# Updraft SDK KMP Migration — M2 Implementation Plan (iOS)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add iOS support to the KMP SDK — iOS targets + `iosMain` actuals in `updraft-core` and `updraft-ui-compose`, a CMP sample app (androidApp + iosApp), XCFramework export for pure-Swift consumers, and CI so every branch is buildable/testable — fulfilling the remaining acceptance criteria of issue #13. No release in M2; publish workflow is prepared but nothing ships.

**Architecture:** Extends M1 (spec: `docs/superpowers/specs/2026-07-16-kmp-migration-design.md`). All logic already lives in `commonMain` behind expect/actual seams; M2 fills the iOS side: CoreMotion shake, UIWindow screenshot, UIApplication url/foreground, NSUserDefaults store, Skia image codec, `ComposeUIViewController` feedback host. Branch: `feature/kmp-migration-m2` (off `feature/kmp-migration-m1`).

**Tech Stack:** Kotlin 2.2.21 Multiplatform (iosArm64, iosSimulatorArm64, iosX64), Ktor Darwin engine, Compose Multiplatform 1.9.3, Skiko (bundled with CMP) for image codec, XCFramework via kotlin.native cocoapods-free `XCFramework()` helper, GitHub Actions (macos runner).

## Global Constraints

- Kotlin 2.2.21, AGP 8.13.0, compileSdk 36, minSdk 23, JVM 11 — unchanged.
- iOS deployment target: **iOS 14.0** (matches updraft-sdk-ios floor; set via `it.binaries.framework` config and sample project).
- Package root stays `com.appswithlove.updraft`. No public API changes to `commonMain` symbols shipped in M1 (additive only).
- All M1 tests must stay green (`./gradlew build`). iOS-target compile checks: `./gradlew :updraft-core:compileKotlinIosSimulatorArm64 :updraft-ui-compose:compileKotlinIosSimulatorArm64`; iOS unit tests: `./gradlew :updraft-core:iosSimulatorArm64Test`.
- Shake constants verbatim (shared common math already): threshold 2.7f g via `isShakeGForce`, slop 500 ms.
- SharedPrefs/NSUserDefaults key names verbatim: suite `feedback_enabled_storage`, key `is_feedback_enabled_property`.
- NEVER add Co-Authored-By or AI attribution to commits. Commit after every task. NOTHING is released/published in M2 — workflow changes are dry-run-verifiable only (`publishToMavenLocal`).
- iOS work requires macOS + Xcode; all verify commands in this plan run on this machine (darwin).
- Apple API adaptation rule: if a Kotlin/Native interop signature differs from the plan's code (argument labels, nullability), adapt minimally and note in the report — behavior contracts and key names stay as written.

---

### Task 1: iOS targets on `updraft-core` + Ktor Darwin

**Files:**
- Modify: `updraft-core/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: `iosArm64()`, `iosSimulatorArm64()`, `iosX64()` targets; `iosMain`/`iosTest` source sets; catalog entry `ktor-client-darwin`. Expect declarations will fail to compile for iOS until Tasks 2–3 add actuals — so this task ONLY adds targets + stub actuals marked `TODO()` is NOT allowed; instead this task adds targets AND the two simplest actuals (Task 2 content is merged here? No —) **this task adds targets and verifies with `linkDebugFrameworkIosSimulatorArm64` deferred**: compile will fail on missing actuals, so the verify step for this task is limited to configuration (`./gradlew :updraft-core:tasks` lists ios targets). Tasks 2–3 restore full compile. Commit together with Task 2 if the intermediate state bothers CI (single-commit allowance).

- [ ] **Step 1: Catalog entry**

`gradle/libs.versions.toml` under `[libraries]`:

```toml
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

- [ ] **Step 2: Targets + source sets**

In `updraft-core/build.gradle.kts` inside `kotlin { }` after `androidTarget { ... }`:

```kotlin
listOf(
    iosArm64(),
    iosSimulatorArm64(),
    iosX64(),
).forEach { target ->
    target.binaries.framework {
        baseName = "UpdraftCore"
        isStatic = true
    }
}
```

and inside `sourceSets { }`:

```kotlin
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

- [ ] **Step 3: Verify configuration**

Run: `./gradlew :updraft-core:tasks --group build | grep -i ios`
Expected: ios compile/link tasks listed. (Full compile deferred to Task 3's verify.)

- [ ] **Step 4: Commit** (allowed to fold into Task 2's commit if compile-breaking intermediate state)

```bash
git add gradle/libs.versions.toml updraft-core/build.gradle.kts
git commit -m "Add iOS targets and Ktor Darwin engine to updraft-core"
```

---

### Task 2: iosMain actuals — `KeyValueStore` + `AppInfo`

**Files:**
- Create: `updraft-core/src/iosMain/kotlin/com/appswithlove/updraft/platform/KeyValueStore.ios.kt`
- Create: `updraft-core/src/iosMain/kotlin/com/appswithlove/updraft/platform/AppInfo.ios.kt`

**Interfaces:**
- Consumes: `KeyValueStore` interface, `AppInfo` class, expect fns (M1 Task 4).
- Produces: NSUserDefaults-suite-backed store; AppInfo from NSBundle/UIDevice.

- [ ] **Step 1: Implement**

`KeyValueStore.ios.kt`:

```kotlin
package com.appswithlove.updraft.platform

import platform.Foundation.NSUserDefaults

private class UserDefaultsKeyValueStore(private val defaults: NSUserDefaults) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    UserDefaultsKeyValueStore(NSUserDefaults(suiteName = name))
```

`AppInfo.ios.kt`:

```kotlin
package com.appswithlove.updraft.platform

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual fun currentAppInfo(): AppInfo {
    val info = NSBundle.mainBundle.infoDictionary
    val versionCode = (info?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: -1L
    val versionName = info?.get("CFBundleShortVersionString") as? String ?: ""
    val device = UIDevice.currentDevice
    return AppInfo(
        versionCode = versionCode,
        versionName = versionName,
        systemVersion = device.systemVersion,
        deviceName = device.model,
        deviceUuid = device.identifierForVendor?.UUIDString ?: "",
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add updraft-core/src/iosMain/
git commit -m "Add iOS actuals for KeyValueStore and AppInfo"
```

---

### Task 3: iosMain actuals — Platform seams (shake, screenshot, url, foreground)

**Files:**
- Create: `updraft-core/src/iosMain/kotlin/com/appswithlove/updraft/platform/Platform.ios.kt`
- Test: `updraft-core/src/iosTest/kotlin/com/appswithlove/updraft/platform/IosSmokeTest.kt`

**Interfaces:**
- Consumes: `ShakeDetector`/`ScreenshotGrabber`/`AppForegroundObserver` interfaces + `isShakeGForce` (M1 Task 7).
- Produces: CoreMotion-based shake (reuses common math, 60 Hz updates, 500 ms slop, one-shot `enabled` flag re-armed via `Updraft.onFeedbackUiClosed()` exactly like Android), key-window screenshot → PNG bytes, `UIApplication.openURL`, foreground observer via `NSNotificationCenter` (`UIApplicationDidBecomeActiveNotification` / `UIApplicationDidEnterBackgroundNotification`).

- [ ] **Step 1: Implement**

`Platform.ios.kt`:

```kotlin
package com.appswithlove.updraft.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIWindow
import platform.posix.memcpy

private class IosShakeDetector(private val onShake: () -> Unit) : ShakeDetector {
    private val motionManager = CMMotionManager()
    private var shakeTimestamp = 0.0
    private var enabled = true

    override fun start() {
        if (!motionManager.accelerometerAvailable) return
        motionManager.accelerometerUpdateInterval = 1.0 / 60.0
        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            val acceleration = data?.acceleration ?: return@startAccelerometerUpdatesToQueue
            acceleration.useContents {
                // CoreMotion reports in g units already; gravity divisor = 1.0
                if (!isShakeGForce(x.toFloat(), y.toFloat(), z.toFloat(), 1.0f)) return@useContents
                val now = NSDate().timeIntervalSince1970
                if (shakeTimestamp + SHAKE_SLOP_SECONDS > now) return@useContents
                shakeTimestamp = now
                if (enabled) {
                    enabled = false
                    onShake()
                }
            }
        }
    }

    override fun stop() {
        motionManager.stopAccelerometerUpdates()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    companion object {
        private const val SHAKE_SLOP_SECONDS = 0.5
    }
}

actual fun createShakeDetector(onShake: () -> Unit): ShakeDetector = IosShakeDetector(onShake)

@OptIn(ExperimentalForeignApi::class)
private class IosScreenshotGrabber : ScreenshotGrabber {
    override fun capturePng(): ByteArray? {
        val window = keyWindow() ?: return null
        val bounds = window.bounds
        val renderer = UIGraphicsImageRenderer(bounds = bounds)
        val image = renderer.imageWithActions { _ ->
            window.drawViewHierarchyInRect(bounds, afterScreenUpdates = false)
        }
        val nsData = UIImagePNGRepresentation(image) ?: return null
        val length = nsData.length.toInt()
        if (length == 0) return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
        return bytes
    }

    private fun keyWindow(): UIWindow? =
        UIApplication.sharedApplication.windows
            .filterIsInstance<UIWindow>()
            .firstOrNull { it.isKeyWindow() }
}

actual fun createScreenshotGrabber(): ScreenshotGrabber = IosScreenshotGrabber()

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

private class IosForegroundObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
) : AppForegroundObserver {
    override fun start() {
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(UIApplicationDidBecomeActiveNotification, `object` = null, queue = NSOperationQueue.mainQueue) { onForeground() }
        center.addObserverForName(UIApplicationDidEnterBackgroundNotification, `object` = null, queue = NSOperationQueue.mainQueue) { onBackground() }
    }
}

actual fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver =
    IosForegroundObserver(onForeground, onBackground)
```

(Missing imports like `usePinned`/`addressOf` from `kotlinx.cinterop` — add as compiler demands; interop-label adaptation rule applies.)

- [ ] **Step 2: iOS smoke test**

`IosSmokeTest.kt`:

```kotlin
package com.appswithlove.updraft.platform

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosSmokeTest {
    @Test
    fun keyValueStore_roundTrips() {
        val store = createKeyValueStore("updraft_test_suite")
        store.putBoolean("k", true)
        assertTrue(store.getBoolean("k", false))
    }

    @Test
    fun appInfo_populates() {
        val info = currentAppInfo()
        assertNotNull(info.systemVersion)
    }
}
```

- [ ] **Step 3: Verify — full iOS compile + iOS tests + Android regression**

Run: `./gradlew :updraft-core:compileKotlinIosSimulatorArm64 :updraft-core:iosSimulatorArm64Test :updraft-core:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (common tests now also run on iOS runner).

- [ ] **Step 4: Commit**

```bash
git add updraft-core/
git commit -m "Add iOS actuals for shake, screenshot, url, and foreground observer"
```

---

### Task 4: iOS targets on `updraft-ui-compose` + Skia image codec actuals

**Files:**
- Modify: `updraft-ui-compose/build.gradle.kts` (same 3 ios targets, framework baseName `UpdraftUI`)
- Create: `updraft-ui-compose/src/iosMain/kotlin/com/appswithlove/updraft/ui/feedback/ImageCodec.ios.kt`

**Interfaces:**
- Consumes: `expect fun decodePng(bytes): ImageBitmap`, `expect fun renderAnnotated(base, paths, canvasSize): ByteArray`, `mapToBitmapSpace` (M1 Task 11 + final-fix wave — CHECK actual current signature in `updraft-ui-compose/src/commonMain/.../ImageCodec.kt` before writing; the fix wave changed `renderAnnotated` to take canvas size).
- Produces: Skia-based actuals usable from CMP on iOS.

- [ ] **Step 1: Targets in build file** (mirror Task 1's block, baseName `"UpdraftUI"`).

- [ ] **Step 2: Implement Skia actuals**

`ImageCodec.ios.kt` (adapt names to the CURRENT common expect signatures):

```kotlin
package com.appswithlove.updraft.ui.feedback

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import com.appswithlove.updraft.ui.drawing.DrawnPath
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.Surface

actual fun decodePng(bytes: ByteArray): ImageBitmap =
    Image.makeFromEncoded(bytes).toComposeImageBitmap()

actual fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>, canvasSize: IntSize): ByteArray {
    val baseImage = Image.makeFromEncoded(base)
    val surface = Surface.makeRasterN32Premul(baseImage.width, baseImage.height)
    val canvas: Canvas = surface.canvas
    canvas.drawImage(baseImage, 0f, 0f)
    paths.forEach { drawn ->
        val paint = Paint().apply {
            color = drawn.color.hashCode() // REPLACE: use drawn.color.toArgb() — androidx.compose.ui.graphics.toArgb works in common
            mode = PaintMode.STROKE
            strokeWidth = drawn.strokeWidthPx
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
            isAntiAlias = true
        }
        val path = Path()
        val mapped = drawn.points.map { mapToBitmapSpace(it, canvasSize, baseImage.width, baseImage.height) }
        mapped.firstOrNull()?.let { path.moveTo(it.x, it.y) }
        mapped.drop(1).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
}
```

IMPORTANT: mirror whatever coordinate-mapping the Android actual does today (read `ImageCodec.android.kt` first) so behavior is identical; `color.toArgb()` (androidx.compose.ui.graphics) is the correct color conversion, the hashCode line above is a placeholder marker that MUST be replaced.

- [ ] **Step 3: Verify**

Run: `./gradlew :updraft-ui-compose:compileKotlinIosSimulatorArm64 :updraft-ui-compose:iosSimulatorArm64Test :updraft-ui-compose:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; existing `ImageCodecTest` mapping tests now also pass on iOS.

- [ ] **Step 4: Commit**

```bash
git add updraft-ui-compose/
git commit -m "Add iOS targets and Skia image codec to updraft-ui-compose"
```

---

### Task 5: iOS feedback host — `UpdraftFeedbackViewController` + auto-wire

**Files:**
- Create: `updraft-ui-compose/src/iosMain/kotlin/com/appswithlove/updraft/ui/ios/UpdraftIos.kt`

**Interfaces:**
- Consumes: `FeedbackScreen(screenshotPng, onClose)`, `UpdraftEventHost(events, onFeedbackRequested)`, `Updraft.events/takePendingScreenshot/onFeedbackUiClosed`.
- Produces:

```kotlin
// Swift-callable factory: present this from any UIViewController
fun UpdraftFeedbackViewController(screenshotPng: ByteArray?, onClose: () -> Unit): UIViewController

// Optional auto mode (parity with Android auto-UI): call once after Updraft.start()
fun UpdraftIos.autoWire()   // collects Updraft.events on Dispatchers.Main:
                             // dialog events -> presents ComposeUIViewController hosting UpdraftEventHost
                             // FeedbackRequested -> presents UpdraftFeedbackViewController(takePendingScreenshot())
                             // presentation via topmost view controller of key window; onClose -> dismiss + onFeedbackUiClosed()
```

Implementation via `androidx.compose.ui.window.ComposeUIViewController { ... }`. Topmost-VC helper: walk `presentedViewController` chain from key window's `rootViewController`. Keep the collector in a `MainScope()` singleton; idempotent like Android's `autoWire`.

- [ ] **Step 1: Implement** (structure above; exact modal style `UIModalPresentationFullScreen` for feedback, `.OverFullScreen` for dialogs).
- [ ] **Step 2: Verify** `./gradlew :updraft-ui-compose:compileKotlinIosSimulatorArm64`
- [ ] **Step 3: Commit** `git commit -m "Add iOS feedback view controller and event auto-wiring"`

---

### Task 6: Sample restructure — `sample/` CMP app (androidApp + iosApp)

**Files:**
- Create: `sample/composeApp/build.gradle.kts` (KMP: androidTarget + ios targets; commonMain deps: core + ui-compose + compose material3; android application config copied from current `app/` incl. updraft upload plugin block)
- Create: `sample/composeApp/src/commonMain/kotlin/com/appswithlove/updraftsdk/App.kt` (shared `@Composable fun SampleApp()` — simple screen: app title, "Check for update" button → `Updraft.checkForUpdate()`, "Give feedback" button → `Updraft.showFeedback()`)
- Create: `sample/composeApp/src/androidMain/` (Application class calling `Updraft.start(...)` staging settings, MainActivity hosting `SampleApp()`, manifest — port from current `app/`)
- Create: `sample/composeApp/src/iosMain/kotlin/.../MainViewController.kt` (`fun MainViewController() = ComposeUIViewController { SampleApp() }` + `Updraft.start(...)` + `UpdraftIos.autoWire()` in an init fn called from Swift)
- Create: `sample/iosApp/` — minimal Xcode project (iosApp.xcodeproj, `iOSApp.swift` with `@main` SwiftUI App wrapping `MainViewController()` via `UIViewControllerRepresentable`, Info.plist). Generate with the standard KMP wizard layout; embed `composeApp` framework via the Gradle-embedAndSign build phase.
- Delete: `app/` (git rm -r) after sample builds
- Modify: `settings.gradle.kts` (remove `:app`, add `:sample:composeApp`)

**Steps:**
- [ ] Build shared module first: `./gradlew :sample:composeApp:assembleDebug`
- [ ] Android sample installs + runs (manual smoke or `:sample:composeApp:installDebug` if device attached; otherwise assemble is the gate)
- [ ] iOS sample builds: `xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug build` (or `-destination 'platform=iOS Simulator,name=iPhone 16'`)
- [ ] `git rm -r app/` + settings update; full `./gradlew build` green
- [ ] Commit: `git commit -m "Replace app/ with CMP sample (androidApp + iosApp)"` (split into 2 commits if cleaner: add sample, then remove app)

---

### Task 7: XCFramework export for pure-Swift consumers

**Files:**
- Modify: `updraft-core/build.gradle.kts`, `updraft-ui-compose/build.gradle.kts`

**Steps:**
- [ ] Use `kotlin("native.cocoapods")`-free approach: `import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework`; create one umbrella XCFramework from `updraft-core` (name `UpdraftCore`) — framework config `export(project(":updraft-core"))` not needed for core itself; for a combined Swift-consumable SDK later evaluate an umbrella module, out of scope now.

```kotlin
// updraft-core/build.gradle.kts, inside kotlin { }
val xcf = XCFramework("UpdraftCore")
listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
    target.binaries.framework {
        baseName = "UpdraftCore"
        isStatic = true
        xcf.add(this)
    }
}
```

(Replace the Task-1 target block with this — don't declare targets twice.)
- [ ] Verify: `./gradlew :updraft-core:assembleUpdraftCoreXCFramework` → `updraft-core/build/XCFrameworks/release/UpdraftCore.xcframework` exists.
- [ ] Document in README: Swift integration = drag XCFramework / SPM binary target (distribution mechanics = release-time decision, out of M2 scope; note it).
- [ ] Commit: `git commit -m "Add XCFramework export for updraft-core"`

---

### Task 8: CI workflow — every branch buildable/testable

**Files:**
- Create: `.github/workflows/ci.yml`

**Steps:**
- [ ] Workflow: on `push` to `feature/**` + `pull_request` to `main`. Job 1 `android` (ubuntu-latest): JDK 17, gradle cache, `./gradlew build` (all module unit tests). Job 2 `ios` (macos-14): JDK 17, `./gradlew :updraft-core:iosSimulatorArm64Test :updraft-ui-compose:iosSimulatorArm64Test :updraft-core:assembleUpdraftCoreXCFramework` + `xcodebuild` of sample/iosApp (simulator, no signing: `CODE_SIGNING_ALLOWED=NO`).
- [ ] Verify locally what's verifiable: `./gradlew build` green; YAML lint (actionlint if available, else careful review). Real verification = push to the M2 branch and watch the run.
- [ ] Commit + push, confirm both jobs green on GitHub before proceeding: `gh run watch`.
- [ ] Commit: `git commit -m "Add CI workflow for Android and iOS builds"`

---

### Task 9: Prepare publish workflow for all modules (NO release)

**Files:**
- Modify: `.github/workflows/publish.yml`

**Steps:**
- [ ] Change the publish step to publish root-level all modules: `./gradlew publishAllPublicationsToMavenCentralRepository` (vanniktech plugin task name — verify against plugin 0.34.0 docs/tasks: `./gradlew tasks --all | grep -i publish`) so `updraft-core`, `updraft-ui-compose`, `updraft-sdk` all ship together. KMP modules publish per-target artifacts automatically (android + ios klibs + metadata) — macos runner REQUIRED for ios artifacts: switch workflow to `runs-on: macos-14`.
- [ ] Reconcile trigger with README Release section (workflow reality wins — document whichever is true).
- [ ] Dry-run verify: `./gradlew publishToMavenLocal` → `~/.m2/repository/com/appswithlove/updraft/` contains updraft-core (incl. `-iosarm64`, `-iossimulatorarm64`, `-iosx64` variants), updraft-ui-compose, updraft-sdk, all 2.0.0. DO NOT run any remote publish task.
- [ ] Commit: `git commit -m "Publish all modules from macos runner (release still manual)"`

---

### Task 10: Docs — iOS setup, Swift integration, updraft-sdk-ios deprecation note

**Files:**
- Modify: `README.md`, `docs/kmp-migration-m1-status.md` (mark M2 items), `docs/superpowers/specs/2026-07-16-kmp-migration-design.md` (tick M2 milestone)

**Steps:**
- [ ] README: replace "iOS = M2, not yet available" notes with real setup: KMP commonMain usage now covers iOS; CMP apps get `UpdraftIos.autoWire()`; Swift apps section (XCFramework, `UpdraftFeedbackViewController` from Swift, `Updraft.start` from Swift via the framework); iOS 14+ requirement; note that permission-free APIs only (CoreMotion accelerometer needs no permission).
- [ ] README + issue: state that updraft-sdk-ios is superseded once 2.0.0 ships; actual deprecation (repo archive, final README banner there) is a release-time action, listed in status doc.
- [ ] Update `docs/kmp-migration-m1-status.md`: retitle relevant section or add `## M2 status` — sample keys still needed for device pass on BOTH platforms.
- [ ] Full build green; commit: `git commit -m "Document iOS setup and Swift integration"`

---

## Verification gate for M2 completion (before any merge)

1. CI green on `feature/kmp-migration-m2` (android + ios jobs).
2. `./gradlew build` + both `iosSimulatorArm64Test` suites green locally.
3. Sample iosApp runs in simulator: start → hint dialog; simulator shake (Device > Shake) → feedback screen appears; annotate; form renders. (Network calls need real keys — UI flow verifiable without.)
4. Android sample unchanged behavior (M1 checklist).
5. Nothing published anywhere; branches `feature/kmp-migration-m1` and `feature/kmp-migration-m2` both on origin.

## Explicit non-goals in M2

- No Maven Central / GitHub release, no repo archive of updraft-sdk-ios (release-time actions).
- No SPM package manifest polish (distribution decision deferred).
- M1 follow-up list (stroke width, rotation state, races) — separate wave, not M2.
