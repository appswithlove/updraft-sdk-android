package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import com.appswithlove.updraft.platform.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeStore : KeyValueStore {
    val map = mutableMapOf<String, Boolean>()
    override fun getBoolean(key: String, default: Boolean) = map[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

private class FakeApi(var feedbackEnabled: Boolean) : UpdraftApiContract {
    override suspend fun checkLastVersion() = CheckLastVersionResponse(isNewVersion = false, isAutoupdateEnabled = false)
    override suspend fun getLastVersion() = GetLastVersionResponse()
    override suspend fun isFeedbackEnabled() = feedbackEnabled
    override fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> = emptyFlow()
}

class CheckFeedbackEnabledInteractorTest {

    @Test
    fun becameEnabled_showsHowToAlert_andPersists() = runTest {
        val store = FakeStore()
        val result = CheckFeedbackEnabledInteractor(FakeApi(true), store).run()
        assertTrue(result.showAlert)
        assertEquals(CheckFeedbackResultModel.AlertType.HowToGiveFeedback, result.alertType)
        assertTrue(store.map["is_feedback_enabled_property"]!!)
    }

    @Test
    fun becameDisabled_showsDisabledAlert() = runTest {
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        val result = CheckFeedbackEnabledInteractor(FakeApi(false), store).run()
        assertTrue(result.showAlert)
        assertEquals(CheckFeedbackResultModel.AlertType.FeedbackDisabled, result.alertType)
        assertFalse(result.isFeedbackEnabled)
    }

    @Test
    fun unchanged_noAlert() = runTest {
        val store = FakeStore().apply { map["is_feedback_enabled_property"] = true }
        assertFalse(CheckFeedbackEnabledInteractor(FakeApi(true), store).run().showAlert)
        assertFalse(CheckFeedbackEnabledInteractor(FakeApi(false), FakeStore()).run().showAlert)
    }
}
