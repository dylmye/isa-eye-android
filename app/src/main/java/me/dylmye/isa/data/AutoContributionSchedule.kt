package me.dylmye.isa.data

import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.time.firstOfNextMonth

/** Every credit date from [firstIso] to [endIso], one month apart and inclusive. */
internal fun creditDatesBetween(firstIso: String, endIso: String): List<String> {
  val dates = mutableListOf<String>()
  var next = firstIso
  while (next <= endIso) {
    dates += next
    next = firstOfNextMonth(next) ?: break
  }
  return dates
}

/** The credits still to apply for [schedule], up to [todayIso] and never past [endIso]. */
internal fun dueCreditDates(
  schedule: AutoContributionEntity,
  endIso: String,
  todayIso: String,
): List<String> {
  val from = schedule.appliedThroughDate?.let(::firstOfNextMonth) ?: schedule.firstCreditDate
  val until = if (todayIso < endIso) todayIso else endIso
  return if (from > until) emptyList() else creditDatesBetween(from, until)
}

/**
 * The first credit for a schedule set up on [todayIso]: today when it is the 1st, otherwise the
 * next 1st, so a month already partly spent is never credited in full.
 */
internal fun nextCreditOn(todayIso: String): String? =
  if (todayIso.endsWith("-01")) todayIso else firstOfNextMonth(todayIso)
