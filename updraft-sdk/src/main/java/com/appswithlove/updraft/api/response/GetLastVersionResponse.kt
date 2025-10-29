package com.appswithlove.updraft.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetLastVersionResponse(
    @SerialName("update_url") val updateUrl: String? = null,
)
