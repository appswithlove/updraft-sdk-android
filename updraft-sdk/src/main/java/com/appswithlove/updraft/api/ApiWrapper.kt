package com.appswithlove.updraft.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.api.request.CheckLastVersionRequest
import com.appswithlove.updraft.api.request.FeedbackEnabledRequest
import com.appswithlove.updraft.api.request.FeedbackMobileRequest
import com.appswithlove.updraft.api.request.GetLastVersionRequest
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import com.appswithlove.updraft.feedback.form.FeedbackChoice
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class ApiWrapper(
    private val context: Context,
    private val settings: Settings
) {

    private val updraftService: UpdraftService

    init {
        val httpClientBuilder = OkHttpClient.Builder()

        val logging = HttpLoggingInterceptor().apply {
            level = when (settings.logLevel) {
                Settings.LOG_LEVEL_DEBUG -> HttpLoggingInterceptor.Level.BODY
                Settings.LOG_LEVEL_NONE, Settings.LOG_LEVEL_ERROR -> HttpLoggingInterceptor.Level.NONE
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }
        httpClientBuilder.addInterceptor(logging)

        val retrofit = Retrofit.Builder()
            .baseUrl(settings.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(httpClientBuilder.build())
            .build()

        updraftService = retrofit.create(UpdraftService::class.java)
    }

    fun getLastVersion(): Single<GetLastVersionResponse> =
        Single.defer {
            val request = GetLastVersionRequest(
                sdkKey = settings.sdkKey,
                appKey = settings.appKey,
            )
            updraftService.getLastVersion(request)
        }

    fun checkLastVersion(): Single<CheckLastVersionResponse> =
        Single.defer {
            val request = CheckLastVersionRequest(
                appKey = settings.appKey,
                sdkKey = settings.sdkKey,
            )

            val versionCode = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                -1
            }

            if (versionCode == -1) {
                Single.error(IllegalStateException("Version code is invalid"))
            } else {
                request.copy(
                    version = versionCode.toString()
                )
                updraftService.checkLastVersion(request)
            }
        }

    fun isFeedbackEnabled(): Single<Boolean> =
        Single.defer {
            val request = FeedbackEnabledRequest(
                appKey = settings.appKey,
                sdkKey = settings.sdkKey,
            )

            updraftService.isFeedbackEnabled(request).flatMap { response ->
                if (response.errorCodes.isNotEmpty()) {
                    val errorDescription = response.errorDescriptions.firstOrNull().orEmpty()
                    Single.error(ApiException(errorDescription))
                } else {
                    Single.just(response.isFeedbackEnabled)
                }
            }
        }

    @SuppressLint("HardwareIds", "CheckResult")
    fun sendMobileFeedback(
        feedbackChoice: FeedbackChoice,
        description: String,
        email: String,
        fileName: String
    ): Observable<Double> =
        Observable.defer {
            val request = FeedbackMobileRequest(
                appKey = settings.appKey,
                sdkKey = settings.sdkKey,
                tag = feedbackChoice.apiName(),
                image = fileName,
                description = description,
                email = email,
                buildVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    ""
                },
                systemVersion = Build.VERSION.RELEASE,
                deviceName = Build.MODEL,
                deviceUuid = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ),
            )

            Observable.create { emitter ->
                try {
                    updraftService.feedbackMobile(
                        mapMultipart(request) { bytesWritten, contentLength ->
                            val progress = 1.0 * bytesWritten / contentLength
                            emitter.onNext(progress)
                        }
                    ).blockingGet()
                } catch (t: Throwable) {
                    if (settings.shouldShowErrors()) {
                        t.printStackTrace()
                    }
                    emitter.onError(t)
                }
                emitter.onComplete()
            }
        }

    private fun mapMultipart(
        request: FeedbackMobileRequest,
        listener: CountingRequestBody.Listener
    ): Map<String, RequestBody> {
        val map = mutableMapOf<String, RequestBody>()
        val imageFile = File(context.filesDir, request.image ?: "")
        val imageRequestBody = CountingRequestBody(
            imageFile.asRequestBody("image/png".toMediaTypeOrNull()),
            listener,
        )
        val plainText = "text/plain".toMediaTypeOrNull()

        map["image\"; filename=\"${imageFile.name}"] = imageRequestBody

        request.appKey?.let { map["app_key"] = it.toRequestBody(plainText) }
        request.sdkKey?.let { map["sdk_key"] = it.toRequestBody(plainText) }
        request.tag?.let { map["tag"] = it.toRequestBody(plainText) }
        request.description?.let { map["description"] = it.toRequestBody(plainText) }
        request.email?.let { map["email"] = it.toRequestBody(plainText) }
        request.buildVersion?.let { map["build_version"] = it.toRequestBody(plainText) }
        request.systemVersion?.let { map["system_version"] = it.toRequestBody(plainText) }
        request.deviceName?.let { map["device_name"] = it.toRequestBody(plainText) }
        request.deviceUuid?.let { map["device_uuid"] = it.toRequestBody(plainText) }
        request.navigationStack?.let { map["navigation_stack"] = it.toRequestBody(plainText) }

        return map
    }
}
