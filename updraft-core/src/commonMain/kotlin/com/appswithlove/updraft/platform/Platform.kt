package com.appswithlove.updraft.platform

import kotlin.math.sqrt

interface ShakeDetector {
    fun start()
    fun stop()
    fun setEnabled(enabled: Boolean)
}

expect fun createShakeDetector(onShake: () -> Unit): ShakeDetector

interface ScreenshotGrabber {
    fun capturePng(): ByteArray?
}

expect fun createScreenshotGrabber(): ScreenshotGrabber

expect fun openUrl(url: String)

interface AppForegroundObserver {
    fun start()
}

expect fun createAppForegroundObserver(onForeground: () -> Unit, onBackground: () -> Unit): AppForegroundObserver

const val SHAKE_THRESHOLD_GRAVITY = 2.7f

fun isShakeGForce(x: Float, y: Float, z: Float, gravity: Float): Boolean {
    val gX = x / gravity
    val gY = y / gravity
    val gZ = z / gravity
    return sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()) > SHAKE_THRESHOLD_GRAVITY
}
