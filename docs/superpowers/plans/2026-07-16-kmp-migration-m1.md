# Updraft SDK KMP Migration — M1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the repo into `updraft-core` (KMP, Android target) + `updraft-ui-compose` (CMP) + rewrapped `updraft-sdk` 2.0.0, replacing Retrofit/RxJava2 with Ktor/coroutines and Views UI with Compose, with full Android feature parity.

**Architecture:** Layered artifacts per spec `docs/superpowers/specs/2026-07-16-kmp-migration-design.md`. Core holds all logic in `commonMain` behind expect/actual platform seams; ui-compose holds the feedback/update UI; updraft-sdk is a thin Android wrapper keeping existing Maven coords.

**Tech Stack:** Kotlin 2.2.21 Multiplatform, Compose Multiplatform 1.9.x, Ktor 3.x, kotlinx.serialization, kotlinx.coroutines, kotlin-test + Turbine, androidx.startup.

## Global Constraints

- Kotlin `2.2.21`, AGP `8.13.0`, compileSdk `36`, minSdk `23`, JVM target `11` (from existing catalog — do not lower/raise).
- Package root stays `com.appswithlove.updraft`.
- `updraft-sdk` Maven coords stay `com.appswithlove.updraft:updraft-sdk`; new artifacts `updraft-core`, `updraft-ui-compose`. Version `2.0.0` everywhere.
- API endpoints and field names verbatim: `check_last_version/`, `get_last_version/`, `feedback-mobile/`, `feedback-mobile-enabled/`; multipart fields `image`, `app_key`, `sdk_key`, `tag`, `description`, `email`, `build_version`, `system_version`, `device_name`, `device_uuid`.
- Base URLs verbatim: prod `https://app.getupdraft.com/api/`, staging `https://u2.mqd.me/api/`.
- Feedback tag strings verbatim: `design`, `feedback`, `bug`.
- SharedPreferences file/key names verbatim (`feedback_enabled_storage`, `is_feedback_enabled_property`) — existing installs must not re-show alerts.
- Shake constants verbatim: threshold `2.7f` g, slop `500` ms, reset `3000` ms.
- No `Co-Authored-By` or AI hints in commit messages. Commit after every task.
- Version comparison is SERVER-side (`is_new_version` in response) — do not invent client-side compare logic.
- iOS targets are OUT of scope (M2). Do not add `iosMain` or ios targets.

---

### Task 1: Gradle scaffolding — version catalog + `updraft-core` module

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `updraft-core/build.gradle.kts`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/core/Placeholder.kt` (deleted in Task 2)

**Interfaces:**
- Produces: buildable `:updraft-core` KMP module (android target), catalog entries `libs.ktor.*`, `libs.turbine`, plugins `kotlin-multiplatform`, `compose-multiplatform`, `compose-compiler`.

- [ ] **Step 1: Add versions/libs/plugins to catalog**

In `gradle/libs.versions.toml` add under `[versions]`:

```toml
composeMultiplatform = "1.9.3"
ktor = "3.3.3"
coroutines = "1.10.2"
turbine = "1.2.1"
lifecycleProcess = "2.9.4"
```

Under `[libraries]`:

```toml
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
lifecycle-process = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycleProcess" }
```

Under `[plugins]`:

```toml
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Create `updraft-core/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.startup)
            implementation(libs.lifecycle.process)
            implementation(libs.core.ktx)
        }
    }
}

android {
    namespace = "com.appswithlove.updraft.core"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
```

- [ ] **Step 3: Register module + placeholder file**

In `settings.gradle.kts` add `include(":updraft-core")` next to existing includes.

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/core/Placeholder.kt`:

```kotlin
package com.appswithlove.updraft.core

internal const val PLACEHOLDER = true
```

- [ ] **Step 4: Verify build**

Run: `./gradlew :updraft-core:assemble`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts updraft-core/
git commit -m "Add updraft-core KMP module scaffolding"
```

---

### Task 2: Move API models + `FeedbackType` to core commonMain

**Files:**
- Move (`git mv`): all 8 files from `updraft-sdk/src/main/java/com/appswithlove/updraft/api/request/` and `.../api/response/` to `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/request/` and `.../response/`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/FeedbackType.kt`
- Delete: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/core/Placeholder.kt`
- Modify: `updraft-sdk/build.gradle.kts` (add `api(project(":updraft-core"))`)
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/FeedbackTypeTest.kt`

**Interfaces:**
- Produces: `@Serializable` request/response data classes (packages unchanged: `com.appswithlove.updraft.api.request` / `.response`); `enum class FeedbackType(val apiName: String) { Design("design"), Feedback("feedback"), Bug("bug") }`.
- Note: old `FeedbackChoice` stays in updraft-sdk until Task 12 deletes the Views UI.

- [ ] **Step 1: Write failing test**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/FeedbackTypeTest.kt`:

```kotlin
package com.appswithlove.updraft

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackTypeTest {
    @Test
    fun apiNames_matchServerContract() {
        assertEquals("design", FeedbackType.Design.apiName)
        assertEquals("feedback", FeedbackType.Feedback.apiName)
        assertEquals("bug", FeedbackType.Bug.apiName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.FeedbackTypeTest"`
Expected: FAIL — unresolved reference `FeedbackType`

- [ ] **Step 3: Move models, add FeedbackType**

```bash
mkdir -p updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api
git mv updraft-sdk/src/main/java/com/appswithlove/updraft/api/request updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/request
git mv updraft-sdk/src/main/java/com/appswithlove/updraft/api/response updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/response
rm updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/core/Placeholder.kt
```

The 8 model files are pure kotlinx.serialization data classes — no source edits needed.

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/FeedbackType.kt`:

```kotlin
package com.appswithlove.updraft

enum class FeedbackType(val apiName: String) {
    Design("design"),
    Feedback("feedback"),
    Bug("bug"),
}
```

In `updraft-sdk/build.gradle.kts` `dependencies { }` add first line:

```kotlin
api(project(":updraft-core"))
```

- [ ] **Step 4: Run tests + full build**

Run: `./gradlew :updraft-core:testDebugUnitTest :updraft-sdk:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL (updraft-sdk resolves models from core)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Move API models to updraft-core commonMain, add FeedbackType"
```

---

### Task 3: `UpdraftSettings` + `LogLevel` in commonMain

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/UpdraftSettings.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/UpdraftSettingsTest.kt`

**Interfaces:**
- Produces:

```kotlin
enum class LogLevel { None, Error, Debug }

class UpdraftSettings(
    val appKey: String,
    val sdkKey: String,
    val baseUrl: String = BASE_URL_PROD,
    val logLevel: LogLevel = LogLevel.Error,
    val showFeedbackAlert: Boolean = true,
    val feedbackEnabled: Boolean = true,
    val storeRelease: Boolean = false,
) {
    fun shouldShowErrors(): Boolean
    companion object { const val BASE_URL_PROD; const val BASE_URL_STAGING }
}
```

- Note: old `Settings` stays in updraft-sdk until Task 12.

