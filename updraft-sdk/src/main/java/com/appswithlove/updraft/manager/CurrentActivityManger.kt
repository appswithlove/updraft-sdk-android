package com.appswithlove.updraft.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle

@SuppressLint("StaticFieldLeak")
class CurrentActivityManger private constructor() : Application.ActivityLifecycleCallbacks {

    companion object {
        val INSTANCE: CurrentActivityManger by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            CurrentActivityManger()
        }
    }

    private var currentActivity: Activity? = null

    private val listeners: MutableSet<CurrentActivityListener> = mutableSetOf()

    fun addListener(listener: CurrentActivityListener) {
        listeners.add(listener)
        dispatchCurrentActivityState()
    }

    fun removeListener(listener: CurrentActivityListener) {
        listeners.remove(listener)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        dispatchCurrentActivityState()
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = null
        dispatchCurrentActivityState()
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private fun dispatchCurrentActivityState() {
        val activity = currentActivity
        if (activity != null) {
            listeners.forEach { listener ->
                if (currentActivity != null) {
                    listener.onActivityResumed(activity)
                } else {
                    listener.onActivityPaused(activity)
                }
            }
        }
    }

    interface CurrentActivityListener {
        fun onActivityResumed(activity: Activity)
        fun onActivityPaused(activity: Activity)
    }
}