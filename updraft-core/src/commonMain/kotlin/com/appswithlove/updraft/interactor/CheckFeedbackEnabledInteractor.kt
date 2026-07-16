package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.platform.KeyValueStore

class CheckFeedbackEnabledInteractor(
    private val api: UpdraftApiContract,
    private val store: KeyValueStore,
) {
    suspend fun run(): CheckFeedbackResultModel {
        val isEnabled = api.isFeedbackEnabled()
        val previouslyEnabled = store.getBoolean(IS_FEEDBACK_ENABLED_PROPERTY, false)

        val showAlert = isEnabled != previouslyEnabled
        val alertType = if (!isEnabled && previouslyEnabled) {
            CheckFeedbackResultModel.AlertType.FeedbackDisabled
        } else {
            CheckFeedbackResultModel.AlertType.HowToGiveFeedback
        }
        store.putBoolean(IS_FEEDBACK_ENABLED_PROPERTY, isEnabled)
        return CheckFeedbackResultModel(showAlert, alertType, isEnabled)
    }

    companion object {
        const val STORE_NAME = "feedback_enabled_storage"
        private const val IS_FEEDBACK_ENABLED_PROPERTY = "is_feedback_enabled_property"
    }
}