- [ ] **Step 1: Write failing test**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/UpdraftSettingsTest.kt`:

```kotlin
package com.appswithlove.updraft

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdraftSettingsTest {
    private fun settings(logLevel: LogLevel) =
        UpdraftSettings(appKey = "a", sdkKey = "s", logLevel = logLevel)

    @Test
    fun shouldShowErrors_trueForErrorAndDebug() {
        assertTrue(settings(LogLevel.Error).shouldShowErrors())
        assertTrue(settings(LogLevel.Debug).shouldShowErrors())
        assertFalse(settings(LogLevel.None).shouldShowErrors())
    }

    @Test
    fun defaults_matchLegacySdk() {
        val s = UpdraftSettings(appKey = "a", sdkKey = "s")
        assertEquals("https://app.getupdraft.com/api/", s.baseUrl)
        assertEquals(LogLevel.Error, s.logLevel)
        assertTrue(s.feedbackEnabled)
        assertFalse(s.storeRelease)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.UpdraftSettingsTest"`
Expected: FAIL — unresolved reference `UpdraftSettings`

- [ ] **Step 3: Implement**

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/UpdraftSettings.kt`:

```kotlin
package com.appswithlove.updraft

enum class LogLevel { None, Error, Debug }

class UpdraftSettings(
    val appKey: String,
    val sdkKey: String,
    val baseUrl: String = BASE_URL_PROD,
    val logLevel: LogLevel = LogLevel.Error,
    val showFeedbackAlert: Boolean = true,
    val feedbackEnabled: Boolean = true,
    val storeRelease: Boolean = false,
) {
    fun shouldShowErrors(): Boolean =
        logLevel == LogLevel.Error || logLevel == LogLevel.Debug

    companion object {
        const val BASE_URL_PROD = "https://app.getupdraft.com/api/"
        const val BASE_URL_STAGING = "https://u2.mqd.me/api/"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.UpdraftSettingsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add updraft-core/
git commit -m "Add UpdraftSettings and LogLevel to commonMain"
```

---

### Task 4: `KeyValueStore` + `AppInfo` expect/actual

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/KeyValueStore.kt`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/AppInfo.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/KeyValueStore.android.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/AppInfo.android.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/UpdraftContext.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/UpdraftCoreInitializer.kt`
- Create: `updraft-core/src/androidMain/AndroidManifest.xml`

**Interfaces:**
- Produces:

```kotlin
// commonMain
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
expect fun createKeyValueStore(name: String): KeyValueStore

class AppInfo(
    val versionCode: Long,
    val versionName: String,
    val systemVersion: String,
    val deviceName: String,
    val deviceUuid: String,
)
expect fun currentAppInfo(): AppInfo
```

- Android context comes from `UpdraftContext.application`, set by androidx.startup initializer — no manual init call.

- [ ] **Step 1: Write commonMain declarations**

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/KeyValueStore.kt`:

```kotlin
package com.appswithlove.updraft.platform

interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

expect fun createKeyValueStore(name: String): KeyValueStore
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/AppInfo.kt`:

```kotlin
package com.appswithlove.updraft.platform

class AppInfo(
    val versionCode: Long,
    val versionName: String,
    val systemVersion: String,
    val deviceName: String,
    val deviceUuid: String,
)

expect fun currentAppInfo(): AppInfo
```

- [ ] **Step 2: Write android actuals + context holder + initializer**

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/UpdraftContext.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.app.Application

object UpdraftContext {
    lateinit var application: Application
        internal set
}
```

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/UpdraftCoreInitializer.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

class UpdraftCoreInitializer : Initializer<UpdraftContext> {
    override fun create(context: Context): UpdraftContext {
        UpdraftContext.application = context.applicationContext as Application
        return UpdraftContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
```

`updraft-core/src/androidMain/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge"
            xmlns:tools="http://schemas.android.com/tools">
            <meta-data
                android:name="com.appswithlove.updraft.platform.UpdraftCoreInitializer"
                android:value="androidx.startup" />
        </provider>
    </application>
</manifest>
```

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/KeyValueStore.android.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private class SharedPrefsKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    SharedPrefsKeyValueStore(UpdraftContext.application.getSharedPreferences(name, Context.MODE_PRIVATE))
```

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/AppInfo.android.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

@SuppressLint("HardwareIds")
actual fun currentAppInfo(): AppInfo {
    val context = UpdraftContext.application
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    val versionCode = when {
        packageInfo == null -> -1L
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageInfo.longVersionCode
        else -> @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    }
    return AppInfo(
        versionCode = versionCode,
        versionName = packageInfo?.versionName.orEmpty(),
        systemVersion = Build.VERSION.RELEASE.orEmpty(),
        deviceName = Build.MODEL.orEmpty(),
        deviceUuid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
    )
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew :updraft-core:assemble`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add updraft-core/
git commit -m "Add KeyValueStore and AppInfo expect/actual with startup initializer"
```

---

### Task 5: `UpdraftApi` — Ktor client in commonMain

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/UpdraftApi.kt`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/ApiException.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/api/UpdraftApiTest.kt`

**Interfaces:**
- Consumes: `UpdraftSettings`, `AppInfo`, request/response models (Task 2/3/4).
- Produces:

```kotlin
class ApiException(message: String) : Exception(message)

class UpdraftApi(
    private val settings: UpdraftSettings,
    private val appInfo: AppInfo,
    engine: io.ktor.client.engine.HttpClientEngine? = null, // null = platform default
) {
    suspend fun checkLastVersion(): CheckLastVersionResponse   // POST check_last_version/, sends versionCode as string
    suspend fun getLastVersion(): GetLastVersionResponse       // POST get_last_version/
    suspend fun isFeedbackEnabled(): Boolean                   // POST feedback-mobile-enabled/, throws ApiException on error_code
    fun sendFeedback(
        screenshotPng: ByteArray,
        type: FeedbackType,
        description: String,
        email: String,
    ): Flow<Double>                                            // multipart POST feedback-mobile/, emits upload progress 0..1, completes on success
}
```

- [ ] **Step 1: Write failing tests**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/api/UpdraftApiTest.kt`:

```kotlin
package com.appswithlove.updraft.api

import app.cash.turbine.test
import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.UpdraftSettings
import com.appswithlove.updraft.platform.AppInfo
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdraftApiTest {

    private val settings = UpdraftSettings(appKey = "APP", sdkKey = "SDK", baseUrl = "https://example.com/api/")
    private val appInfo = AppInfo(42L, "1.2.3", "16", "Pixel", "uuid-1")

    private fun jsonResponse(body: String) = headersOf(HttpHeaders.ContentType, "application/json") to body

    @Test
    fun checkLastVersion_sendsKeysAndVersionCode() = runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            requestBody = String(request.body.toByteArray())
            assertEquals("/api/check_last_version/", request.url.encodedPath)
            respond(
                """{"is_new_version":true,"is_autoupdate_enabled":true,"version":"9"}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val response = UpdraftApi(settings, appInfo, engine).checkLastVersion()
        val sent = Json.parseToJsonElement(requestBody).jsonObject
        assertEquals("APP", sent["app_key"]!!.jsonPrimitive.content)
        assertEquals("SDK", sent["sdk_key"]!!.jsonPrimitive.content)
        assertEquals("42", sent["version"]!!.jsonPrimitive.content)
        assertTrue(response.isNewVersion)
    }

    @Test
    fun isFeedbackEnabled_throwsApiExceptionOnErrorCodes() = runTest {
        val engine = MockEngine {
            respond(
                """{"is_feedback_enabled":false,"error_code":["x"],"error_description":["broken"]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val ex = assertFailsWith<ApiException> { UpdraftApi(settings, appInfo, engine).isFeedbackEnabled() }
        assertEquals("broken", ex.message)
    }

    @Test
    fun sendFeedback_uploadsMultipartAndCompletes() = runTest {
        var contentType = ""
        var bodyText = ""
        val engine = MockEngine { request ->
            contentType = request.body.contentType.toString()
            bodyText = String(request.body.toByteArray())
            assertEquals("/api/feedback-mobile/", request.url.encodedPath)
            respond("""{}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        UpdraftApi(settings, appInfo, engine)
            .sendFeedback(byteArrayOf(1, 2, 3), FeedbackType.Bug, "desc", "a@b.c")
            .test {
                awaitComplete()
            }
        assertTrue(contentType.startsWith("multipart/form-data"))
        assertTrue(bodyText.contains("name=app_key") || bodyText.contains("name=\"app_key\""))
        assertTrue(bodyText.contains("bug"))
        assertTrue(bodyText.contains("Pixel"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.api.UpdraftApiTest"`
Expected: FAIL — unresolved reference `UpdraftApi`

- [ ] **Step 3: Implement**

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/ApiException.kt`:

```kotlin
package com.appswithlove.updraft.api

class ApiException(message: String) : Exception(message)
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/UpdraftApi.kt`:

```kotlin
package com.appswithlove.updraft.api

import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.LogLevel
import com.appswithlove.updraft.UpdraftSettings
import com.appswithlove.updraft.api.request.CheckLastVersionRequest
import com.appswithlove.updraft.api.request.FeedbackEnabledRequest
import com.appswithlove.updraft.api.request.GetLastVersionRequest
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.FeedbackEnabledResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import com.appswithlove.updraft.platform.AppInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel as KtorLogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class UpdraftApi(
    private val settings: UpdraftSettings,
    private val appInfo: AppInfo,
    engine: HttpClientEngine? = null,
) {
    private val client: HttpClient = (engine?.let { HttpClient(it) } ?: HttpClient()).config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = if (settings.logLevel == LogLevel.Debug) KtorLogLevel.BODY else KtorLogLevel.NONE
        }
    }

    private fun url(path: String) = settings.baseUrl + path

    suspend fun checkLastVersion(): CheckLastVersionResponse {
        check(appInfo.versionCode >= 0) { "Version code is invalid" }
        val request = CheckLastVersionRequest(
            sdkKey = settings.sdkKey,
            appKey = settings.appKey,
            version = appInfo.versionCode.toString(),
        )
        return client.post(url("check_last_version/")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getLastVersion(): GetLastVersionResponse =
        client.post(url("get_last_version/")) {
            contentType(ContentType.Application.Json)
            setBody(GetLastVersionRequest(sdkKey = settings.sdkKey, appKey = settings.appKey))
        }.body()

    suspend fun isFeedbackEnabled(): Boolean {
        val response: FeedbackEnabledResponse = client.post(url("feedback-mobile-enabled/")) {
            contentType(ContentType.Application.Json)
            setBody(FeedbackEnabledRequest(sdkKey = settings.sdkKey, appKey = settings.appKey))
        }.body()
        if (response.errorCodes.isNotEmpty()) {
            throw ApiException(response.errorDescriptions.firstOrNull().orEmpty())
        }
        return response.isFeedbackEnabled
    }

    fun sendFeedback(
        screenshotPng: ByteArray,
        type: FeedbackType,
        description: String,
        email: String,
    ): Flow<Double> = callbackFlow {
        val form = formData {
            append(
                "image",
                screenshotPng,
                Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"UPDRAFT_SCREENSHOT.png\"")
                },
            )
            append("app_key", settings.appKey)
            append("sdk_key", settings.sdkKey)
            append("tag", type.apiName)
            append("description", description)
            append("email", email)
            append("build_version", appInfo.versionName)
            append("system_version", appInfo.systemVersion)
            append("device_name", appInfo.deviceName)
            append("device_uuid", appInfo.deviceUuid)
        }
        val uploadJob = launch {
            try {
                client.post(url("feedback-mobile/")) {
                    setBody(MultiPartFormDataContent(form))
                    onUpload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            trySend(bytesSentTotal.toDouble() / contentLength.toDouble())
                        }
                    }
                }
                close()
            } catch (t: Throwable) {
                close(t)
            }
        }
        awaitClose { uploadJob.cancel() }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.api.UpdraftApiTest"`
Expected: PASS (3 tests). If a Ktor API name differs on this Ktor version (e.g. `onUpload` signature), fix implementation — not the assertions on wire format.

- [ ] **Step 5: Commit**

```bash
git add updraft-core/
git commit -m "Add Ktor-based UpdraftApi with multipart feedback upload"
```

---

### Task 6: Interactors in commonMain

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckUpdateInteractor.kt`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckUpdateResultModel.kt`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackEnabledInteractor.kt`
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackResultModel.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/interactor/CheckUpdateInteractorTest.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackEnabledInteractorTest.kt`

**Interfaces:**
- Consumes: `UpdraftApi` (Task 5), `KeyValueStore` (Task 4).
- Produces:

```kotlin
data class CheckUpdateResultModel(
    val showAlert: Boolean,
    val url: String? = null,
    val version: String? = null,
    val yourVersion: String? = null,
    val createAt: String? = null,
)
class CheckUpdateInteractor(api: UpdraftApi) { suspend fun checkUpdate(): CheckUpdateResultModel }

class CheckFeedbackResultModel(val showAlert: Boolean, val alertType: AlertType, val isFeedbackEnabled: Boolean) {
    enum class AlertType { FeedbackDisabled, HowToGiveFeedback }
}
class CheckFeedbackEnabledInteractor(api: UpdraftApi, store: KeyValueStore) { suspend fun run(): CheckFeedbackResultModel }
```

- To make interactors testable without network, api calls go through an internal interface:

```kotlin
interface UpdraftApiContract {
    suspend fun checkLastVersion(): CheckLastVersionResponse
    suspend fun getLastVersion(): GetLastVersionResponse
    suspend fun isFeedbackEnabled(): Boolean
    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double>
}
```

Make `UpdraftApi` implement `UpdraftApiContract` (add `: UpdraftApiContract` and `override` markers in `UpdraftApi.kt`). Interactors take `UpdraftApiContract`.

- [ ] **Step 1: Write failing tests**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/interactor/CheckUpdateInteractorTest.kt`:

```kotlin
package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeApi(
    var checkResponse: CheckLastVersionResponse,
    var lastVersionResponse: GetLastVersionResponse = GetLastVersionResponse(updateUrl = null),
    var feedbackEnabled: Boolean = true,
) : UpdraftApiContract {
    override suspend fun checkLastVersion() = checkResponse
    override suspend fun getLastVersion() = lastVersionResponse
    override suspend fun isFeedbackEnabled() = feedbackEnabled
    override fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> = emptyFlow()
}

class CheckUpdateInteractorTest {

    @Test
    fun newVersionWithUrl_showsAlert() = runTest {
        val api = FakeApi(
            CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = true, version = "5", yourVersion = "4", createAt = "2026-01-01T00:00:00Z"),
            GetLastVersionResponse(updateUrl = "https://dl.example/app"),
        )
        val result = CheckUpdateInteractor(api).checkUpdate()
        assertTrue(result.showAlert)
        assertEquals("https://dl.example/app", result.url)
        assertEquals("5", result.version)
        assertEquals("4", result.yourVersion)
    }

    @Test
    fun newVersionWithoutUrl_noAlert() = runTest {
        val api = FakeApi(
            CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = true),
            GetLastVersionResponse(updateUrl = null),
        )
        assertFalse(CheckUpdateInteractor(api).checkUpdate().showAlert)
    }

    @Test
    fun autoupdateDisabled_noAlert() = runTest {
        val api = FakeApi(CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = false))
        assertFalse(CheckUpdateInteractor(api).checkUpdate().showAlert)
    }
}
```

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackEnabledInteractorTest.kt`:

