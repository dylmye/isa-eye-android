package me.dylmye.isa.ui

import me.dylmye.isa.time.DAY_MILLIS
import me.dylmye.isa.time.UkZone
import me.dylmye.isa.time.calendar
import me.dylmye.isa.time.wholeMonthsBetween
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val WEEK_MILLIS = 7 * DAY_MILLIS

/** The last relative step: past this, a calendar month count describes the age better. */
private const val WEEKS_BEFORE_MONTHS = 5

/**
 * Describes when something was last updated against [nowMillis]: minutes, hours, days, weeks and
 * months, then the plain date once it is over a year old. The date carries no time and is UK format.
 */
internal fun formatLastUpdated(unixMillis: Long, nowMillis: Long): String {
  val elapsed = nowMillis - unixMillis
  // Anything inside a minute reads as "just now", which also covers a timestamp in the future: the
  // device clock moved back, and a negative count would read badly.
  return when {
    elapsed < MINUTE_MILLIS -> "just now"
    elapsed < HOUR_MILLIS -> ago(elapsed / MINUTE_MILLIS, "minute")
    elapsed < DAY_MILLIS -> ago(elapsed / HOUR_MILLIS, "hour")
    elapsed < WEEK_MILLIS -> ago(elapsed / DAY_MILLIS, "day")
    elapsed < WEEKS_BEFORE_MONTHS * WEEK_MILLIS -> ago(elapsed / WEEK_MILLIS, "week")
    isOverAYearOld(unixMillis, nowMillis) -> formatDate(unixMillis)
    else -> ago(wholeMonthsBetween(unixMillis, nowMillis).coerceAtLeast(1), "month")
  }
}

/** "3 days ago", with the unit left singular when the count is one. */
private fun ago(count: Long, unit: String): String =
  if (count == 1L) "1 $unit ago" else "$count ${unit}s ago"

private fun formatDate(unixMillis: Long): String =
  SimpleDateFormat("d MMM yyyy", Locale.UK).apply { timeZone = UkZone }.format(Date(unixMillis))

/** Strictly older than a calendar year, so the anniversary itself is still described relatively. */
private fun isOverAYearOld(unixMillis: Long, nowMillis: Long): Boolean {
  val aYearAgo = calendar(nowMillis).apply { add(Calendar.YEAR, -1) }
  return unixMillis < aYearAgo.timeInMillis
}
