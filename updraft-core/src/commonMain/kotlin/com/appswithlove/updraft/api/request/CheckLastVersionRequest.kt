package com.appswithlove.updraft.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckLastVersionRequest(
    @SerialName("sdk_key") val sdkKey: String? = null,
    @SerialName("app_key") val appKey: String? = null,
    @SerialName("version") val version: String? = null,
)
