package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.ui.formatGbp

/** Shown in place of the figure once a year's allowance has been used up. */
private const val ALLOWANCE_MAXED_OUT = "Allowance maxed out!"

/**
 * Immutable snapshot of everything the accounts list needs to render.
 *
 * Following the UI layer guidance, a single UI state object is exposed as one stream and loading is
 * modelled as a boolean field on it rather than as a sealed hierarchy.
 */
data class AccountsUiState(
  val accounts: List<AccountListItemUiState> = emptyList(),
  val ruleset: RulesetState = RulesetState(),
  val remainingPence: Long? = null,
  val hasAccounts: Boolean = false,
  val isLoading: Boolean = false,
)

/** The tax year the accounts view is scoped to, and whether the user can step either way. */
data class RulesetState(
  val current: String? = null,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val canReset: Boolean = false,
)

/** A single account row in the list. */
data class AccountListItemUiState(
  val productId: String,
  val name: String,
  val providerName: String,
  val providerColour: String,
  val contributionsPence: Long = 0L,
  val providerIconUrl: String? = null,
)

/** Maps a flattened data-layer row into its UI representation, carrying [contributionsPence]. */
internal fun AccountSummary.toListItemUiState(contributionsPence: Long): AccountListItemUiState =
  AccountListItemUiState(
    productId = productId,
    name = friendlyName,
    providerName = providerName,
    providerColour = providerColour,
    contributionsPence = contributionsPence,
    providerIconUrl = providerIconUrl,
  )

/**
 * Builds the list state from the account rows, their recorded [balances], the available [rulesets]
 * and the [selectedId]. Accounts not open in the selected year are dropped, and each remaining row
 * shows the contribution recorded for that year. [AccountsUiState.hasAccounts] counts every account,
 * so the empty state can tell "no accounts" apart from "none open this year".
 */
internal fun resolveAccountsUiState(
  accounts: List<AccountSummary>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  selectedId: String?,
): AccountsUiState {
  val selection = resolveRuleset(rulesets.map { it.id }, selectedId)
  val currentRuleset = selection.current.orEmpty()
  val recorded =
    balances.associate { (it.productId to it.rulesetId) to it.deductedFromAllowancePence }
  val items =
    accounts
      .filter { isOpenDuringRuleset(currentRuleset, it.startTaxYear, it.endTaxYear) }
      .map { account ->
        account.toListItemUiState(recorded[account.productId to currentRuleset] ?: 0L)
      }
  return AccountsUiState(
    accounts = items,
    ruleset = selection,
    remainingPence = remainingAllowancePence(selection.current, balances, rulesets),
    hasAccounts = accounts.isNotEmpty(),
    isLoading = false,
  )
}

/**
 * The shared allowance left for [rulesetId]: that year's shared allowance minus every contribution
 * recorded against it, matching the web. Junior ISA contributions are counted here too, which the
 * web also gets wrong — separating them out is a known follow-up.
 */
internal fun remainingAllowancePence(
  rulesetId: String?,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
): Long? {
  val allowance = rulesets.firstOrNull { it.id == rulesetId }?.sharedAllowancePence ?: return null
  val contributed =
    balances.filter { it.rulesetId == rulesetId }.sumOf { it.deductedFromAllowancePence }
  return allowance - contributed
}

/**
 * The hero headline for a year: the remaining allowance, or a nudge once it is all used up. Null
 * when there is no year to describe.
 */
internal fun allowanceHeadline(remainingPence: Long?): String? = when {
  remainingPence == null -> null
  remainingPence <= 0L -> ALLOWANCE_MAXED_OUT
  else -> "${formatGbp(remainingPence)} remaining"
}

/**
 * Resolves the selected ruleset from [selectedId], falling back to the latest one. An id that is no
 * longer present (e.g. after a seed change) also falls back to the latest.
 */
internal fun resolveRuleset(rulesetIds: List<String>, selectedId: String?): RulesetState {
  val current = selectedId?.takeIf { it in rulesetIds } ?: rulesetIds.lastOrNull()
  val index = rulesetIds.indexOf(current)
  return RulesetState(
    current = current,
    canGoBack = index > 0,
    canGoForward = index in 0 until rulesetIds.lastIndex,
    canReset = current != rulesetIds.lastOrNull(),
  )
}

/**
 * Whether a tax year [target] falls within an account's open range: the account opened in or before
 * [target] and either is still open or closed in or after [target]. A blank [target] includes all.
 */
internal fun isOpenDuringRuleset(
  target: String,
  startTaxYear: String,
  endTaxYear: String?,
): Boolean {
  val targetYear = target.taxYearStart()
  val startYear = startTaxYear.taxYearStart()
  if (targetYear == null || startYear == null) return true
  val endYear = endTaxYear?.taxYearStart()
  return startYear <= targetYear && (endYear == null || targetYear <= endYear)
}

private fun String.taxYearStart(): Int? = substringBefore('/').toIntOrNull()
