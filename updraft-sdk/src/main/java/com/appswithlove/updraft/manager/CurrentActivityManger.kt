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
    private var currentLifecycle: LifecycleState? = null

    private val listeners: MutableSet<CurrentActivityListener> = mutableSetOf()

    fun addListener(listener: CurrentActivityListener) {
        listeners.add(listener)
        dispatchCurrentActivityState()
    }

    fun removeListener(listener: CurrentActivityListener) {
        listeners.remove(listener)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        currentLifecycle = LifecycleState.Resumed
        dispatchCurrentActivityState()
    }

    override fun onActivityPaused(activity: Activity) {
        currentLifecycle = LifecycleState.Paused
        dispatchCurrentActivityState()
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
        currentLifecycle = null
    }

    private fun dispatchCurrentActivityState() {
        val activity = currentActivity
        if (activity != null) {
            listeners.forEach { listener ->
                if (currentLifecycle == LifecycleState.Resumed) {
                    listener.onActivityResumed(activity)
                } else if (currentLifecycle == LifecycleState.Paused) {
                    listener.onActivityPaused(activity)
                }
            }
        }
    }

    interface CurrentActivityListener {
        fun onActivityResumed(activity: Activity)
        fun onActivityPaused(activity: Activity)
    }

    private enum class LifecycleState {
        Resumed,
        Paused
    }
}