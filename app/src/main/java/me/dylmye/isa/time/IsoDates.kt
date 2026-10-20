package me.dylmye.isa.time

import java.text.ParsePosition
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
internal val UkZone: TimeZone = TimeZone.getTimeZone("Europe/London")

/** One whole day in milliseconds, shared with the relative-time labels. */
internal const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** [unixMillis] as `yyyy-MM-dd` in the UK zone, the form the seeded rulesets use. */
internal fun isoDate(unixMillis: Long): String =
  SimpleDateFormat(ISO_DATE, Locale.UK).apply { timeZone = UkZone }.format(Date(unixMillis))

/** [iso] as "1 November 2026" in the UK zone, or null when it cannot be read. */
internal fun formatLongDate(iso: String): String? = parseIsoDate(iso)?.let { date ->
  SimpleDateFormat("d MMMM yyyy", Locale.UK).apply { timeZone = UkZone }.format(date)
}

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

/** The first day of the month after [iso], or null when it cannot be read. */
internal fun firstOfNextMonth(iso: String): String? {
  val date = parseIsoDate(iso) ?: return null
  return isoDate(
    calendar(date.time).apply {
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MONTH, 1)
    }.timeInMillis,
  )
}

/**
 * Whole calendar months from [fromIso] to [toIso], rounded up, or null when either cannot be read.
 * A part-month counts as a whole one, because a saver still has that month to put money aside.
 */
internal fun monthsBetweenCeiling(fromIso: String, toIso: String): Int? {
  val from = parseIsoDate(fromIso)?.let(::atMidday)
  val to = parseIsoDate(toIso)?.let(::atMidday)
  return when {
    from == null || to == null -> {
      null
    }

    to <= from -> {
      0
    }

    else -> {
      val whole = wholeMonthsBetween(from, to)
      val shifted = calendar(from).apply { add(Calendar.MONTH, whole.toInt()) }.timeInMillis
      if (shifted < to) (whole + 1).toInt() else whole.toInt()
    }
  }
}

/** Whole calendar months between two instants, discarding any part-month remainder. */
internal fun wholeMonthsBetween(unixMillis: Long, nowMillis: Long): Long {
  val from = calendar(unixMillis)
  val to = calendar(nowMillis)
  val months =
    (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12L +
      (to.get(Calendar.MONTH) - from.get(Calendar.MONTH))
  // A month only counts once its day-of-month has come round, so 18 Jan to 17 Feb is not one yet.
  val dayNotReached = to.get(Calendar.DAY_OF_MONTH) < from.get(Calendar.DAY_OF_MONTH)
  return if (dayNotReached) months - 1 else months
}

internal fun calendar(unixMillis: Long): Calendar =
  Calendar.getInstance(UkZone).apply { timeInMillis = unixMillis }

/** Midnight is unsafe to subtract across a daylight-saving change, so both ends are pulled to noon. */
private fun atMidday(date: Date): Long = calendar(date.time).apply {
  set(Calendar.HOUR_OF_DAY, 12)
  set(Calendar.MINUTE, 0)
}.timeInMillis

private fun parseIsoDate(iso: String): Date? {
  val format = SimpleDateFormat(ISO_DATE, Locale.UK).apply {
    timeZone = UkZone
    isLenient = false
  }
  // Parsed through a position so a string that is not a date returns null rather than throwing, and
  // the whole string has to be consumed so trailing junk cannot pass as one.
  val position = ParsePosition(0)
  val date = format.parse(iso, position)
  return date?.takeIf { position.index == iso.length }
}

private const val ISO_DATE = "yyyy-MM-dd"
