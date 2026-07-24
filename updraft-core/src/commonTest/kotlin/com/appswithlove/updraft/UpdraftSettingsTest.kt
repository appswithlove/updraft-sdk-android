package com.appswithlove.updraft

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdraftSettingsTest {
    private fun settings(logLevel: LogLevel) =
        UpdraftSettings(appKey = "a", sdkKey = "s", logLevel = logLevel)

    @Test
    fun shouldShowErrors_trueForErrorAndDebug() {
        assertTrue(settings(LogLevel.Error).shouldShowErrors())
        assertTrue(settings(LogLevel.Debug).shouldShowErrors())
        assertFalse(settings(LogLevel.None).shouldShowErrors())
    }

    @Test
    fun defaults_matchLegacySdk() {
        val s = UpdraftSettings(appKey = "a", sdkKey = "s")
        assertEquals("https://app.getupdraft.com/api/", s.baseUrl)
        assertEquals(LogLevel.Error, s.logLevel)
        assertTrue(s.feedbackEnabled)
        assertFalse(s.storeRelease)
    }
}
