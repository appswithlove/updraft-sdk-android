package com.appswithlove.updraftsdk

import android.app.Application
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraftsdk.Keys.APP_KEY
import com.appswithlove.updraftsdk.Keys.SDK_KEY

/**
 * Created by satori on 3/27/18.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val settings = Settings()
        settings.appKey = APP_KEY
        settings.sdkKey = SDK_KEY
        settings.isStoreRelease = false
        settings.logLevel = Settings.LOG_LEVEL_DEBUG
        settings.showFeedbackAlert = true
        Updraft.initialize(this, settings)
        Updraft.getInstance()?.start()
    }
}