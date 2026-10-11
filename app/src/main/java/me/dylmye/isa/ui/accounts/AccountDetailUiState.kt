package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity

/** Which way an account's balance moved against the previous tax year. */
enum class BalanceTrend {
  Up,
  Down,
  Flat,
}

/** Immutable snapshot of everything the account detail pane needs to render. */
data class AccountDetailUiState(
  val name: String? = null,
  val balancePence: Long = 0L,
  val ruleset: String? = null,
  val previousRuleset: String? = null,
  val trend: BalanceTrend? = null,
)

/**
 * Builds the detail state for [product] from its [balances], the available [rulesetIds] and the
 * [selectedId] (falling back to the latest ruleset). The year-on-year comparison is only present
 * when the tax year immediately before the selected one has a recorded balance for this account.
 */
internal fun resolveAccountDetail(
  product: ProductEntity?,
  balances: List<AnnualBalanceEntity>,
  rulesetIds: List<String>,
  selectedId: String?,
): AccountDetailUiState {
  val current = resolveRuleset(rulesetIds, selectedId).current
  val previous = rulesetIds.getOrNull(rulesetIds.indexOf(current) - 1)
  val balancesByRuleset = balances.associateBy { it.rulesetId }
  val balancePence = balancesByRuleset[current]?.deductedFromAllowancePence ?: 0L
  val previousBalance = previous?.let { balancesByRuleset[it]?.deductedFromAllowancePence }
  return AccountDetailUiState(
    name = product?.friendlyName,
    balancePence = balancePence,
    ruleset = current,
    previousRuleset = previous.takeIf { previousBalance != null },
    trend = previousBalance?.let { trendOf(balancePence, it) },
  )
}

/** Compares two balances, reporting only the direction of the change. */
internal fun trendOf(balancePence: Long, previousBalancePence: Long): BalanceTrend = when {
  balancePence > previousBalancePence -> BalanceTrend.Up
  balancePence < previousBalancePence -> BalanceTrend.Down
  else -> BalanceTrend.Flat
}
