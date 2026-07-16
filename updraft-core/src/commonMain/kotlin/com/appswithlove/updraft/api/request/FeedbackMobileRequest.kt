package com.appswithlove.updraft.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackMobileRequest(
    @SerialName("sdk_key") val sdkKey: String? = null,
    @SerialName("app_key") val appKey: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("tag") val tag: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("build_version") val buildVersion: String? = null,
    @SerialName("system_version") val systemVersion: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("device_uuid") val deviceUuid: String? = null,
    @SerialName("navigation_stack") val navigationStack: String? = null,
)
