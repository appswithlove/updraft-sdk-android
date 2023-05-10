package com.appswithlove.updraft

/**
 * Created by satori on 3/27/18.
 */
class Settings {
    var sdkKey: String? = null
    var appKey: String? = null
    var isStoreRelease = false
    var baseUrl = BASE_URL_PROD
    var logLevel = LOG_LEVEL_ERROR
    var showFeedbackAlert = false
    var feedbackEnabled = true // force disabling feedback if needed
    fun shouldShowErrors(): Boolean {
        return logLevel == LOG_LEVEL_DEBUG || logLevel == LOG_LEVEL_ERROR
    }

    companion object {
        const val BASE_URL_STAGING = "https://u2.mqd.me/api/"
        const val BASE_URL_PROD = "https://getupdraft.com/api/"
        const val LOG_LEVEL_NONE = 0
        const val LOG_LEVEL_ERROR = 1
        const val LOG_LEVEL_DEBUG = 2
    }
}