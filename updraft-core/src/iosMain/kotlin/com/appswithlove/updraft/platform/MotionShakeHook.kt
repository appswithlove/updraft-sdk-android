package com.appswithlove.updraft.platform

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSLog
import platform.darwin.NSInteger
import platform.objc.class_getInstanceMethod
import platform.objc.method_getImplementation
import platform.objc.method_setImplementation
import platform.objc.objc_getClass
import platform.objc.sel_registerName

private const val UI_EVENT_SUBTYPE_MOTION_SHAKE: NSInteger = 1

private var installed = false
private var onShakeCallback: (() -> Unit)? = null

@OptIn(ExperimentalForeignApi::class)
private var originalImp: COpaquePointer? = null

@OptIn(ExperimentalForeignApi::class)
private fun motionEndedImp(self: COpaquePointer?, cmd: COpaquePointer?, motion: NSInteger, event: COpaquePointer?) {
    if (motion == UI_EVENT_SUBTYPE_MOTION_SHAKE) {
        NSLog("Updraft: shake motion received")
        onShakeCallback?.invoke()
    }
    originalImp
        ?.reinterpret<CFunction<(COpaquePointer?, COpaquePointer?, NSInteger, COpaquePointer?) -> Unit>>()
        ?.invoke(self, cmd, motion, event)
}

/**
 * Replaces UIWindow.motionEnded:withEvent: so shake gestures reach the SDK
 * without any host-app integration. This is the only mechanism that works on
 * the simulator (Device > Shake sends a responder-chain motion event, not
 * accelerometer data) and it also covers physical devices. The replacement is
 * a raw C function (IMP) — Kotlin lambda-to-block bridging must not be used
 * here because it would marshal the NSInteger motion argument as an object.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun installMotionShakeHook(onShake: () -> Unit) {
    onShakeCallback = onShake
    if (installed) return
    installed = true

    val windowClass = objc_getClass("UIWindow") ?: run {
        NSLog("Updraft: UIWindow class not found, shake hook not installed")
        return
    }
    val selector = sel_registerName("motionEnded:withEvent:")
    val method = class_getInstanceMethod(windowClass as? ObjCClass, selector) ?: run {
        NSLog("Updraft: motionEnded:withEvent: not found, shake hook not installed")
        return
    }
    originalImp = method_getImplementation(method)
    method_setImplementation(method, staticCFunction(::motionEndedImp).reinterpret())
    NSLog("Updraft: shake hook installed on UIWindow")
}
