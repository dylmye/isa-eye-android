package me.dylmye.isa.notifications

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.time.daysBetween
import me.dylmye.isa.time.isoDate
import me.dylmye.isa.time.monthsBetweenCeiling
import me.dylmye.isa.ui.accounts.resolveAllowanceStanding
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.insights.monthlySavingPence

/**
 * Works out which reminders are due at [nowMillis], dropping any whose key is already in
 * [sentKeys]. It reads only the snapshot and the clock, so every rule below can be tested without a
 * database or a real date.
 *
 * At most one monthly reminder is planned: the contribution due on the accounts when any of them
 * has an auto-contribution this year, otherwise the goal's monthly figure. The allowance warning
 * and the year summary happen once a year and key on the tax year.
 */
internal fun planReminders(
  snapshot: ReminderSnapshot,
  nowMillis: Long,
  sentKeys: Set<String> = emptySet(),
): List<Reminder> {
  val today = isoDate(nowMillis)
  val current = snapshot.rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
  val due =
    buildList<Reminder> {
      if (current != null) {
        monthlyReminder(snapshot, current, today)?.let(::add)
        allowanceReminder(snapshot, current, today)?.let(::add)
      }
      summaryReminder(snapshot, today)?.let(::add)
    }
  return due.filterNot { it.key in sentKeys }
}

/** The first-of-the-month reminder: the contributions due, or a nudge towards the goal. */
private fun monthlyReminder(
  snapshot: ReminderSnapshot,
  year: RulesetEntity,
  today: String,
): Reminder? {
  val day = today.takeLast(2).toIntOrNull()
  if (day == null || day > MONTHLY_GRACE_DAYS) return null
  val month = today.take(MONTH_LENGTH)
  val schedules = snapshot.schedules.filter { it.rulesetId == year.id }
  return if (schedules.isEmpty()) {
    goalReminder(snapshot, year, today, month)
  } else {
    autoContributionReminder(snapshot, schedules, month)
  }
}

/** One notification naming every contribution due today, rather than one notification per account. */
private fun autoContributionReminder(
  snapshot: ReminderSnapshot,
  schedules: List<AutoContributionEntity>,
  month: String,
): Reminder? {
  val payable =
    schedules.mapNotNull { schedule ->
      val account = snapshot.accounts.firstOrNull { it.productId == schedule.productId }
      account?.let { schedule to it }
    }
  return if (payable.isEmpty()) {
    null
  } else {
    val total = payable.sumOf { it.first.monthlyPence }
    val single = payable.singleOrNull()
    Reminder(
      id = AUTO_CONTRIBUTION_ID,
      key = "auto/$month",
      title = "Remember to save",
      text =
        if (single == null) {
          "Tracking ${formatGbp(total)} across ${payable.size} accounts"
        } else {
          "Tracking ${formatGbp(total)} in your ${single.second.friendlyName} account"
        },
      // A single account needs no itemised list; the body already names it.
      lines =
        if (single == null) {
          payable.map { (schedule, account) ->
            "${account.friendlyName}: +${formatGbp(schedule.monthlyPence)}"
          }
        } else {
          emptyList()
        },
      target =
        single?.let { ReminderTarget.AccountDetails(it.second.productId) }
          ?: ReminderTarget.Accounts,
    )
  }
}

/** What to put aside to clear the year's goal, only planned when nothing is auto-contributed. */
private fun goalReminder(
  snapshot: ReminderSnapshot,
  year: RulesetEntity,
  today: String,
  month: String,
): Reminder? {
  val target = snapshot.goals.firstOrNull { it.rulesetId == year.id }?.targetPence ?: return null
  val remaining = target - contributedPence(snapshot.balances, year.id)
  val months = monthsBetweenCeiling(today, year.endDate)?.coerceAtLeast(1) ?: 1
  val monthly = monthlySavingPence(remaining, months)
  return if (remaining <= 0L || monthly <= 0L) {
    null
  } else {
    Reminder(
      id = GOAL_ID,
      key = "goal/$month",
      title = "Hit your savings goal",
      text = "Put ${formatGbp(monthly)} aside this month to hit your goal. You've got this!",
      target = ReminderTarget.Insights,
    )
  }
}

