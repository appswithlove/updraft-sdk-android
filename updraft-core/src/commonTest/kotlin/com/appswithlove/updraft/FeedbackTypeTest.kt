package com.appswithlove.updraft

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackTypeTest {
    @Test
    fun apiNames_matchServerContract() {
        assertEquals("design", FeedbackType.Design.apiName)
        assertEquals("feedback", FeedbackType.Feedback.apiName)
        assertEquals("bug", FeedbackType.Bug.apiName)
    }
}
