package com.appswithlove.updraft.manager

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appswithlove.updraft.Settings

class ShakeDetectorManager(
    application: Application,
    private val settings: Settings,
    private val shakeDetectorListener: ShakeDetectorListener
) : DefaultLifecycleObserver, SensorEventListener {

    interface ShakeDetectorListener {
        fun onShakeDetected()
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
        private const val UPD_TAG = "ShakeDetectorManager"
    }

    private val sensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeTimestamp: Long = 0
    private var shakeCount: Int = 0
    private var shouldListen = true

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = kotlin.math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

//        if (settings.logLevel == Settings.LOG_LEVEL_DEBUG) {
//             Log.d(UPD_TAG, "Sensor change with gForce = $gForce")
//        }

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()

            // Ignore shakes too close to each other
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) return

            // Reset count if no shakes in the last 3 seconds
            if (shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0
            }

            shakeTimestamp = now
            shakeCount++
            onShaken()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun onShaken() {
        if (shouldListen) {
            if (settings.logLevel == Settings.LOG_LEVEL_DEBUG) {
                Log.d(UPD_TAG, "Shake event detected")
            }
            shakeDetectorListener.onShakeDetected()
            stopListening()
        }
    }

    fun startListening() {
        shouldListen = true
    }

    fun stopListening() {
        shouldListen = false
    }
}
