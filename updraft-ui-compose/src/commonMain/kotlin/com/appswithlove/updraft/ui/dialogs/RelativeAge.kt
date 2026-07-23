package com.appswithlove.updraft.ui.dialogs

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal sealed interface RelativeAge {
    data object JustNow : RelativeAge
    data class Hours(val count: Int) : RelativeAge
    data class Days(val count: Int) : RelativeAge
    data class Weeks(val count: Int) : RelativeAge
    data class Months(val count: Int) : RelativeAge
    data class Years(val count: Int) : RelativeAge
}

@OptIn(ExperimentalTime::class)
internal fun parseCreateAt(createAt: String): Instant? {
    val candidates = listOf(
        createAt,
        createAt.replace(" ", "T"),
        createAt.replace(" ", "T") + "Z",
        createAt + "Z",
    )
    for (candidate in candidates) {
        try {
            return Instant.parse(candidate)
        } catch (_: IllegalArgumentException) {
        }
    }
    return null
}

@OptIn(ExperimentalTime::class)
internal fun relativeAge(createAt: Instant, now: Instant): RelativeAge {
    val age = now - createAt
    val days = age.inWholeDays
    return when {
        age < 1.hours -> RelativeAge.JustNow
        age < 1.days -> RelativeAge.Hours(age.inWholeHours.toInt())
        days < 7 -> RelativeAge.Days(days.toInt())
        days < 30 -> RelativeAge.Weeks((days / 7).toInt())
        days < 365 -> RelativeAge.Months((days / 30).toInt())
        else -> RelativeAge.Years((days / 365).toInt())
    }
}
