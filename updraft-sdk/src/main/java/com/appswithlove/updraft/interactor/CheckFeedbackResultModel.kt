package com.appswithlove.updraft.interactor

data class CheckFeedbackResultModel(
    val showAlert: Boolean,
    val alertType: Int,
    val isFeedbackEnabled: Boolean
) {
    companion object {
        const val ALERT_TYPE_HOW_TO_GIVE_FEEDBACK = 0
        const val ALERT_TYPE_FEEDBACK_DISABLED = 1
    }
}
