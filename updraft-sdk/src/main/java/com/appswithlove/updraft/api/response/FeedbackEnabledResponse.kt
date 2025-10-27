package com.appswithlove.updraft.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackEnabledResponse(
    @SerialName("is_feedback_enabled") val isFeedbackEnabled: Boolean,
    @SerialName("error_code") val errorCodes: List<String> = emptyList(),
    @SerialName("error_description") val errorDescriptions: List<String> = emptyList(),
)
