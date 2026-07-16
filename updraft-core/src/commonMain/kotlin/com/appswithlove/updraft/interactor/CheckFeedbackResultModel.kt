package com.appswithlove.updraft.interactor

class CheckFeedbackResultModel(
    val showAlert: Boolean,
    val alertType: AlertType,
    val isFeedbackEnabled: Boolean,
) {
    enum class AlertType { FeedbackDisabled, HowToGiveFeedback }
}
