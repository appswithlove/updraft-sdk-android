package com.appswithlove.updraft

sealed interface UpdraftEvent {
    data class UpdateAvailable(
        val url: String,
        val version: String?,
        val yourVersion: String?,
        val createAt: String?,
    ) : UpdraftEvent

    data object ShowFeedbackHint : UpdraftEvent
    data object FeedbackDisabled : UpdraftEvent
    data object FeedbackRequested : UpdraftEvent
    data object CloseFeedback : UpdraftEvent
    data class Error(val cause: Throwable) : UpdraftEvent
}

fun interface FeedbackUiPresenter {
    fun presentFeedback(screenshotPng: ByteArray?)
}
