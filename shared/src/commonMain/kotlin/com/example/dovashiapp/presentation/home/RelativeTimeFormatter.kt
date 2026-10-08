package com.example.dovashiapp.presentation.home

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod

private val monthAbbreviations =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatRelativeTime(then: Instant, now: Instant, zone: TimeZone): String {
    val elapsed = now - then
    return when {
        elapsed < 1.minutes -> "Just now"
        elapsed < 1.hours -> "${elapsed.inWholeMinutes} min ago"
        elapsed < 1.days -> {
            val hours = elapsed.inWholeHours
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        else -> {
            val thenDate = then.toLocalDateTime(zone).date
            val today = now.toLocalDateTime(zone).date
            if (thenDate == today.minus(DatePeriod(days = 1))) {
                "Yesterday"
            } else {
                "${monthAbbreviations[thenDate.month.ordinal]} ${thenDate.day}"
            }
        }
    }
}