/** The once-a-year warning about allowance that is about to expire unused. */
private fun allowanceReminder(
  snapshot: ReminderSnapshot,
  year: RulesetEntity,
  today: String,
): Reminder? {
  val days = daysBetween(today, year.endDate) ?: return null
  // The same figure the accounts hero leads with: the shared pot, or the Junior one when that is
  // all the year holds. Junior contributions are kept out of the shared figure.
  val remaining =
    resolveAllowanceStanding(
      rulesetId = year.id,
      accounts = snapshot.accounts,
      balances = snapshot.balances,
      rulesets = snapshot.rulesets,
      exceptions = snapshot.exceptions,
    )
      .headlinePence
  val warn = days in 1..ALLOWANCE_WARNING_DAYS && remaining != null && remaining > 0L
  return if (warn) {
    val goalRemaining =
      snapshot.goals
        .firstOrNull { it.rulesetId == year.id }
        ?.let { it.targetPence - contributedPence(snapshot.balances, year.id) }
    Reminder(
      id = ALLOWANCE_ID,
      key = "allowance/${year.id}",
      title = "$days ${if (days == 1) "day" else "days"} left to save!",
      text =
        if (goalRemaining != null && goalRemaining > 0L) {
          "${formatGbp(goalRemaining)} left to meet your goal. Save while you can!"
        } else {
          "Make the most of your ${year.id} ISA allowance."
        },
      target = ReminderTarget.Insights,
    )
  } else {
    null
  }
}

/** The summary of the year that has just ended, within a fortnight of it closing. */
private fun summaryReminder(snapshot: ReminderSnapshot, today: String): Reminder? {
  val finished = snapshot.rulesets.lastOrNull { it.endDate < today }
  val days = finished?.let { daysBetween(it.endDate, today) }
  return if (finished != null && days != null && days <= SUMMARY_WINDOW_DAYS) {
    val total = contributedPence(snapshot.balances, finished.id)
    if (total <= 0L) {
      null
    } else {
      val accounts =
        snapshot.balances.count {
          it.rulesetId == finished.id && it.deductedFromAllowancePence > 0
        }
      val goal = snapshot.goals.firstOrNull { it.rulesetId == finished.id }?.targetPence
      Reminder(
        id = YEAR_SUMMARY_ID,
        key = "summary/${finished.id}",
        title = "${finished.id} is done",
        text = summaryText(total, accounts, goal),
        target = ReminderTarget.Insights,
      )
    }
  } else {
    null
  }
}

/** The year-end sentence: a met goal is the big cheer, everything else depends on the total. */
private fun summaryText(totalPence: Long, accounts: Int, goalPence: Long?): String {
  val saved = formatGbp(totalPence)
  val noun = if (accounts == 1) "account" else "accounts"
  val cheer = when {
    goalPence != null && totalPence >= goalPence -> " Way to go!"
    totalPence >= CELEBRATION_MINIMUM_PENCE -> " Nice one!"
    else -> ""
  }
  return when {
    goalPence != null && totalPence >= goalPence -> {
      "You put away $saved and met your ${formatGbp(goalPence)} goal.$cheer"
    }

    goalPence != null -> {
      "You put away $saved of your ${formatGbp(goalPence)} goal.$cheer"
    }

    else -> {
      "You put away $saved across $accounts $noun.$cheer"
    }
  }
}

/** What was recorded for [rulesetId] across every account. */
private fun contributedPence(balances: List<AnnualBalanceEntity>, rulesetId: String): Long =
  balances.filter { it.rulesetId == rulesetId }.sumOf { it.deductedFromAllowancePence }

/** A monthly reminder that could not run on the 1st still lands in the first three days. */
private const val MONTHLY_GRACE_DAYS = 3

/** How long before the year ends unused allowance is worth a nudge. */
private const val ALLOWANCE_WARNING_DAYS = 60

/** How long after the year ends its summary is still worth sending. */
private const val SUMMARY_WINDOW_DAYS = 14

/** The year summary only cheers once a year has reached this much: £50. */
private const val CELEBRATION_MINIMUM_PENCE = 5_000L

/** `yyyy-MM` is the first seven characters of an ISO date. */
private const val MONTH_LENGTH = 7

/** One id per reminder type, so a newer reminder of that type replaces the older one. */
private const val AUTO_CONTRIBUTION_ID = 1
private const val GOAL_ID = 2
private const val ALLOWANCE_ID = 3
private const val YEAR_SUMMARY_ID = 4
