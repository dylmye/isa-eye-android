package me.dylmye.isa.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * The zone dates are read in. Pinned rather than taken from the device, so one instant always renders
 * the same way and the screenshot tests do not depend on where they run.
 */
private val UkZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS
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

/** [unixMillis] as `yyyy-MM-dd` in the UK zone, the form the seeded rulesets use. */
internal fun isoDate(unixMillis: Long): String =
  SimpleDateFormat(ISO_DATE, Locale.UK).apply { timeZone = UkZone }.format(Date(unixMillis))

/**
 * Whole calendar days from [fromIso] to [toIso], or null when either cannot be read. Both ends sit at
 * midday so a daylight-saving change inside the span cannot shift the count.
 */
internal fun daysBetween(fromIso: String, toIso: String): Int? {
  val from = parseIsoDate(fromIso)?.let(::atMidday)
  val to = parseIsoDate(toIso)?.let(::atMidday)
  if (from == null || to == null) return null
  return ((to - from).toDouble() / DAY_MILLIS).roundToInt()
}

/** [iso] shifted by whole [months], as `yyyy-MM-dd`, or null when it cannot be read. */
internal fun isoDatePlusMonths(iso: String, months: Int): String? {
  val date = parseIsoDate(iso) ?: return null
  return isoDate(calendar(date.time).apply { add(Calendar.MONTH, months) }.timeInMillis)
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

/** Whole calendar months between two instants, discarding any part-month remainder. */
private fun wholeMonthsBetween(unixMillis: Long, nowMillis: Long): Long {
  val from = calendar(unixMillis)
  val to = calendar(nowMillis)
  val months =
    (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12L +
      (to.get(Calendar.MONTH) - from.get(Calendar.MONTH))
  // A month only counts once its day-of-month has come round, so 18 Jan to 17 Feb is not one yet.
  val dayNotReached = to.get(Calendar.DAY_OF_MONTH) < from.get(Calendar.DAY_OF_MONTH)
  return if (dayNotReached) months - 1 else months
}

private fun calendar(unixMillis: Long): Calendar =
  Calendar.getInstance(UkZone).apply { timeInMillis = unixMillis }

/** Midnight is unsafe to subtract across a daylight-saving change, so both ends are pulled to noon. */
private fun atMidday(date: Date): Long = calendar(date.time).apply {
  set(Calendar.HOUR_OF_DAY, 12)
  set(Calendar.MINUTE, 0)
}.timeInMillis

private fun parseIsoDate(iso: String): Date? = SimpleDateFormat(ISO_DATE, Locale.UK).apply {
  timeZone = UkZone
  isLenient = false
}.parse(iso)

private const val ISO_DATE = "yyyy-MM-dd"
