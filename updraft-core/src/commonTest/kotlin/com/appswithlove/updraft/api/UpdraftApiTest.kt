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
