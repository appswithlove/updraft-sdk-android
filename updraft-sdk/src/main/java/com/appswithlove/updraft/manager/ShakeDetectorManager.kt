package com.appswithlove.updraft.manager

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.appswithlove.updraft.Settings
import com.appswithlove.updraft.Updraft

class ShakeDetectorManager(
    application: Application,
    settings: Settings,
    shakeDetectorListener: ShakeDetectorListener?
) : LifecycleObserver, SensorEventListener {
    interface ShakeDetectorListener {
        fun onShakeDetected()
    }

    private val mSensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mSettings: Settings = settings
    private val mAccelerometer: Sensor
    private val mShakeDetectorListener: ShakeDetectorListener?
    private var mShakeTimestamp: Long = 0
    private var mShakeCount = 0
    private var mShouldListen = true
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        mSensorManager.registerListener(
            this,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // gForce will be close to 1 when there is no movement.
        val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble())
            .toFloat()
        if (mSettings.logLevel == Settings.LOG_LEVEL_DEBUG) {
            Log.d(Updraft.UPDRAFT_TAG, "sensor change with gForce = $gForce")
        }
        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            // ignore shake events too close to each other (500ms)
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            // reset the shake count after 3 seconds of no shakes
            if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                mShakeCount = 0
            }
            mShakeTimestamp = now
            mShakeCount++
            onShaked()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    private fun onShaked() {
        if (mShouldListen) {
            if (mSettings.logLevel == Settings.LOG_LEVEL_DEBUG) {
                Log.d(Updraft.UPDRAFT_TAG, "shake event detected")
            }
            mShakeDetectorListener?.onShakeDetected()
            stopListening()
        }
    }

    fun startListening() {
        mShouldListen = true
    }

    fun stopListening() {
        mShouldListen = false
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }

    init {
        mAccelerometer = mSensorManager
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mShakeDetectorListener = shakeDetectorListener
    }
}