```kotlin
package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import com.appswithlove.updraft.platform.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeStore : KeyValueStore {
    val map = mutableMapOf<String, Boolean>()
    override fun getBoolean(key: String, default: Boolean) = map[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

private class FakeApi(var feedbackEnabled: Boolean) : UpdraftApiContract {
    override suspend fun checkLastVersion() = CheckLastVersionResponse(isNewVersion = false, isAutoupdateEnabled = false)
    override suspend fun getLastVersion() = GetLastVersionResponse()
    override suspend fun isFeedbackEnabled() = feedbackEnabled
    override fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> = emptyFlow()
}

class CheckFeedbackEnabledInteractorTest {

    @Test
    fun becameEnabled_showsHowToAlert_andPersists() = runTest {
        val store = FakeStore()
        val result = CheckFeedbackEnabledInteractor(FakeApi(true), store).run()
        assertTrue(result.showAlert)
        assertEquals(CheckFeedbackResultModel.AlertType.HowToGiveFeedback, result.alertType)
        assertTrue(store.map["is_feedback_enabled_property"]!!)
    }

    @Test
    fun becameDisabled_showsDisabledAlert() = runTest {
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        val result = CheckFeedbackEnabledInteractor(FakeApi(false), store).run()
        assertTrue(result.showAlert)
        assertEquals(CheckFeedbackResultModel.AlertType.FeedbackDisabled, result.alertType)
        assertFalse(result.isFeedbackEnabled)
    }

    @Test
    fun unchanged_noAlert() = runTest {
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        assertFalse(CheckFeedbackEnabledInteractor(FakeApi(true), store).run().showAlert)
        assertFalse(CheckFeedbackEnabledInteractor(FakeApi(false), FakeStore()).run().showAlert)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.interactor.*"`
Expected: FAIL — unresolved references

- [ ] **Step 3: Implement**

Add to `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/api/UpdraftApi.kt` (top of file, same package):

```kotlin
interface UpdraftApiContract {
    suspend fun checkLastVersion(): CheckLastVersionResponse
    suspend fun getLastVersion(): GetLastVersionResponse
    suspend fun isFeedbackEnabled(): Boolean
    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double>
}
```

and change `class UpdraftApi(...)` to `class UpdraftApi(...) : UpdraftApiContract`, adding `override` to the four members.

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckUpdateResultModel.kt`:

```kotlin
package com.appswithlove.updraft.interactor

data class CheckUpdateResultModel(
    val showAlert: Boolean,
    val url: String? = null,
    val version: String? = null,
    val yourVersion: String? = null,
    val createAt: String? = null,
)
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckUpdateInteractor.kt`:

```kotlin
package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.UpdraftApiContract

class CheckUpdateInteractor(private val api: UpdraftApiContract) {

    suspend fun checkUpdate(): CheckUpdateResultModel {
        val check = api.checkLastVersion()
        if (!check.isNewVersion || !check.isAutoupdateEnabled) {
            return CheckUpdateResultModel(showAlert = false)
        }
        val url = api.getLastVersion().updateUrl
        return CheckUpdateResultModel(
            showAlert = url != null,
            url = url,
            version = check.version,
            yourVersion = check.yourVersion,
            createAt = check.createAt,
        )
    }
}
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackResultModel.kt`:

```kotlin
package com.appswithlove.updraft.interactor

class CheckFeedbackResultModel(
    val showAlert: Boolean,
    val alertType: AlertType,
    val isFeedbackEnabled: Boolean,
) {
    enum class AlertType { FeedbackDisabled, HowToGiveFeedback }
}
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/interactor/CheckFeedbackEnabledInteractor.kt`:

```kotlin
package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.platform.KeyValueStore

