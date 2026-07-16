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
        private const val APP_KEY = ""
        private const val SDK_KEY = ""
    }
}