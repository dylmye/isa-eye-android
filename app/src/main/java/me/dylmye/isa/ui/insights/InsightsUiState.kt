package me.dylmye.isa.ui.insights

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.ui.ceilToPounds
import me.dylmye.isa.ui.daysBetween
import me.dylmye.isa.ui.isoDate
import me.dylmye.isa.ui.isoDatePlusMonths
import me.dylmye.isa.ui.monthsBetweenCeiling

/** How far through the tax year it is: the countdown card colours these green, amber and red. */
enum class TaxYearStage {
  Early,
  Mid,
  Late,
}

/** One account's share of this tax year's contributions, for the by-account donut. */
data class AccountShare(
  val productId: String,
  val name: String,
  val colour: String,
  val pence: Long,
)

/** One product type's share of a year's contributions, for the by-type donut and the bar's stacks. */
data class TypeShare(
  val productTypeId: String,
  val name: String,
  val colour: String,
  val pence: Long,
)

/** One tax year's contributions split by product type, for the stacked bar chart. */
data class YearShare(
  val rulesetId: String,
  val label: String,
  val totalPence: Long,
  val byType: List<TypeShare>,
)

/**
 * This year's goal and where the recorded contributions stand against it, for the goal card and the
 * monthly figure that would clear what is left.
 */
data class GoalProgress(
  val targetPence: Long,
  val contributedPence: Long,
  val monthsRemaining: Int,
  val monthlySuggestionPence: Long,
) {
  /** What is still to be saved, or zero once the goal is passed. */
  val remainingPence: Long
    get() = (targetPence - contributedPence).coerceAtLeast(0L)

  /** How full the progress bar is, capped at one so an overshoot still draws as complete. */
  val fraction: Float
    get() = if (targetPence <=
      0L
    ) {
      0f
    } else {
      (contributedPence.toFloat() / targetPence).coerceIn(0f, 1f)
    }

  val isMet: Boolean
    get() = contributedPence >= targetPence
}

/**
 * Immutable snapshot of the insights tab, which always describes today's tax year rather than the one
 * the accounts view is scoped to. The countdown and the goal are the cards that survive an empty
 * year: both donuts need a contribution recorded this year, and the bar chart needs at least one year
 * with one.
 */
data class InsightsUiState(
  val ruleset: String? = null,
  val daysRemaining: Int? = null,
  val stage: TaxYearStage? = null,
  val currentYearTotalPence: Long = 0L,
  val accountShares: List<AccountShare> = emptyList(),
  val typeShares: List<TypeShare> = emptyList(),
  val yearShares: List<YearShare> = emptyList(),
  val goal: GoalProgress? = null,
)

/**
 * Builds the insights state from every [accounts] row, [productTypes], every [balances] row, the
 * [rulesets] and the [goals] set for them, read against [nowMillis]. The tax year comes from the
 * ruleset dates rather than the accounts view's selection, so the tab always reports current
 * performance.
 */
internal fun resolveInsights(
  accounts: List<AccountSummary>,
  productTypes: List<ProductTypeEntity>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  goals: List<GoalEntity>,
  nowMillis: Long,
): InsightsUiState {
  val today = isoDate(nowMillis)
  val current = rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
  val recorded = balances.filter { it.deductedFromAllowancePence > 0 }
  val thisYear = recorded.filter { it.rulesetId == current?.id }
  val accountShares = accountShares(thisYear, accounts)
  val currentYearTotal = accountShares.sumOf { it.pence }
  val yearShares = yearShares(recorded, accounts, productTypes, rulesets)
  return InsightsUiState(
    ruleset = current?.id,
    daysRemaining = current?.let { daysBetween(today, it.endDate) },
    stage = current?.let { stageOf(it.startDate, today) },
    currentYearTotalPence = currentYearTotal,
    accountShares = accountShares,
    typeShares = typeShares(thisYear, accounts, productTypes),
    yearShares = yearShares,
    goal = goalProgress(current, currentYearTotal, goals, today),
  )
}

