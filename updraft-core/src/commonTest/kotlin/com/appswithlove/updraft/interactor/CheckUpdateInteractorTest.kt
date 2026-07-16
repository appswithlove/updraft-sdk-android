package com.appswithlove.updraft.interactor

import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.api.UpdraftApiContract
import com.appswithlove.updraft.api.response.CheckLastVersionResponse
import com.appswithlove.updraft.api.response.GetLastVersionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeUpdateApi(
    var checkResponse: CheckLastVersionResponse,
    var lastVersionResponse: GetLastVersionResponse = GetLastVersionResponse(updateUrl = null),
    var feedbackEnabled: Boolean = true,
) : UpdraftApiContract {
    override suspend fun checkLastVersion() = checkResponse
    override suspend fun getLastVersion() = lastVersionResponse
    override suspend fun isFeedbackEnabled() = feedbackEnabled
    override fun sendFeedback(screenshotPng: ByteArray, type: FeedbackType, description: String, email: String): Flow<Double> = emptyFlow()
}

class CheckUpdateInteractorTest {

    @Test
    fun newVersionWithUrl_showsAlert() = runTest {
        val api = FakeUpdateApi(
            CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = true, version = "5", yourVersion = "4", createAt = "2026-01-01T00:00:00Z"),
            GetLastVersionResponse(updateUrl = "https://dl.example/app"),
        )
        val result = CheckUpdateInteractor(api).checkUpdate()
        assertTrue(result.showAlert)
        assertEquals("https://dl.example/app", result.url)
        assertEquals("5", result.version)
        assertEquals("4", result.yourVersion)
    }

    @Test
    fun newVersionWithoutUrl_noAlert() = runTest {
        val api = FakeUpdateApi(
            CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = true),
            GetLastVersionResponse(updateUrl = null),
        )
        assertFalse(CheckUpdateInteractor(api).checkUpdate().showAlert)
    }

    @Test
    fun autoupdateDisabled_noAlert() = runTest {
        val api = FakeUpdateApi(CheckLastVersionResponse(isNewVersion = true, isAutoupdateEnabled = false))
        assertFalse(CheckUpdateInteractor(api).checkUpdate().showAlert)
    }
}
