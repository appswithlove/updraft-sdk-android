package com.appswithlove.updraft.ui.dialogs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class RelativeAgeTest {

    private val now = Instant.parse("2026-07-23T12:00:00Z")

    @Test
    fun buckets_matchLegacyBehaviour() {
        assertEquals(RelativeAge.JustNow, relativeAge(now - 5.minutes, now))
        assertEquals(RelativeAge.Hours(5), relativeAge(now - 5.hours, now))
        assertEquals(RelativeAge.Days(3), relativeAge(now - 3.days, now))
        assertEquals(RelativeAge.Weeks(1), relativeAge(now - 8.days, now))
        assertEquals(RelativeAge.Weeks(4), relativeAge(now - 29.days, now))
        assertEquals(RelativeAge.Months(1), relativeAge(now - 31.days, now))
        assertEquals(RelativeAge.Months(11), relativeAge(now - 350.days, now))
        assertEquals(RelativeAge.Years(2), relativeAge(now - 800.days, now))
    }

    @Test
    fun parseCreateAt_acceptsServerFormats() {
        assertNotNull(parseCreateAt("2026-07-22T09:31:25.785624Z"))
        assertNotNull(parseCreateAt("2026-07-22T09:31:25Z"))
        assertNotNull(parseCreateAt("2026-07-22T09:31:25+02:00"))
        assertNotNull(parseCreateAt("2026-07-22T09:31:25"))
        assertNotNull(parseCreateAt("2026-07-22 09:31:25"))
        assertNull(parseCreateAt("not a date"))
        assertNull(parseCreateAt(""))
    }
}
