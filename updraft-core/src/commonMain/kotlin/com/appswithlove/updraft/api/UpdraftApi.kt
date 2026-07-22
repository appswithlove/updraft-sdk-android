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
import com.appswithlove.updraft.platform.currentNavigationStack
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

interface UpdraftApiContract {
    suspend fun checkLastVersion(): CheckLastVersionResponse
    suspend fun getLastVersion(): GetLastVersionResponse
    suspend fun isFeedbackEnabled(): Boolean
    fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double>
}

class UpdraftApi(
    private val settings: UpdraftSettings,
    private val appInfo: AppInfo,
    engine: HttpClientEngine? = null,
) : UpdraftApiContract {
    private val client: HttpClient = (engine?.let { HttpClient(it) } ?: HttpClient()).config {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = if (settings.logLevel == LogLevel.Debug) KtorLogLevel.BODY else KtorLogLevel.NONE
        }
    }

    private fun url(path: String) = settings.baseUrl + path

    override suspend fun checkLastVersion(): CheckLastVersionResponse {
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

    override suspend fun getLastVersion(): GetLastVersionResponse =
        client.post(url("get_last_version/")) {
            contentType(ContentType.Application.Json)
            setBody(GetLastVersionRequest(sdkKey = settings.sdkKey, appKey = settings.appKey))
        }.body()

    override suspend fun isFeedbackEnabled(): Boolean {
        val response: FeedbackEnabledResponse = client.post(url("feedback-mobile-enabled/")) {
            contentType(ContentType.Application.Json)
            setBody(FeedbackEnabledRequest(sdkKey = settings.sdkKey, appKey = settings.appKey))
        }.body()
        if (response.errorCodes.isNotEmpty()) {
            throw ApiException(response.errorDescriptions.firstOrNull().orEmpty())
        }
        return response.isFeedbackEnabled
    }

    override fun sendFeedback(
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
            append("device_uudid", appInfo.deviceUuid)
            append("navigation_stack", currentNavigationStack())
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
            } catch (t: kotlin.coroutines.cancellation.CancellationException) {
                throw t
            } catch (t: Throwable) {
                close(t)
            }
        }
        awaitClose { uploadJob.cancel() }
    }
}
