package com.appswithlove.updraft.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle

object CurrentActivityManager : Application.ActivityLifecycleCallbacks {

    var current: Activity? = null
        private set

    private val activityStack = mutableListOf<Activity>()
    val stack: List<Activity> get() = activityStack

    private val listeners = mutableSetOf<(Activity?) -> Unit>()

    fun addListener(listener: (Activity?) -> Unit) {
        listeners.add(listener)
        listener(current)
    }

    override fun onActivityResumed(activity: Activity) {
        current = activity
        listeners.forEach { it(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        current = null
        listeners.forEach { it(null) }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        activityStack.add(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.remove(activity)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
}
