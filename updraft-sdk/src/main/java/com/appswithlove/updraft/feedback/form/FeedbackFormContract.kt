package com.appswithlove.updraft.feedback.form

interface FeedbackFormContract {

    interface View {
        fun getSelectedChoice(): FeedbackChoice
        fun getEmail(): String
        fun getDescription(): String

        fun showProgress()
        fun hideProgress()
        fun showSuccessMessage()
        fun showErrorMessage(t: Throwable)
        fun updateProgress(progress: Double)
        fun closeFeedback()
    }
}
