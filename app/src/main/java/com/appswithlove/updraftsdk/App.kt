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
        val settings = Settings()
        settings.appKey = Keys.APP_KEY
        settings.sdkKey = Keys.SDK_KEY
        settings.isStoreRelease = false
        settings.logLevel = Settings.LOG_LEVEL_DEBUG
        settings.showStartAlert = false
        Updraft.initialize(this, settings)
        Updraft.getInstance().start()
    }
}
