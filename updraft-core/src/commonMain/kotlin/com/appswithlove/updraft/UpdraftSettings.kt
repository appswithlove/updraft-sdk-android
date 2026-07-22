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
    }
}
