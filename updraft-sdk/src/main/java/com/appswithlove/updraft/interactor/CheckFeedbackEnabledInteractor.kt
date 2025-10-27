package com.appswithlove.updraft.interactor

import android.content.Context
import android.content.SharedPreferences
import com.appswithlove.updraft.api.ApiWrapper
import io.reactivex.Single
import androidx.core.content.edit

class CheckFeedbackEnabledInteractor(
    private val apiWrapper: ApiWrapper,
    context: Context
) {

    companion object {
        private const val FEEDBACK_ENABLED_STORAGE = "feedback_enabled_storage"
        private const val IS_FEEDBACK_ENABLED_PROPERTY = "is_feedback_enabled_property"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(FEEDBACK_ENABLED_STORAGE, Context.MODE_PRIVATE)

    fun run(): Single<CheckFeedbackResultModel> {
        return apiWrapper.isFeedbackEnabled()
            .map { isEnabled ->
                val previouslyEnabled = sharedPreferences.getBoolean(IS_FEEDBACK_ENABLED_PROPERTY, false)

                val showAlert = when {
                    !isEnabled && !previouslyEnabled -> false
                    isEnabled && previouslyEnabled -> false
                    else -> true
                }

                val alertType = if (!isEnabled && previouslyEnabled) {
                    CheckFeedbackResultModel.ALERT_TYPE_FEEDBACK_DISABLED
                } else {
                    CheckFeedbackResultModel.ALERT_TYPE_HOW_TO_GIVE_FEEDBACK
                }

                sharedPreferences.edit {
                    putBoolean(IS_FEEDBACK_ENABLED_PROPERTY, isEnabled)
                }

                CheckFeedbackResultModel(showAlert, alertType, isEnabled)
            }
    }
}
