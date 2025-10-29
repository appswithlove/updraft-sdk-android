package com.appswithlove.updraft.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackMobileResponse(
    @SerialName("image") val image: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("tag") val tag: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("build_version") val buildVersion: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("device_uuid") val deviceUuid: String? = null,
    @SerialName("navigation_stack") val navigationStack: String? = null,
)
