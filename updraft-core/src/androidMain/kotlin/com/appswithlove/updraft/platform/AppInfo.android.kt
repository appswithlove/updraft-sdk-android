package com.appswithlove.updraft.platform

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

@SuppressLint("HardwareIds")
actual fun currentAppInfo(): AppInfo {
    val context = UpdraftContext.application
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    val versionCode = when {
        packageInfo == null -> -1L
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageInfo.longVersionCode
        else -> @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    }
    return AppInfo(
        versionCode = versionCode,
        versionName = packageInfo?.versionName.orEmpty(),
        systemVersion = Build.VERSION.RELEASE.orEmpty(),
        deviceName = Build.MODEL.orEmpty(),
        deviceUuid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
    )
}
