package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity

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
)

/**
 * Builds the detail state for [product] from its [productTypes], [balances], the available
 * [rulesetIds] and the [selectedId] (falling back to the latest ruleset). The year-on-year
 * comparison is only present when the tax year immediately before the selected one has a recorded
 * contribution for this account.
 */
internal fun resolveAccountDetail(
  product: ProductEntity?,
  productTypes: List<ProductTypeEntity>,
  balances: List<AnnualBalanceEntity>,
  rulesetIds: List<String>,
  selectedId: String?,
): AccountDetailUiState {
  val current = resolveRuleset(rulesetIds, selectedId).current
  val previous = rulesetIds.getOrNull(rulesetIds.indexOf(current) - 1)
  val productType = productTypes.firstOrNull { it.id == product?.productTypeId }
  val contributionsByRuleset = balances.associateBy { it.rulesetId }
  val contributionsPence = contributionsByRuleset[current]?.deductedFromAllowancePence ?: 0L
  val previousContributions =
    previous?.let { contributionsByRuleset[it]?.deductedFromAllowancePence }
  return AccountDetailUiState(
    name = product?.friendlyName,
    typeName = productType?.name,
    longDescription = productType?.longDescription,
    contributionsPence = contributionsPence,
    ruleset = current,
    previousRuleset = previous.takeIf { previousContributions != null },
    isLatestRuleset = current != null && current == rulesetIds.lastOrNull(),
    trend = previousContributions?.let { trendOf(contributionsPence, it) },
  )
}

/** Compares two contribution totals, reporting only the direction of the change. */
internal fun trendOf(contributionsPence: Long, previousPence: Long): ContributionTrend = when {
  contributionsPence > previousPence -> ContributionTrend.Up
  contributionsPence < previousPence -> ContributionTrend.Down
  else -> ContributionTrend.Flat
}
