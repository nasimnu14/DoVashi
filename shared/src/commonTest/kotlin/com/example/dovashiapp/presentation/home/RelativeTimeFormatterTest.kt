package com.example.dovashiapp.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class RelativeTimeFormatterTest {
    private val utc = TimeZone.UTC
    private val now = Instant.parse("2026-10-08T12:00:00Z")

    private fun format(then: String, now: Instant = this.now, zone: TimeZone = utc) =
        formatRelativeTime(Instant.parse(then), now, zone)

    @Test
    fun underOneMinuteIsJustNow() {
        assertEquals("Just now", format("2026-10-08T11:59:30Z"))
        assertEquals("Just now", format("2026-10-08T11:59:01Z"))
    }

    @Test
    fun minuteBoundaries() {
        assertEquals("1 min ago", format("2026-10-08T11:59:00Z"))
        assertEquals("5 min ago", format("2026-10-08T11:55:00Z"))
        assertEquals("59 min ago", format("2026-10-08T11:01:00Z"))
    }

    @Test
    fun hourBoundaries() {
        assertEquals("1 hour ago", format("2026-10-08T11:00:00Z"))
        assertEquals("3 hours ago", format("2026-10-08T09:00:00Z"))
        assertEquals("23 hours ago", format("2026-10-07T12:01:00Z"))
    }

    @Test
    fun previousCalendarDayIsYesterday() {
        assertEquals("Yesterday", format("2026-10-07T12:00:00Z"))
        assertEquals("Yesterday", format("2026-10-07T10:00:00Z"))
    }

    @Test
    fun olderDatesUseShortDate() {
        assertEquals("Sep 28", format("2026-09-28T09:00:00Z"))
        assertEquals("Jan 5", format("2026-01-05T09:00:00Z"))
    }

    @Test
    fun hoursBucketWinsOverYesterdayWithinTwentyFourHours() {
        val earlyMorning = Instant.parse("2026-10-08T01:00:00Z")
        assertEquals("1 hour ago", format("2026-10-07T23:30:00Z", earlyMorning))
        assertEquals("5 hours ago", format("2026-10-07T20:00:00Z", earlyMorning))
        assertEquals("Oct 6", format("2026-10-06T22:00:00Z", earlyMorning))
    }

    @Test
    fun calendarDayIsDecidedInTheGivenTimeZone() {
        val later = Instant.parse("2026-10-09T12:00:00Z")
        assertEquals("Oct 7", format("2026-10-07T23:00:00Z", later, utc))
        assertEquals("Yesterday", format("2026-10-07T23:00:00Z", later, TimeZone.of("Australia/Brisbane")))
    }

    @Test
    fun yesterdayAcrossNewYear() {
        val newYear = Instant.parse("2026-01-01T12:00:00Z")
        assertEquals("Yesterday", format("2025-12-31T09:00:00Z", newYear))
        assertEquals("Dec 30", format("2025-12-30T09:00:00Z", newYear))
    }

    @Test
    fun futureTimestampDoesNotCrashAndReadsJustNow() {
        assertEquals("Just now", format("2026-10-08T13:00:00Z"))
    }
}
