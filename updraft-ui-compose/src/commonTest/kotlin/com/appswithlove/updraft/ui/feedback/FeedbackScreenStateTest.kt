package com.appswithlove.updraft.ui.feedback

import com.appswithlove.updraft.FeedbackType
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedbackScreenStateTest {

    @Test
    fun canSend_requiresTypeSelection() = runTest {
        val state = FeedbackScreenState(send = { _, _, _, _ -> flowOf() }, scope = backgroundScope)
        assertFalse(state.canSend)
        state.selectedType = FeedbackType.Bug
        assertTrue(state.canSend)
    }

    @Test
    fun sendFeedback_success_setsResult() = runTest {
        val state = FeedbackScreenState(send = { _, _, _, _ -> flowOf(0.5, 1.0) }, scope = backgroundScope)
        state.selectedType = FeedbackType.Feedback
        state.sendFeedback(byteArrayOf(1))
        advanceUntilIdle()
        assertNotNull(state.result)
        assertTrue(state.result!!.isSuccess)
    }

    @Test
    fun sendFeedback_failure_setsFailureResult() = runTest {
        val state = FeedbackScreenState(
            send = { _, _, _, _ -> flow { throw IllegalStateException("boom") } },
            scope = backgroundScope,
        )
        state.selectedType = FeedbackType.Bug
        state.sendFeedback(byteArrayOf(1))
        advanceUntilIdle()
        assertTrue(state.result!!.isFailure)
        assertEquals("boom", state.result!!.exceptionOrNull()!!.message)
    }
}