/** "2026/2027" shortened to "26/27" for the bar chart's axis. */
internal fun shortRulesetLabel(rulesetId: String): String =
  rulesetId.split("/").joinToString("/") { it.takeLast(2) }

/** Green for the first six months of the year, amber for the next three, then red. */
private fun stageOf(startIso: String, todayIso: String): TaxYearStage {
  val sixMonthsIn = isoDatePlusMonths(startIso, 6)
  val nineMonthsIn = isoDatePlusMonths(startIso, 9)
  return when {
    nineMonthsIn != null && todayIso >= nineMonthsIn -> TaxYearStage.Late
    sixMonthsIn != null && todayIso >= sixMonthsIn -> TaxYearStage.Mid
    else -> TaxYearStage.Early
  }
}

/** The accounts with a contribution this year, largest first so the donut and its order are stable. */
private fun accountShares(
  balances: List<AnnualBalanceEntity>,
  accounts: List<AccountSummary>,
): List<AccountShare> = balances
  .mapNotNull { balance ->
    val account = accounts.firstOrNull { it.productId == balance.productId }
    account?.let {
      AccountShare(
        productId = it.productId,
        name = it.friendlyName,
        colour = it.providerColour,
        pence = balance.deductedFromAllowancePence,
      )
    }
  }
  .sortedWith(compareByDescending<AccountShare> { it.pence }.thenBy { it.name })

/** Contributions grouped by the account's product type, largest first. */
private fun typeShares(
  balances: List<AnnualBalanceEntity>,
  accounts: List<AccountSummary>,
  productTypes: List<ProductTypeEntity>,
): List<TypeShare> {
  val typeOf = accounts.associate { it.productId to it.productTypeId }
  val types = productTypes.associateBy { it.id }
  return balances
    .groupBy { typeOf[it.productId] }
    .mapNotNull { (typeId, rows) ->
      typeId?.let { id ->
        TypeShare(
          productTypeId = id,
          name = types[id]?.name.orEmpty(),
          colour = types[id]?.colour.orEmpty(),
          pence = rows.sumOf { row -> row.deductedFromAllowancePence },
        )
      }
    }
    .sortedWith(compareByDescending<TypeShare> { it.pence }.thenBy { it.name })
}

/** Every year with a contribution, oldest first, each split by product type. */
private fun yearShares(
  balances: List<AnnualBalanceEntity>,
  accounts: List<AccountSummary>,
  productTypes: List<ProductTypeEntity>,
  rulesets: List<RulesetEntity>,
): List<YearShare> {
  val byRuleset = balances.groupBy { it.rulesetId }
  return rulesets
    .sortedBy { it.startDate }
    .mapNotNull { ruleset ->
      val rows = byRuleset[ruleset.id] ?: return@mapNotNull null
      val byType = typeShares(rows, accounts, productTypes)
      if (byType.isEmpty()) return@mapNotNull null
      YearShare(
        rulesetId = ruleset.id,
        label = shortRulesetLabel(ruleset.id),
        totalPence = byType.sumOf { it.pence },
        byType = byType,
      )
    }
}

/** This year's goal, if one is set, with the monthly saving that would clear what is left. */
private fun goalProgress(
  ruleset: RulesetEntity?,
  contributedPence: Long,
  goals: List<GoalEntity>,
  todayIso: String,
): GoalProgress? {
  val target = ruleset?.let { year -> goals.firstOrNull { it.rulesetId == year.id }?.targetPence }
  if (ruleset == null || target == null) return null
  // The last day of the year still leaves one month to save in, so the count never reaches zero.
  val months = monthsBetweenCeiling(todayIso, ruleset.endDate)?.coerceAtLeast(1) ?: 1
  return GoalProgress(
    targetPence = target,
    contributedPence = contributedPence,
    monthsRemaining = months,
    monthlySuggestionPence = monthlySavingPence(target - contributedPence, months),
  )
}

/** What to put aside each month to clear [remainingPence] over [months], rounded up to the pound. */
internal fun monthlySavingPence(remainingPence: Long, months: Int): Long {
  if (remainingPence <= 0L || months <= 0) return 0L
  return ceilToPounds((remainingPence + months - 1) / months)
}
