package com.appswithlove.updraft.feedback.form

class FeedbackChoice(
    val id: Long,
    val name: String,
    val isHiddenInDropdown: Boolean,
) {

    fun id(): Long {
        return id
    }

    fun name(): String {
        return name
    }

    fun getIsHiddenInDropdown(): Boolean {
        return isHiddenInDropdown
    }

    fun apiName(): String? = when (id) {
        FEEDBACK_TYPE_DESIGN -> FEEDBACK_TYPE_DESIGN_API_STRING
        FEEDBACK_TYPE_FEEDBACK -> FEEDBACK_TYPE_FEEDBACK_API_STRING
        FEEDBACK_TYPE_BUG -> FEEDBACK_TYPE_BUG_API_STRING
        else -> null
    }

    companion object {
        const val FEEDBACK_TYPE_NOT_SELECTED = 0L
        const val FEEDBACK_TYPE_DESIGN = 1L
        const val FEEDBACK_TYPE_FEEDBACK = 2L
        const val FEEDBACK_TYPE_BUG = 3L

        private const val FEEDBACK_TYPE_DESIGN_API_STRING = "design"
        private const val FEEDBACK_TYPE_FEEDBACK_API_STRING = "feedback"
        private const val FEEDBACK_TYPE_BUG_API_STRING = "bug"
    }
}
