package com.appswithlove.updraft.platform

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual fun currentAppInfo(): AppInfo {
    val info = NSBundle.mainBundle.infoDictionary
    val versionCode = (info?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: -1L
    val versionName = info?.get("CFBundleShortVersionString") as? String ?: ""
    val device = UIDevice.currentDevice
    return AppInfo(
        versionCode = versionCode,
        versionName = versionName,
        systemVersion = device.systemVersion,
        deviceName = device.model,
        deviceUuid = device.identifierForVendor?.UUIDString ?: "",
    )
}
