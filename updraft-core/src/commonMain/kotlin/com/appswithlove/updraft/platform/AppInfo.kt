package com.appswithlove.updraft.platform

class AppInfo(
    val versionCode: Long,
    val versionName: String,
    val systemVersion: String,
    val deviceName: String,
    val deviceUuid: String,
)

expect fun currentAppInfo(): AppInfo