class CheckFeedbackEnabledInteractor(
    private val api: UpdraftApiContract,
    private val store: KeyValueStore,
) {
    suspend fun run(): CheckFeedbackResultModel {
        val isEnabled = api.isFeedbackEnabled()
        val previouslyEnabled = store.getBoolean(IS_FEEDBACK_ENABLED_PROPERTY, false)

        val showAlert = isEnabled != previouslyEnabled
        val alertType = if (!isEnabled && previouslyEnabled) {
            CheckFeedbackResultModel.AlertType.FeedbackDisabled
        } else {
            CheckFeedbackResultModel.AlertType.HowToGiveFeedback
        }
        store.putBoolean(IS_FEEDBACK_ENABLED_PROPERTY, isEnabled)
        return CheckFeedbackResultModel(showAlert, alertType, isEnabled)
    }

    companion object {
        const val STORE_NAME = "feedback_enabled_storage"
        private const val IS_FEEDBACK_ENABLED_PROPERTY = "is_feedback_enabled_property"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.interactor.*"`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add updraft-core/
git commit -m "Port interactors to commonMain against UpdraftApiContract"
```

---

### Task 7: Platform seams — `ShakeDetector`, `ScreenshotGrabber`, `UrlOpener`, `AppForegroundObserver`

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/Platform.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/Platform.android.kt`
- Create: `updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/CurrentActivityManager.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/platform/ShakeMathTest.kt`

**Interfaces:**
- Produces (commonMain):

```kotlin
interface ShakeDetector { fun start(); fun stop(); fun setEnabled(enabled: Boolean) }
expect fun createShakeDetector(onShake: () -> Unit): ShakeDetector

interface ScreenshotGrabber { fun capturePng(): ByteArray? }
expect fun createScreenshotGrabber(): ScreenshotGrabber

expect fun openUrl(url: String)

interface AppForegroundObserver { fun start() }
expect fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver

// pure shake math, testable in common
fun isShakeGForce(x: Float, y: Float, z: Float, gravity: Float): Boolean
```

- Android actuals reuse existing `ShakeDetectorManager` sensor logic (constants from Global Constraints), `DefaultScreenshotProvider` draw logic, `Intent.ACTION_VIEW` for url, `ProcessLifecycleOwner` for foreground.
- `CurrentActivityManager` (androidMain, note fixed spelling) is a copy of existing `CurrentActivityManger` registered by `UpdraftCoreInitializer` (extend Task 4 initializer: `UpdraftContext.application.registerActivityLifecycleCallbacks(CurrentActivityManager)`), exposing `val current: Activity?` as singleton `object`.

- [ ] **Step 1: Write failing test for shake math**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/platform/ShakeMathTest.kt`:

```kotlin
package com.appswithlove.updraft.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeMathTest {
    private val g = 9.80665f

    @Test
    fun restingPhone_noShake() {
        assertFalse(isShakeGForce(0f, 0f, g, g))
    }

    @Test
    fun hardShake_detected() {
        assertTrue(isShakeGForce(3f * g, 0f, 0f, g))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.platform.ShakeMathTest"`
Expected: FAIL — unresolved reference `isShakeGForce`

- [ ] **Step 3: Implement commonMain declarations**

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/platform/Platform.kt`:

```kotlin
package com.appswithlove.updraft.platform

import kotlin.math.sqrt

interface ShakeDetector {
    fun start()
    fun stop()
    fun setEnabled(enabled: Boolean)
}

expect fun createShakeDetector(onShake: () -> Unit): ShakeDetector

interface ScreenshotGrabber {
    fun capturePng(): ByteArray?
}

expect fun createScreenshotGrabber(): ScreenshotGrabber

expect fun openUrl(url: String)

interface AppForegroundObserver {
    fun start()
}

expect fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver

const val SHAKE_THRESHOLD_GRAVITY = 2.7f

fun isShakeGForce(x: Float, y: Float, z: Float, gravity: Float): Boolean {
    val gX = x / gravity
    val gY = y / gravity
    val gZ = z / gravity
    return sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()) > SHAKE_THRESHOLD_GRAVITY
}
```

- [ ] **Step 4: Run shake math test to verify it passes**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.platform.ShakeMathTest"`
Expected: PASS

- [ ] **Step 5: Implement android actuals**

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/CurrentActivityManager.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle

object CurrentActivityManager : Application.ActivityLifecycleCallbacks {

    var current: Activity? = null
        private set

    private val listeners = mutableSetOf<(Activity?) -> Unit>()

    fun addListener(listener: (Activity?) -> Unit) {
        listeners.add(listener)
        listener(current)
    }

    override fun onActivityResumed(activity: Activity) {
        current = activity
        listeners.forEach { it(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        current = null
        listeners.forEach { it(null) }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
```

In `UpdraftCoreInitializer.create` add before `return`:

```kotlin
UpdraftContext.application.registerActivityLifecycleCallbacks(CurrentActivityManager)
```

`updraft-core/src/androidMain/kotlin/com/appswithlove/updraft/platform/Platform.android.kt`:

```kotlin
package com.appswithlove.updraft.platform

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.io.ByteArrayOutputStream

private class AndroidShakeDetector(private val onShake: () -> Unit) :
    ShakeDetector, SensorEventListener, DefaultLifecycleObserver {

    private val sensorManager =
        UpdraftContext.application.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeTimestamp = 0L
    private var enabled = true

    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun onStart(owner: LifecycleOwner) {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onStop(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (!isShakeGForce(x, y, z, SensorManager.GRAVITY_EARTH)) return

        val now = System.currentTimeMillis()
        if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) return
        shakeTimestamp = now

        if (enabled) {
            enabled = false
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_SLOP_TIME_MS = 500
    }
}

actual fun createShakeDetector(onShake: () -> Unit): ShakeDetector = AndroidShakeDetector(onShake)

private class AndroidScreenshotGrabber : ScreenshotGrabber {
    override fun capturePng(): ByteArray? {
        val activity = CurrentActivityManager.current ?: return null
        val view = activity.window.decorView.rootView
        if (view.width == 0 || view.height == 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }
}

actual fun createScreenshotGrabber(): ScreenshotGrabber = AndroidScreenshotGrabber()

actual fun openUrl(url: String) {
    val activity = CurrentActivityManager.current ?: return
    activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

private class AndroidForegroundObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
) : AppForegroundObserver, DefaultLifecycleObserver {
    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) = onForeground()
    override fun onStop(owner: LifecycleOwner) = onBackground()
}

actual fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver =
    AndroidForegroundObserver(onForeground, onBackground)
```

- [ ] **Step 6: Verify build + all core tests**

Run: `./gradlew :updraft-core:testDebugUnitTest :updraft-core:assemble`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 7: Commit**

```bash
git add updraft-core/
git commit -m "Add platform seams: shake, screenshot, url, foreground observer"
```

---

### Task 8: `Updraft` entry object + events in commonMain

**Files:**
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/Updraft.kt` (core; legacy class in updraft-sdk is removed in Task 12)
- Create: `updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/UpdraftEvent.kt`
- Test: `updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/UpdraftControllerTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 3–7.
- Produces:

```kotlin
sealed interface UpdraftEvent {
    data class UpdateAvailable(val url: String, val version: String?, val yourVersion: String?, val createAt: String?) : UpdraftEvent
    data object ShowFeedbackHint : UpdraftEvent      // "how to give feedback" alert
    data object FeedbackDisabled : UpdraftEvent      // feedback got disabled alert
    data object FeedbackRequested : UpdraftEvent     // shake or showFeedback(); payload fetched via takePendingScreenshot()
    data object CloseFeedback : UpdraftEvent         // feedback disabled while feedback UI open
    data class Error(val cause: Throwable) : UpdraftEvent
}

fun interface FeedbackUiPresenter { fun presentFeedback(screenshotPng: ByteArray?) }

object Updraft {
    fun start(settings: UpdraftSettings)
    fun checkForUpdate()                       // fire-and-forget, emits UpdateAvailable/Error
    fun showFeedback()                         // manual trigger, same path as shake
    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double>
    fun openUpdateUrl(url: String)
    fun setFeedbackUiPresenter(presenter: FeedbackUiPresenter?)
    fun onFeedbackUiClosed()                   // re-arms shake detector
    val events: SharedFlow<UpdraftEvent>
    val settings: UpdraftSettings              // throws if not started
}

// internal, constructor-injected for tests:
internal class UpdraftController(
    settings: UpdraftSettings,
    api: UpdraftApiContract,
    store: KeyValueStore,
    scope: CoroutineScope,
) { ... }
```

- `Updraft` object is a thin holder delegating to an internal `UpdraftController` so logic is testable without platform actuals. `start()` builds the controller with real dependencies plus shake detector + foreground observer; on foreground it runs update check + feedback-enabled check (mirrors old `AppUpdateManager.onStart` + `CheckFeedbackEnabledManager.onStart`); on shake it captures screenshot and either calls the presenter or emits `FeedbackRequested`.

- [ ] **Step 1: Write failing controller test**

`updraft-core/src/commonTest/kotlin/com/appswithlove/updraft/UpdraftControllerTest.kt`:

```kotlin
package com.appswithlove.updraft

import app.cash.turbine.test
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import com.appswithlove.updraft.platform.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeStore : KeyValueStore {
    val map = mutableMapOf<String, Boolean>()
    override fun getBoolean(key: String, default: Boolean) = map[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

private class FakeApi : UpdraftApiContract {
    var check = CheckLastVersionResponse(isNewVersion = false, isAutoupdateEnabled = false)
    var last = GetLastVersionResponse()
    var feedbackEnabled = true
    override suspend fun checkLastVersion() = check
    override suspend fun getLastVersion() = last
    override suspend fun isFeedbackEnabled() = feedbackEnabled
    override fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> = emptyFlow()
}

class UpdraftControllerTest {

    private val settings = UpdraftSettings(appKey = "a", sdkKey = "s", showFeedbackAlert = true)

    @Test
    fun onForeground_updateAvailable_emitsEvent() = runTest {
        val api = FakeApi().apply {
            check = CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = true, version = "7")
            last = GetLastVersionResponse(updateUrl = "https://u")
            feedbackEnabled = true
        }
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        val controller = UpdraftController(settings, api, store, backgroundScope)

        controller.events.test {
            controller.onForeground()
            val event = awaitItem()
            val update = assertIs<UpdraftEvent.UpdateAvailable>(event)
            assertEquals("https://u", update.url)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onForeground_feedbackBecameDisabled_emitsFeedbackDisabled() = runTest {
        val api = FakeApi().apply { feedbackEnabled = false }
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        val controller = UpdraftController(settings, api, store, backgroundScope)

        controller.events.test {
            controller.onForeground()
            assertIs<UpdraftEvent.FeedbackDisabled>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun feedbackRequested_emittedOnShakePath() = runTest {
        val controller = UpdraftController(settings, FakeApi(), FakeStore(), backgroundScope)
        controller.events.test {
            controller.onFeedbackTriggered(screenshotPng = byteArrayOf(1))
            assertIs<UpdraftEvent.FeedbackRequested>(awaitItem())
            assertEquals(1, controller.takePendingScreenshot()!!.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :updraft-core:testDebugUnitTest --tests "com.appswithlove.updraft.UpdraftControllerTest"`
Expected: FAIL — unresolved reference `UpdraftController`

- [ ] **Step 3: Implement**

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/UpdraftEvent.kt`:

```kotlin
package com.appswithlove.updraft

sealed interface UpdraftEvent {
    data class UpdateAvailable(
        val url: String,
        val version: String?,
        val yourVersion: String?,
        val createAt: String?,
    ) : UpdraftEvent

    data object ShowFeedbackHint : UpdraftEvent
    data object FeedbackDisabled : UpdraftEvent
    data object FeedbackRequested : UpdraftEvent
    data object CloseFeedback : UpdraftEvent
    data class Error(val cause: Throwable) : UpdraftEvent
}

fun interface FeedbackUiPresenter {
    fun presentFeedback(screenshotPng: ByteArray?)
}
```

`updraft-core/src/commonMain/kotlin/com/appswithlove/updraft/Updraft.kt`:

```kotlin
package com.appswithlove.updraft

import com.appswithlove.updraft.api.UpdraftApi
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.interactor.CheckFeedbackEnabledInteractor
import com.appswithlove.updraft.interactor.CheckFeedbackResultModel
import com.appswithlove.updraft.interactor.CheckUpdateInteractor
import com.appswithlove.updraft.platform.KeyValueStore
import com.appswithlove.updraft.platform.createAppForegroundObserver
import com.appswithlove.updraft.platform.createKeyValueStore
import com.appswithlove.updraft.platform.createScreenshotGrabber
import com.appswithlove.updraft.platform.createShakeDetector
import com.appswithlove.updraft.platform.currentAppInfo
import com.appswithlove.updraft.platform.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class UpdraftController(
    private val settings: UpdraftSettings,
    private val api: UpdraftApiContract,
    store: KeyValueStore,
    private val scope: CoroutineScope,
) {
    private val checkUpdateInteractor = CheckUpdateInteractor(api)
    private val checkFeedbackInteractor = CheckFeedbackEnabledInteractor(api, store)

    private val _events = MutableSharedFlow<UpdraftEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UpdraftEvent> = _events

    private var updateAlertShown = false
    private var feedbackHintShown = false
    private var pendingScreenshot: ByteArray? = null

    var feedbackUiPresenter: FeedbackUiPresenter? = null

    fun onForeground() {
        if (settings.showFeedbackAlert && settings.feedbackEnabled && !feedbackHintShown) {
            feedbackHintShown = true
            _events.tryEmit(UpdraftEvent.ShowFeedbackHint)
        }
        checkForUpdate()
        checkFeedbackEnabled()
    }

    fun checkForUpdate() {
        scope.launch {
            try {
                val result = checkUpdateInteractor.checkUpdate()
                val url = result.url
                if (result.showAlert && url != null && !updateAlertShown) {
                    updateAlertShown = true
                    _events.tryEmit(
                        UpdraftEvent.UpdateAvailable(url, result.version, result.yourVersion, result.createAt),
                    )
                }
            } catch (t: Throwable) {
                _events.tryEmit(UpdraftEvent.Error(t))
            }
        }
    }

    private fun checkFeedbackEnabled() {
        scope.launch {
            try {
                val result = checkFeedbackInteractor.run()
                if (!result.isFeedbackEnabled) {
                    _events.tryEmit(UpdraftEvent.CloseFeedback)
                }
                if (result.showAlert) {
                    when (result.alertType) {
                        CheckFeedbackResultModel.AlertType.FeedbackDisabled ->
                            _events.tryEmit(UpdraftEvent.FeedbackDisabled)
                        CheckFeedbackResultModel.AlertType.HowToGiveFeedback ->
                            if (settings.showFeedbackAlert) _events.tryEmit(UpdraftEvent.ShowFeedbackHint)
                    }
                }
            } catch (t: Throwable) {
                _events.tryEmit(UpdraftEvent.Error(t))
            }
        }
    }

    fun onFeedbackTriggered(screenshotPng: ByteArray?) {
        pendingScreenshot = screenshotPng
        val presenter = feedbackUiPresenter
        if (presenter != null) {
            presenter.presentFeedback(screenshotPng)
        } else {
            _events.tryEmit(UpdraftEvent.FeedbackRequested)
        }
    }

    fun takePendingScreenshot(): ByteArray? {
        val screenshot = pendingScreenshot
        pendingScreenshot = null
        return screenshot
    }

    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> =
        api.sendFeedback(screenshotPng, type, description, email)
}

object Updraft {
    private var controller: UpdraftController? = null
    private var currentSettings: UpdraftSettings? = null
    private var shakeDetector: com.appswithlove.updraft.platform.ShakeDetector? = null

    val settings: UpdraftSettings
        get() = checkNotNull(currentSettings) { "Must call Updraft.start() first" }

    val events: SharedFlow<UpdraftEvent>
        get() = requireController().events

    fun start(settings: UpdraftSettings) {
        if (controller != null) return
        currentSettings = settings
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val api = UpdraftApi(settings, currentAppInfo())
        val store = createKeyValueStore(CheckFeedbackEnabledInteractor.STORE_NAME)
        val newController = UpdraftController(settings, api, store, scope)
        controller = newController

        if (settings.feedbackEnabled) {
            val detector = createShakeDetector { showFeedback() }
            shakeDetector = detector
            detector.start()
        }
        createAppForegroundObserver(
            onForeground = { newController.onForeground() },
            onBackground = { },
        ).start()
    }

    fun checkForUpdate() = requireController().checkForUpdate()

    fun showFeedback() {
        val screenshot = createScreenshotGrabber().capturePng()
        requireController().onFeedbackTriggered(screenshot)
    }

    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> =
        requireController().sendFeedback(screenshotPng, type, description, email)

    fun openUpdateUrl(url: String) = openUrl(url)

    fun setFeedbackUiPresenter(presenter: FeedbackUiPresenter?) {
        requireController().feedbackUiPresenter = presenter
    }

    fun onFeedbackUiClosed() {
        shakeDetector?.setEnabled(true)
    }

    internal fun takePendingScreenshot(): ByteArray? = requireController().takePendingScreenshot()

    private fun requireController(): UpdraftController =
        checkNotNull(controller) { "Must call Updraft.start() first" }
}
```

Note: `Updraft.kt` in updraft-sdk still exists with the same FQN — that causes a duplicate-class conflict ONLY when both are on the classpath. To avoid it now, this task also renames the legacy class: in `updraft-sdk`, rename `Updraft` to `LegacyUpdraft` (IDE-style rename: update `Updraft.getInstance()` call sites in `UpdraftSdkUi.kt`, `FeedbackActivity.kt`, `FeedbackFormPresenter.kt`, `app/src/main/java/com/appswithlove/updraftsdk/App.kt` — grep `Updraft.initialize|getInstance` to catch all). Legacy code is deleted entirely in Task 12.

- [ ] **Step 4: Run tests**

Run: `./gradlew :updraft-core:testDebugUnitTest :updraft-sdk:assembleDebug`
Expected: PASS + BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add Updraft entry object with event flow and controller"
```

---

### Task 9: `updraft-ui-compose` module + string resources

**Files:**
- Create: `updraft-ui-compose/build.gradle.kts`
- Modify: `settings.gradle.kts` (add `include(":updraft-ui-compose")`)
- Create: `updraft-ui-compose/src/commonMain/composeResources/values/strings.xml`
- Create: `updraft-ui-compose/src/commonMain/composeResources/values-de/strings.xml`

**Interfaces:**
- Produces: buildable CMP module `:updraft-ui-compose` depending on `:updraft-core`; compose-resources strings accessible as `Res.string.updraft_*`.

- [ ] **Step 1: Create module build file**

`updraft-ui-compose/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":updraft-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.appswithlove.updraft.ui.resources"
    publicResClass = false
}

android {
    namespace = "com.appswithlove.updraft.ui"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
```

Add `include(":updraft-ui-compose")` to `settings.gradle.kts`.

- [ ] **Step 2: Migrate strings**

Copy every `updraft_*` string from `updraft-sdk/src/main/res/values/strings.xml` and `values-de/strings.xml` into the two new `composeResources/values*/strings.xml` files verbatim (same names, same `en`/`de` content). Plurals (`updraft_relative_weeksAgo`, `updraft_relative_monthsAgo`) move as `<plurals>` too — compose-resources supports plurals via `pluralStringResource`.

Source files stay in updraft-sdk until Task 12 (Views UI still uses them).

- [ ] **Step 3: Verify build**

Run: `./gradlew :updraft-ui-compose:assemble`
Expected: BUILD SUCCESSFUL, generated `Res` class contains `updraft_feedbackDialog_title` etc.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts updraft-ui-compose/
git commit -m "Add updraft-ui-compose CMP module with migrated strings"
```

---

### Task 10: `DrawingController` + `DrawingCanvas` composable (FreeDraw port)

**Files:**
- Create: `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/drawing/DrawingController.kt`
- Create: `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/drawing/DrawingCanvas.kt`
- Test: `updraft-ui-compose/src/commonTest/kotlin/com/appswithlove/updraft/ui/drawing/DrawingControllerTest.kt`
- Modify: `updraft-ui-compose/build.gradle.kts` (add `commonTest.dependencies { implementation(kotlin("test")) }`)

**Interfaces:**
- Produces:

```kotlin
class DrawnPath(val points: List<Offset>, val color: Color, val strokeWidthPx: Float)

class DrawingController(initialColor: Color = Color.Red, initialStrokeWidthPx: Float = 12f) {
    val paths: List<DrawnPath>          // committed paths (snapshot-state backed)
    var color: Color
    var strokeWidthPx: Float
    val canUndo: Boolean
    val canRedo: Boolean
    fun startPath(point: Offset)
    fun addPoint(point: Offset)
    fun endPath()
    fun undo()
    fun redo()
}

@Composable
fun DrawingCanvas(controller: DrawingController, modifier: Modifier = Modifier)
```

- Mirrors FreeDrawView semantics: new stroke clears redo stack; undo moves last path to redo stack.

- [ ] **Step 1: Write failing tests**

`updraft-ui-compose/src/commonTest/kotlin/com/appswithlove/updraft/ui/drawing/DrawingControllerTest.kt`:

```kotlin
package com.appswithlove.updraft.ui.drawing

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawingControllerTest {

    private fun DrawingController.drawStroke(vararg points: Offset) {
        startPath(points.first())
        points.drop(1).forEach { addPoint(it) }
        endPath()
    }

    @Test
    fun stroke_commitsPath() {
        val c = DrawingController()
        c.drawStroke(Offset(0f, 0f), Offset(10f, 10f))
        assertEquals(1, c.paths.size)
        assertEquals(2, c.paths.first().points.size)
    }

    @Test
    fun undoRedo_roundTrips() {
        val c = DrawingController()
        c.drawStroke(Offset(0f, 0f), Offset(1f, 1f))
        assertTrue(c.canUndo)
        c.undo()
        assertEquals(0, c.paths.size)
        assertTrue(c.canRedo)
        c.redo()
        assertEquals(1, c.paths.size)
    }

    @Test
    fun newStroke_clearsRedoStack() {
        val c = DrawingController()
        c.drawStroke(Offset(0f, 0f), Offset(1f, 1f))
        c.undo()
        c.drawStroke(Offset(2f, 2f), Offset(3f, 3f))
        assertFalse(c.canRedo)
        assertEquals(1, c.paths.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :updraft-ui-compose:testDebugUnitTest --tests "com.appswithlove.updraft.ui.drawing.DrawingControllerTest"`
Expected: FAIL — unresolved reference `DrawingController`

- [ ] **Step 3: Implement**

`updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/drawing/DrawingController.kt`:

```kotlin
package com.appswithlove.updraft.ui.drawing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidthPx: Float,
)

class DrawingController(
    initialColor: Color = Color.Red,
    initialStrokeWidthPx: Float = 12f,
) {
    private val committedPaths = mutableStateListOf<DrawnPath>()
    private val redoStack = mutableStateListOf<DrawnPath>()
    private val currentPoints = mutableStateListOf<Offset>()

    val paths: List<DrawnPath> get() = committedPaths
    val currentStroke: List<Offset> get() = currentPoints

    var color by mutableStateOf(initialColor)
    var strokeWidthPx by mutableStateOf(initialStrokeWidthPx)

    val canUndo: Boolean get() = committedPaths.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun startPath(point: Offset) {
        currentPoints.clear()
        currentPoints.add(point)
    }

    fun addPoint(point: Offset) {
        currentPoints.add(point)
    }

    fun endPath() {
        if (currentPoints.isNotEmpty()) {
            committedPaths.add(DrawnPath(currentPoints.toList(), color, strokeWidthPx))
            redoStack.clear()
            currentPoints.clear()
        }
    }

    fun undo() {
        if (committedPaths.isNotEmpty()) {
            redoStack.add(committedPaths.removeAt(committedPaths.lastIndex))
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            committedPaths.add(redoStack.removeAt(redoStack.lastIndex))
        }
    }
}
```

`updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/drawing/DrawingCanvas.kt`:

```kotlin
package com.appswithlove.updraft.ui.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DrawingCanvas(controller: DrawingController, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.pointerInput(controller) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                    controller.startPath(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            controller.endPath()
                            break
                        }
                        controller.addPoint(change.position)
                        change.consume()
                    }
                }
            }
        },
    ) {
        controller.paths.forEach { drawStroke(it.points, it.color, it.strokeWidthPx) }
        drawStroke(controller.currentStroke, controller.color, controller.strokeWidthPx)
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, color: Color, strokeWidthPx: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = strokeWidthPx / 2f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
```

Add to `updraft-ui-compose/build.gradle.kts` sourceSets:

```kotlin
commonTest.dependencies {
    implementation(kotlin("test"))
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :updraft-ui-compose:testDebugUnitTest --tests "com.appswithlove.updraft.ui.drawing.DrawingControllerTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add updraft-ui-compose/
git commit -m "Add DrawingController and DrawingCanvas (FreeDrawView port)"
```

---

### Task 11: Feedback screen + dialogs in CMP

**Files:**
- Create: `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreenState.kt`
- Create: `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreen.kt`
- Create: `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/dialogs/UpdraftDialogs.kt`
- Test: `updraft-ui-compose/src/commonTest/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreenStateTest.kt`

**Interfaces:**
- Consumes: `Updraft.sendFeedback` (Task 8), `DrawingCanvas`/`DrawingController` (Task 10), `Res.string.updraft_*` (Task 9), `FeedbackType` (Task 2).
- Produces:

```kotlin
class FeedbackScreenState(
    private val send: (screenshot: ByteArray, type: FeedbackType, description: String, email: String) -> Flow<Double>,
    private val scope: CoroutineScope,
) {
    var selectedType: FeedbackType?
    var description: String
    var email: String
    val uploadProgress: Double?       // null = idle
    val result: Result<Unit>?        // null until finished
    val canSend: Boolean             // type selected && not uploading
    fun sendFeedback(screenshotPng: ByteArray)
}

@Composable
fun FeedbackScreen(
    screenshotPng: ByteArray?,
    onClose: () -> Unit,
    state: FeedbackScreenState = remember { ... default wired to Updraft ... },
)  // two-step: annotate screenshot (DrawingCanvas overlay) -> form -> send

@Composable fun UpdateAvailableDialog(event: UpdraftEvent.UpdateAvailable, onOpen: (String) -> Unit, onLater: () -> Unit)
@Composable fun FeedbackHintDialog(onDismiss: () -> Unit)
@Composable fun FeedbackDisabledDialog(onDismiss: () -> Unit)
@Composable fun UpdraftEventHost(events: Flow<UpdraftEvent>, onFeedbackRequested: () -> Unit)
    // collects events, renders the three dialogs; calls onFeedbackRequested for UpdraftEvent.FeedbackRequested
```

- Screenshot rendering: decode `ByteArray` PNG to `ImageBitmap` via expect/actual `fun decodePng(bytes: ByteArray): ImageBitmap` in `updraft-ui-compose` (androidMain: `BitmapFactory.decodeByteArray(...).asImageBitmap()`); merged annotated bitmap produced by drawing paths over the image in a `Canvas`-backed `ImageBitmap` via expect/actual `fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>): ByteArray` (androidMain: mutable Bitmap + android.graphics.Canvas + Paint per DrawnPath).
- `UpdateAvailableDialog` message: reuse simplified relative-age copy — show `updraft_updateAvailable_yourVersion` line when `yourVersion` present, plain description otherwise (drop `DateUtils` relative-age formatting; note in KDoc that relative age returns in a follow-up).

- [ ] **Step 1: Write failing state test**

`updraft-ui-compose/src/commonTest/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreenStateTest.kt`:

```kotlin
package com.appswithlove.updraft.ui.feedback

import com.appswithlove.updraft.FeedbackType
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedbackScreenStateTest {

    @Test
    fun canSend_requiresTypeSelection() = runTest {
        val state = FeedbackScreenState(send = { _, _, _, _ -> flowOf() }, scope = backgroundScope)
        assertFalse(state.canSend)
        state.selectedType = FeedbackType.Bug
        assertTrue(state.canSend)
    }

    @Test
    fun sendFeedback_success_setsResult() = runTest {
        val state = FeedbackScreenState(send = { _, _, _, _ -> flowOf(0.5, 1.0) }, scope = backgroundScope)
        state.selectedType = FeedbackType.Feedback
        state.sendFeedback(byteArrayOf(1))
        advanceUntilIdle()
        assertNotNull(state.result)
        assertTrue(state.result!!.isSuccess)
    }

    @Test
    fun sendFeedback_failure_setsFailureResult() = runTest {
        val state = FeedbackScreenState(
            send = { _, _, _, _ -> flow { throw IllegalStateException("boom") } },
            scope = backgroundScope,
        )
        state.selectedType = FeedbackType.Bug
        state.sendFeedback(byteArrayOf(1))
        advanceUntilIdle()
        assertTrue(state.result!!.isFailure)
        assertEquals("boom", state.result!!.exceptionOrNull()!!.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :updraft-ui-compose:testDebugUnitTest --tests "com.appswithlove.updraft.ui.feedback.FeedbackScreenStateTest"`
Expected: FAIL — unresolved reference `FeedbackScreenState`

- [ ] **Step 3: Implement state holder**

`updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreenState.kt`:

```kotlin
package com.appswithlove.updraft.ui.feedback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appswithlove.updraft.FeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class FeedbackScreenState(
    private val send: (screenshot: ByteArray, type: FeedbackType, description: String, email: String) -> Flow<Double>,
    private val scope: CoroutineScope,
) {
    var selectedType: FeedbackType? by mutableStateOf(null)
    var description: String by mutableStateOf("")
    var email: String by mutableStateOf("")
    var uploadProgress: Double? by mutableStateOf(null)
        private set
    var result: Result<Unit>? by mutableStateOf(null)
        private set

    val canSend: Boolean get() = selectedType != null && uploadProgress == null

    fun sendFeedback(screenshotPng: ByteArray) {
        val type = selectedType ?: return
        uploadProgress = 0.0
        scope.launch {
            send(screenshotPng, type, description, email)
                .catch { t ->
                    uploadProgress = null
                    result = Result.failure(t)
                }
                .onCompletion { cause ->
                    if (cause == null && result == null) {
                        uploadProgress = null
                        result = Result.success(Unit)
                    }
                }
                .collect { progress -> uploadProgress = progress }
        }
    }
}
```

- [ ] **Step 4: Run state tests to verify they pass**

Run: `./gradlew :updraft-ui-compose:testDebugUnitTest --tests "com.appswithlove.updraft.ui.feedback.FeedbackScreenStateTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Implement composables**

`updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/dialogs/UpdraftDialogs.kt`:

```kotlin
package com.appswithlove.updraft.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.ui.resources.Res
import com.appswithlove.updraft.ui.resources.updraft_button_cancel
import com.appswithlove.updraft.ui.resources.updraft_button_ok
import com.appswithlove.updraft.ui.resources.updraft_feedbackDialog_description
import com.appswithlove.updraft.ui.resources.updraft_feedbackDialog_title
import com.appswithlove.updraft.ui.resources.updraft_feedbackDisabled_description
import com.appswithlove.updraft.ui.resources.updraft_feedbackDisabled_title
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_description
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_laterButton
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_openButton
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_title
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_titleWithVersion
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_yourVersion
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateAvailableDialog(
    event: UpdraftEvent.UpdateAvailable,
    onOpen: (String) -> Unit,
    onLater: () -> Unit,
) {
    val title = if (!event.version.isNullOrBlank()) {
        stringResource(Res.string.updraft_updateAvailable_titleWithVersion, event.version!!)
    } else {
        stringResource(Res.string.updraft_updateAvailable_title)
    }
    val message = if (!event.yourVersion.isNullOrBlank()) {
        stringResource(Res.string.updraft_updateAvailable_yourVersion, event.yourVersion!!)
    } else {
        stringResource(Res.string.updraft_updateAvailable_description)
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onOpen(event.url) }) {
                Text(stringResource(Res.string.updraft_updateAvailable_openButton))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(Res.string.updraft_updateAvailable_laterButton))
            }
        },
    )
}

@Composable
fun FeedbackHintDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.updraft_feedbackDialog_title)) },
        text = { Text(stringResource(Res.string.updraft_feedbackDialog_description)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.updraft_button_ok)) }
        },
    )
}

@Composable
fun FeedbackDisabledDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.updraft_feedbackDisabled_title)) },
        text = { Text(stringResource(Res.string.updraft_feedbackDisabled_description)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.updraft_button_cancel)) }
        },
    )
}

