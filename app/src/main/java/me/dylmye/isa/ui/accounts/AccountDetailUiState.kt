package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.time.isoDate
import me.dylmye.isa.ui.formatLastUpdated

/** Which way an account's contributions moved against the previous tax year. */
enum class ContributionTrend {
  Up,
  Down,
  Flat,
}

/** Immutable snapshot of everything the account detail pane needs to render. */
data class AccountDetailUiState(
  val name: String? = null,
  val typeName: String? = null,
  val longDescription: String? = null,
  val contributionsPence: Long = 0L,
  val ruleset: String? = null,
  val previousRuleset: String? = null,
  val isLatestRuleset: Boolean = false,
  val trend: ContributionTrend? = null,
  /** When the selected year's figure was recorded, or null when nothing is recorded for it. */
  val lastUpdatedLabel: String? = null,
  /** Whether the selected year is the one today falls in, so money can still go into it. */
  val isCurrentTaxYear: Boolean = false,
  /** Whether this account has a monthly schedule in the running year. */
  val hasAutoContribution: Boolean = false,
)

/**
 * Builds the detail state for [product] from its [productTypes], [balances], the available
 * [rulesets] and the [selectedId] (falling back to the latest ruleset). The year-on-year
 * comparison is only present when the tax year immediately before the selected one has a recorded
 * contribution for this account, and the last-updated label is measured against [nowMillis].
 *
 * [autoContributions] says whether the selected year has a schedule, so the actions card can label
 * its button; the schedule itself can only credit the running year.
 */
internal fun resolveAccountDetail(
  product: ProductEntity?,
  productTypes: List<ProductTypeEntity>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  selectedId: String?,
  autoContributions: List<AutoContributionEntity>,
  nowMillis: Long,
): AccountDetailUiState {
  val rulesetIds = rulesets.map { it.id }
  val current = resolveRuleset(rulesetIds, selectedId).current
  val previous = rulesetIds.getOrNull(rulesetIds.indexOf(current) - 1)
  val productType = productTypes.firstOrNull { it.id == product?.productTypeId }
  val contributionsByRuleset = balances.associateBy { it.rulesetId }
  val contributionsPence = contributionsByRuleset[current]?.deductedFromAllowancePence ?: 0L
  val previousContributions =
    previous?.let { contributionsByRuleset[it]?.deductedFromAllowancePence }
  val lastUpdated =
    contributionsByRuleset[current]?.let { formatLastUpdated(it.lastUpdatedDateUnix, nowMillis) }
  val running = runningRuleset(rulesets, nowMillis)
  val isCurrent = current != null && current == running?.id
  return AccountDetailUiState(
    name = product?.friendlyName,
    typeName = productType?.name,
    longDescription = productType?.longDescription,
    contributionsPence = contributionsPence,
    ruleset = current,
    previousRuleset = previous.takeIf { previousContributions != null },
    isLatestRuleset = current != null && current == rulesetIds.lastOrNull(),
    trend = previousContributions?.let { trendOf(contributionsPence, it) },
    lastUpdatedLabel = lastUpdated,
    isCurrentTaxYear = isCurrent,
    hasAutoContribution =
      current != null &&
        autoContributions.any { it.productId == product?.id && it.rulesetId == current },
  )
}

/** The ruleset whose dates contain [nowMillis], or null once today is past the seeded range. */
private fun runningRuleset(rulesets: List<RulesetEntity>, nowMillis: Long): RulesetEntity? {
  val today = isoDate(nowMillis)
  return rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
}

/** Compares two contribution totals, reporting only the direction of the change. */
internal fun trendOf(contributionsPence: Long, previousPence: Long): ContributionTrend = when {
  contributionsPence > previousPence -> ContributionTrend.Up
  contributionsPence < previousPence -> ContributionTrend.Down
  else -> ContributionTrend.Flat
}
