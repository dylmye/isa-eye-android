package me.dylmye.isa.widgets

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import me.dylmye.isa.time.daysBetween
import me.dylmye.isa.time.formatLongDate
import me.dylmye.isa.time.isoDate
import me.dylmye.isa.time.isoDatePlusMonths
import me.dylmye.isa.ui.accounts.JuniorAllowanceUiState
import me.dylmye.isa.ui.accounts.resolveAllowanceStanding
import me.dylmye.isa.ui.insights.GoalProgress
import me.dylmye.isa.ui.insights.TaxYearStage
import me.dylmye.isa.ui.insights.resolveInsights

/**
 * Everything the four widgets draw, resolved once against the tax year that is running today.
 *
 * Widgets are passive: they never pick a year from the accounts view's selection, because a figure
 * on the home screen should describe now rather than whatever was last being browsed. When today
 * falls outside every seeded year, the year-scoped fields are null and the widgets show their empty
 * state.
 */
internal data class WidgetState(
  val hasAccounts: Boolean = false,
  val rulesetId: String? = null,
  val yearLabel: String? = null,
  val sharedAllowancePence: Long? = null,
  val sharedRemainingPence: Long? = null,
  val hasSharedAccounts: Boolean = false,
  val juniors: List<JuniorAllowanceUiState> = emptyList(),
  val goal: GoalProgress? = null,
  val contributedPence: Long = 0L,
  val startDate: String? = null,
  val endDate: String? = null,
  val endDateLabel: String? = null,
  val renewalLabel: String? = null,
  val daysRemaining: Int? = null,
  val daysElapsed: Int? = null,
  val totalDays: Int? = null,
  val stage: TaxYearStage? = null,
) {
  /** How much of the year has gone, for the cube grid's faded days. */
  val elapsedFraction: Float
    get() = fractionOf(daysElapsed, totalDays)

  /** How much of the year is left, for the tax year ring. */
  val remainingYearFraction: Float
    get() = fractionOf(daysRemaining, totalDays)

  /** How much of the shared allowance is still available. */
  val sharedRemainingFraction: Float
    get() = fractionOf(sharedRemainingPence, sharedAllowancePence)

  /** How much of the shared allowance has been used, for the card that fills as it is saved. */
  val sharedUsedFraction: Float
    get() = fractionOf(sharedContributedPence, sharedAllowancePence)

  /** True once the shared allowance has run out, or been overrun. */
  val sharedIsMaxedOut: Boolean
    get() = sharedRemainingPence != null && sharedRemainingPence <= 0L

  /** What the shared accounts have added this year. */
  val sharedContributedPence: Long?
    get() = if (sharedAllowancePence == null || sharedRemainingPence == null) {
      null
    } else {
      (sharedAllowancePence - sharedRemainingPence).coerceAtLeast(0L)
    }
}

/**
 * Reads today's standing from every [accounts] row, [productTypes], their [balances], the seeded
 * [rulesets] and [exceptions], and the [goals]. [nowMillis] is fixed by the caller so the result can
 * be tested at any point in the year.
 */
internal fun resolveWidgetState(
  accounts: List<AccountSummary>,
  productTypes: List<ProductTypeEntity>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  exceptions: List<RulesetExceptionEntity>,
  goals: List<GoalEntity>,
  nowMillis: Long,
): WidgetState {
  val today = isoDate(nowMillis)
  val current = rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
  val standing =
    resolveAllowanceStanding(
      rulesetId = current?.id,
      accounts = accounts,
      balances = balances,
      rulesets = rulesets,
      exceptions = exceptions,
      productTypes = productTypes,
    )
  val insights =
    resolveInsights(
      accounts = accounts,
      productTypes = productTypes,
      balances = balances,
      rulesets = rulesets,
      exceptions = exceptions,
      goals = goals,
      nowMillis = nowMillis,
    )
  // The tax year is a closed span: 6 April to 5 April is 365 days, or 366 when February has a 29th.
  val totalDays = current?.let { daysBetween(it.startDate, it.endDate)?.plus(1) }
  val daysRemaining = insights.daysRemaining
  return WidgetState(
    hasAccounts = accounts.isNotEmpty(),
    rulesetId = current?.id,
    yearLabel = current?.id,
    sharedAllowancePence = standing.sharedAllowancePence,
    sharedRemainingPence = standing.sharedRemainingPence,
    hasSharedAccounts = standing.hasSharedAccounts,
    juniors = standing.juniorAllowances,
    goal = insights.goal,
    contributedPence = insights.currentYearTotalPence,
    startDate = current?.startDate,
    endDate = current?.endDate,
    endDateLabel = current?.endDate?.let(::formatLongDate),
    renewalLabel =
      current?.startDate?.let { startDate ->
        isoDatePlusMonths(startDate, 12)?.let(::formatLongDate)
      },
    daysRemaining = daysRemaining,
    daysElapsed = if (totalDays != null &&
      daysRemaining != null
    ) {
      totalDays - daysRemaining
    } else {
      null
    },
    totalDays = totalDays,
    stage = insights.stage,
  )
}

/** The share [part] is of [whole], capped to 0..1 so an overshoot still draws as complete. */
private fun fractionOf(part: Long?, whole: Long?): Float {
  if (part == null || whole == null || whole <= 0L) return 0f
  return (part.toFloat() / whole).coerceIn(0f, 1f)
}

/** The day-count form of [fractionOf]. */
private fun fractionOf(part: Int?, whole: Int?): Float = fractionOf(part?.toLong(), whole?.toLong())
