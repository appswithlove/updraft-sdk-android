package com.appswithlove.updraftsdk

import android.app.Application
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft

/**
 * Created by satori on 3/27/18.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val settings = Settings().apply {
            appKey = "" //APP_KEY
            sdkKey = "" // SDK_KEY
            isStoreRelease = false
            logLevel = Settings.LOG_LEVEL_DEBUG
            showFeedbackAlert = false
            feedbackEnabled = true
        }

        Updraft.initialize(this, settings)
        Updraft.getInstance()?.start()
    }
}