package me.dylmye.isa.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DatesTest {
  private val now = at("2026-03-15 12:00")

  @Test
  fun formatLastUpdated_saysJustNowWithinTheFirstMinute() {
    assertEquals("just now", formatLastUpdated(now - 30_000L, now))
  }

  @Test
  fun formatLastUpdated_treatsATimestampInTheFutureAsJustNow() {
    assertEquals("just now", formatLastUpdated(now + 60_000L, now))
  }

  @Test
  fun formatLastUpdated_countsWholeMinutes() {
    assertEquals("1 minute ago", formatLastUpdated(now - 60_000L, now))
    assertEquals("5 minutes ago", formatLastUpdated(now - 5 * 60_000L, now))
  }

  @Test
  fun formatLastUpdated_countsWholeHours() {
    assertEquals("1 hour ago", formatLastUpdated(at("2026-03-15 11:00"), now))
    assertEquals("3 hours ago", formatLastUpdated(at("2026-03-15 09:00"), now))
  }

  @Test
  fun formatLastUpdated_countsWholeDays() {
    assertEquals("3 days ago", formatLastUpdated(at("2026-03-12 12:00"), now))
  }

  @Test
  fun formatLastUpdated_countsWholeWeeks() {
    assertEquals("2 weeks ago", formatLastUpdated(at("2026-02-23 12:00"), now))
  }

  @Test
  fun formatLastUpdated_switchesToMonthsAtFiveWeeks() {
    assertEquals("1 month ago", formatLastUpdated(at("2026-02-08 12:00"), now))
  }

  @Test
  fun formatLastUpdated_countsWholeMonths() {
    assertEquals("3 months ago", formatLastUpdated(at("2025-12-15 12:00"), now))
  }

  @Test
  fun formatLastUpdated_stillCountsMonthsOnTheAnniversaryItself() {
    assertEquals("12 months ago", formatLastUpdated(at("2025-03-15 12:00"), now))
  }

  @Test
  fun formatLastUpdated_showsTheDateOnceOverAYearOld() {
    assertEquals("4 Jan 2025", formatLastUpdated(at("2025-01-04 12:00"), now))
  }

  @Test
  fun formatLastUpdated_showsADateWithoutATime() {
    assertEquals("4 Jan 2025", formatLastUpdated(at("2025-01-04 23:30"), now))
  }
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

/** Parses a UK wall-clock instant so the expectations above read as dates rather than millis. */
private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