@Composable
fun UpdraftEventHost(events: Flow<UpdraftEvent>, onFeedbackRequested: () -> Unit) {
    var updateEvent by remember { mutableStateOf<UpdraftEvent.UpdateAvailable?>(null) }
    var showHint by remember { mutableStateOf(false) }
    var showDisabled by remember { mutableStateOf(false) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UpdraftEvent.UpdateAvailable -> updateEvent = event
                UpdraftEvent.ShowFeedbackHint -> showHint = true
                UpdraftEvent.FeedbackDisabled -> showDisabled = true
                UpdraftEvent.FeedbackRequested -> onFeedbackRequested()
                UpdraftEvent.CloseFeedback -> Unit
                is UpdraftEvent.Error -> Unit
            }
        }
    }

    updateEvent?.let { event ->
        UpdateAvailableDialog(
            event = event,
            onOpen = { url -> Updraft.openUpdateUrl(url); updateEvent = null },
            onLater = { updateEvent = null },
        )
    }
    if (showHint) FeedbackHintDialog(onDismiss = { showHint = false })
    if (showDisabled) FeedbackDisabledDialog(onDismiss = { showDisabled = false })
}
```

`updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/feedback/FeedbackScreen.kt`:

```kotlin
package com.appswithlove.updraft.ui.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.ui.drawing.DrawingCanvas
import com.appswithlove.updraft.ui.drawing.DrawingController
import com.appswithlove.updraft.ui.resources.Res
import com.appswithlove.updraft.ui.resources.updraft_button_cancel
import com.appswithlove.updraft.ui.resources.updraft_button_ok
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedbackScreen(
    screenshotPng: ByteArray?,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = remember {
        FeedbackScreenState(
            send = { screenshot, type, description, email ->
                Updraft.sendFeedback(screenshot, type, description, email)
            },
            scope = scope,
        )
    }
    val drawingController = remember { DrawingController() }
    var annotating by remember { mutableStateOf(screenshotPng != null) }

    LaunchedEffect(state.result) {
        if (state.result?.isSuccess == true) onClose()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (annotating && screenshotPng != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    Image(
                        bitmap = decodePng(screenshotPng),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DrawingCanvas(drawingController, modifier = Modifier.fillMaxSize())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { drawingController.undo() }, enabled = drawingController.canUndo) { Text("Undo") }
                    TextButton(onClick = { drawingController.redo() }, enabled = drawingController.canRedo) { Text("Redo") }
                    Button(onClick = { annotating = false }) { Text(stringResource(Res.string.updraft_button_ok)) }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeedbackTypeDropdown(selected = state.selectedType, onSelect = { state.selectedType = it })
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { state.description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { state.email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.uploadProgress?.let { progress ->
                    LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
                if (state.result?.isFailure == true) {
                    Text("Upload failed. Please try again.", color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClose) { Text(stringResource(Res.string.updraft_button_cancel)) }
                    Button(
                        enabled = state.canSend,
                        onClick = {
                            val base = screenshotPng ?: ByteArray(0)
                            val annotated = if (screenshotPng != null) {
                                renderAnnotated(base, drawingController.paths)
                            } else {
                                base
                            }
                            state.sendFeedback(annotated)
                        },
                    ) {
                        if (state.uploadProgress != null) CircularProgressIndicator() else Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackTypeDropdown(selected: FeedbackType?, onSelect: (FeedbackType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenuDefaults.run {
            androidx.compose.material3.ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                FeedbackType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = { onSelect(type); expanded = false },
                    )
                }
            }
        }
    }
}
```

Create expect declarations in `updraft-ui-compose/src/commonMain/kotlin/com/appswithlove/updraft/ui/feedback/ImageCodec.kt`:

```kotlin
package com.appswithlove.updraft.ui.feedback

import androidx.compose.ui.graphics.ImageBitmap
import com.appswithlove.updraft.ui.drawing.DrawnPath

expect fun decodePng(bytes: ByteArray): ImageBitmap
expect fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>): ByteArray
```

And android actuals in `updraft-ui-compose/src/androidMain/kotlin/com/appswithlove/updraft/ui/feedback/ImageCodec.android.kt`:

```kotlin
package com.appswithlove.updraft.ui.feedback

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.appswithlove.updraft.ui.drawing.DrawnPath
import java.io.ByteArrayOutputStream

actual fun decodePng(bytes: ByteArray): ImageBitmap =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()

actual fun renderAnnotated(base: ByteArray, paths: List<DrawnPath>): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(base, 0, base.size)
        .copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    paths.forEach { drawn ->
        val paint = Paint().apply {
            color = drawn.color.toArgb()
            strokeWidth = drawn.strokeWidthPx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val path = Path()
        drawn.points.firstOrNull()?.let { path.moveTo(it.x, it.y) }
        drawn.points.drop(1).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
```

Note on annotation coordinates: `DrawingCanvas` paths are in on-screen pixel coordinates of the displayed image; the screenshot is captured at the same decor-view size on the same device, so 1:1 mapping holds when the Image fills the screen. Verify visually in Task 13; if the Image is letterboxed, scale paths by (bitmapWidth / canvasWidth) before drawing in `renderAnnotated` — implement that scaling in Task 13's manual-verification fix window if observed.

- [ ] **Step 6: Verify build + tests**

Run: `./gradlew :updraft-ui-compose:testDebugUnitTest :updraft-ui-compose:assemble`
Expected: BUILD SUCCESSFUL, tests PASS

- [ ] **Step 7: Commit**

```bash
git add updraft-ui-compose/
git commit -m "Add Compose feedback screen, dialogs, and event host"
```

---

### Task 12: Rewrap `updraft-sdk` — delete Views stack

**Files:**
- Delete: `updraft-sdk/src/main/java/com/rm/freedrawview/` (all files)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/feedback/` (all files: FeedbackActivity, FeedbackRootContainer, drawing/, form/)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/presentation/` (all files)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/manager/` (all files)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/interactor/` (all files)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/api/` (remaining files: UpdraftService, ApiWrapper, ApiException, CountingRequestBody)
- Delete: `updraft-sdk/src/main/java/com/appswithlove/updraft/Settings.kt`, legacy `LegacyUpdraft.kt`
- Delete: all layout/menu/drawable resources used only by deleted UI (`updraft-sdk/src/main/res/layout/`, res referenced by FeedbackActivity — check with grep before deleting shared drawables); keep `values*/strings.xml` until confirmed unused, then delete
- Create: `updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftFeedbackActivity.kt`
- Create: `updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftOverlayActivity.kt`
- Create: `updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftAndroid.kt`
- Modify: `updraft-sdk/src/main/AndroidManifest.xml`
- Modify: `updraft-sdk/build.gradle.kts`

**Interfaces:**
- Consumes: `Updraft` object + `UpdraftEvent` (Task 8), `FeedbackScreen` + `UpdraftEventHost` (Task 11).
- Produces: `updraft-sdk` = `api(project(":updraft-core")) + api(project(":updraft-ui-compose"))` + two host activities. Consumer-facing API is `Updraft.start(UpdraftSettings)` from core — updraft-sdk adds the auto-UI glue only.
- Auto-UI wiring: `UpdraftAndroid` is an androidx.startup `Initializer` (declared in manifest, depends on `UpdraftCoreInitializer`) that collects `Updraft.events` once `Updraft.start` runs and launches transparent `UpdraftOverlayActivity` for dialog events / `UpdraftFeedbackActivity` for `FeedbackRequested`.

- [ ] **Step 1: Delete legacy stack**

```bash
git rm -r updraft-sdk/src/main/java/com/rm \
  updraft-sdk/src/main/java/com/appswithlove/updraft/feedback \
  updraft-sdk/src/main/java/com/appswithlove/updraft/presentation \
  updraft-sdk/src/main/java/com/appswithlove/updraft/manager \
  updraft-sdk/src/main/java/com/appswithlove/updraft/interactor \
  updraft-sdk/src/main/java/com/appswithlove/updraft/api \
  updraft-sdk/src/main/java/com/appswithlove/updraft/Settings.kt \
  updraft-sdk/src/main/java/com/appswithlove/updraft/LegacyUpdraft.kt \
  updraft-sdk/src/main/res/layout
```

Then grep remaining res usages (`grep -r "R.drawable\|R.menu\|R.style" updraft-sdk/src/main/java/`) and `git rm` orphaned drawable/menu/style files. Remove Loco block and now-unused deps from `updraft-sdk/build.gradle.kts` (retrofit, adapter-rxjava2, logging-interceptor, ink, lifecycle-extensions, lifecycle-compiler, viewBinding flag, appcompat, material, converter-kotlinx-serialization) and remove `alias(libs.plugins.loco)`. Delete `updraft-sdk/src/main/res/values*/strings.xml` once ui-compose owns strings (Task 9 migrated them).

Resulting `updraft-sdk/build.gradle.kts` dependencies block:

```kotlin
dependencies {
    api(project(":updraft-core"))
    api(project(":updraft-ui-compose"))
    implementation(libs.core.ktx)
    implementation(libs.androidx.startup)
    implementation(libs.kotlinx.coroutines.core)
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui:1.9.4")
}
```

(Add `activityCompose = "1.12.0"` / catalog entries instead of string literals: `androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }` — use the catalog form.)

- [ ] **Step 2: Implement host activities + initializer**

`updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftFeedbackActivity.kt`:

```kotlin
package com.appswithlove.updraft.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.ui.feedback.FeedbackScreen

class UpdraftFeedbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenshot = pendingScreenshot
        pendingScreenshot = null
        setContent {
            FeedbackScreen(
                screenshotPng = screenshot,
                onClose = { finish() },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Updraft.onFeedbackUiClosed()
    }

    companion object {
        private var pendingScreenshot: ByteArray? = null

        fun launch(context: Context, screenshotPng: ByteArray?) {
            pendingScreenshot = screenshotPng
            context.startActivity(
                Intent(context, UpdraftFeedbackActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            (context as? Activity)?.overridePendingTransition(0, 0)
        }
    }
}
```

(Screenshot passed via static holder, not Intent extra — PNG can exceed the 1 MB Binder limit.)

`updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftOverlayActivity.kt`:

```kotlin
package com.appswithlove.updraft.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.ui.dialogs.FeedbackDisabledDialog
import com.appswithlove.updraft.ui.dialogs.FeedbackHintDialog
import com.appswithlove.updraft.ui.dialogs.UpdateAvailableDialog
import com.appswithlove.updraft.Updraft

class UpdraftOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val event = pendingEvent
        pendingEvent = null
        if (event == null) {
            finish()
            return
        }
        setContent {
            when (event) {
                is UpdraftEvent.UpdateAvailable -> UpdateAvailableDialog(
                    event = event,
                    onOpen = { url -> Updraft.openUpdateUrl(url); finish() },
                    onLater = { finish() },
                )
                UpdraftEvent.ShowFeedbackHint -> FeedbackHintDialog(onDismiss = { finish() })
                UpdraftEvent.FeedbackDisabled -> FeedbackDisabledDialog(onDismiss = { finish() })
                else -> finish()
            }
        }
    }

    companion object {
        private var pendingEvent: UpdraftEvent? = null

        fun launch(context: Context, event: UpdraftEvent) {
            pendingEvent = event
            context.startActivity(
                Intent(context, UpdraftOverlayActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
```

`updraft-sdk/src/main/java/com/appswithlove/updraft/android/UpdraftAndroid.kt`:

```kotlin
package com.appswithlove.updraft.android

import android.content.Context
import androidx.startup.Initializer
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.platform.UpdraftContext
import com.appswithlove.updraft.platform.UpdraftCoreInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object UpdraftAndroid {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    fun autoWire() {
        if (started) return
        started = true
        scope.launch {
            // Wait until the host app calls Updraft.start()
            while (!runCatching { Updraft.settings }.isSuccess) delay(100)
            Updraft.events.collect { event ->
                val context = UpdraftContext.application
                when (event) {
                    is UpdraftEvent.UpdateAvailable,
                    UpdraftEvent.ShowFeedbackHint,
                    UpdraftEvent.FeedbackDisabled,
                    -> UpdraftOverlayActivity.launch(context, event)

                    UpdraftEvent.FeedbackRequested ->
                        UpdraftFeedbackActivity.launch(context, Updraft.takePendingScreenshot())

                    UpdraftEvent.CloseFeedback, is UpdraftEvent.Error -> Unit
                }
            }
        }
    }
}

class UpdraftSdkInitializer : Initializer<UpdraftAndroid> {
    override fun create(context: Context): UpdraftAndroid {
        UpdraftAndroid.autoWire()
        return UpdraftAndroid
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(UpdraftCoreInitializer::class.java)
}
```

`Updraft.takePendingScreenshot()` is `internal` in core — since updraft-sdk is a separate module, widen it: in Task 8's `Updraft.kt` change `internal fun takePendingScreenshot()` to `fun takePendingScreenshot()` with KDoc `/** For SDK-internal UI hosts. Returns the screenshot captured for the pending feedback request, once. */`.

`updraft-sdk/src/main/AndroidManifest.xml` — replace activity/provider entries with:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <application>
        <activity
            android:name="com.appswithlove.updraft.android.UpdraftFeedbackActivity"
            android:exported="false"
            android:theme="@style/Theme.AppCompat.DayNight.NoActionBar" />
        <activity
            android:name="com.appswithlove.updraft.android.UpdraftOverlayActivity"
            android:exported="false"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="com.appswithlove.updraft.android.UpdraftSdkInitializer"
                android:value="androidx.startup" />
        </provider>
    </application>
</manifest>
```

If `Theme.AppCompat` reference fails without appcompat dep, use `@android:style/Theme.Material.NoActionBar` instead — activities are Compose-only.

- [ ] **Step 3: Build**

Run: `./gradlew :updraft-sdk:assembleDebug`
Expected: BUILD SUCCESSFUL. `app/` module will now FAIL to compile (uses old API) — expected; fixed in Task 13. Verify with `./gradlew :updraft-sdk:assembleDebug` only.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Rewrap updraft-sdk as Compose host over core + ui-compose"
```

---

### Task 13: Sample app on new API + manual verification

**Deviation from spec:** spec proposed renaming `app/` → `sample/` with CMP structure. Deferred to M2 (when `iosApp` arrives and the rename is forced anyway) — M1 keeps `app/` as the Android sample. YAGNI.

**Files:**
- Modify: `app/src/main/java/com/appswithlove/updraftsdk/App.kt`
- Modify: `app/src/main/java/com/appswithlove/updraftsdk/MainActivity.kt` (only if it references removed API)
- Modify: `app/build.gradle.kts` (ensure it depends on `project(":updraft-sdk")` only)

**Interfaces:**
- Consumes: `Updraft.start(UpdraftSettings)` (Task 8), auto-UI from Task 12.

- [ ] **Step 1: Port App.kt**

```kotlin
package com.appswithlove.updraftsdk

import android.app.Application
import com.appswithlove.updraft.LogLevel
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftSettings

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Updraft.start(
            UpdraftSettings(
                appKey = APP_KEY,
                sdkKey = SDK_KEY,
                baseUrl = UpdraftSettings.BASE_URL_STAGING,
                logLevel = LogLevel.Debug,
                showFeedbackAlert = true,
            ),
        )
    }

    companion object {
        private const val APP_KEY = "<existing key from current App.kt>"
        private const val SDK_KEY = "<existing key from current App.kt>"
    }
}
```

(Copy the actual key constants from the current `App.kt` before editing.)

- [ ] **Step 2: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all module tests pass

- [ ] **Step 3: Manual device verification (checklist)**

Install: `./gradlew :app:installDebug`. On device against staging:

1. Launch → feedback hint dialog appears (first launch).
2. Shake → screenshot annotate screen; draw; undo/redo; OK → form; select type; send → progress → closes. Verify annotation aligns with drawing (if offset/letterboxed, apply the scaling fix noted in Task 11).
3. Feedback arrives in Updraft staging dashboard with image + metadata.
4. With lower build number installed: update dialog on launch; "Open" opens browser.
5. Second launch: hint dialog NOT shown again in same process; feedback-enabled toggle on dashboard triggers disabled/how-to dialog on next foreground.

Fix any failures before commit; note fixes in commit message.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Port sample app to Updraft.start API"
```

---

### Task 14: Version 2.0.0, README, migration guide

**Files:**
- Modify: `build.gradle.kts` / wherever `version` is set for publishing (check root `build.gradle.kts` and `gradle.properties` for `VERSION_NAME`)
- Modify: `README.md`
- Modify: docs spec checklist (tick nothing — just leave)

**Interfaces:** none (docs only).

- [ ] **Step 1: Bump version**

Find current version declaration: `grep -rn "1.1.2\|VERSION_NAME" gradle.properties build.gradle.kts updraft-sdk/`. Set to `2.0.0` for all three published modules.

- [ ] **Step 2: Rewrite README**

Update README sections:
- Installation: three artifacts table (`updraft-sdk` = Android all-in; `updraft-core` = KMP commonMain logic-only; `updraft-core` + `updraft-ui-compose` = CMP full UI), version `2.0.0`.
- Setup: `Updraft.start(UpdraftSettings(...))` snippet (replaces `Settings()`/`initialize`).
- New section "Migrating from 1.x": table mapping `Settings` → `UpdraftSettings` fields, `Updraft.initialize(this, settings)` + `getInstance()?.start()` → `Updraft.start(settings)`, `Settings.LOG_LEVEL_DEBUG` → `LogLevel.Debug`, note Compose runtime now included and `updraft-core`-only escape hatch for size-sensitive Views apps.
- New section "KMP / Compose Multiplatform": commonMain usage, `setFeedbackUiPresenter` for native-UI apps, `UpdraftEventHost` + `FeedbackScreen` embedding for CMP apps; note iOS targets land in M2.
- Local Development section: keep, still valid (`publishToMavenLocal`).

- [ ] **Step 3: Build + commit**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

```bash
git add -A
git commit -m "Bump to 2.0.0, document KMP/CMP setup and 1.x migration"
```

---

## Post-plan reminders (do NOT implement in M1)

- Revisit options for Views-based Android apps after M1 (spec "Post-migration reminder").
- M2 plan: iOS targets/actuals, XCFramework, iosApp sample, deprecate updraft-sdk-ios.
- Relative-age copy in update dialog (dropped `DateUtils` formatting) — restore with `kotlinx-datetime` in follow-up.
- CI: `.github/workflows/publish.yml` publishes on release — verify it runs `publishAllPublicationsToMavenCentralRepository`-equivalent for all three modules before first 2.0.0 release.
