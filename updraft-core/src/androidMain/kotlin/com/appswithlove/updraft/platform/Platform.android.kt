package com.appswithlove.updraft.platform

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.io.ByteArrayOutputStream

private class AndroidShakeDetector(private val onShake: () -> Unit) :
    ShakeDetector, SensorEventListener, DefaultLifecycleObserver {

    private val sensorManager =
        UpdraftContext.application.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeTimestamp = 0L
    private var enabled = true

    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        sensorManager.unregisterListener(this)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun onStart(owner: LifecycleOwner) {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onStop(owner: LifecycleOwner) {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (!isShakeGForce(x, y, z, SensorManager.GRAVITY_EARTH)) return

        val now = System.currentTimeMillis()
        if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) return
        shakeTimestamp = now

        if (enabled) {
            enabled = false
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_SLOP_TIME_MS = 500
    }
}

actual fun createShakeDetector(onShake: () -> Unit): ShakeDetector = AndroidShakeDetector(onShake)

private class AndroidScreenshotGrabber : ScreenshotGrabber {
    override fun capturePng(): ByteArray? {
        val activity = CurrentActivityManager.current ?: return null
        val view = activity.window.decorView.rootView
        if (view.width == 0 || view.height == 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }
}

actual fun createScreenshotGrabber(): ScreenshotGrabber = AndroidScreenshotGrabber()

actual fun openUrl(url: String) {
    val activity = CurrentActivityManager.current ?: return
    activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

private class AndroidForegroundObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
) : AppForegroundObserver, DefaultLifecycleObserver {
    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) = onForeground()
    override fun onStop(owner: LifecycleOwner) = onBackground()
}

actual fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver =
    AndroidForegroundObserver(onForeground, onBackground)
