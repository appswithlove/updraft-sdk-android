package com.appswithlove.updraft.manager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.startup.Initializer

@SuppressLint("Unused")
class SdkInitializer : Initializer<CurrentActivityManger> {
    override fun create(context: Context): CurrentActivityManger {
        val activityManger = CurrentActivityManger.INSTANCE
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(activityManger)
        return activityManger
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
