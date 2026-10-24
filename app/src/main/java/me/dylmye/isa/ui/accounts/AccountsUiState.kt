package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AccountOrderEntity
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
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
  /**
   * The figure the hero leads with: the shared allowance, or the Junior one when that is all the
   * selected year holds.
   */
  val remainingPence: Long? = null,
  /** True when [remainingPence] is the Junior allowance rather than the shared one. */
  val heroIsJunior: Boolean = false,
  /** Junior ISA accounts open in the selected year, each with its own allowance use. */
  val juniorAllowances: List<JuniorAllowanceUiState> = emptyList(),
  val notes: RulesetNotesUiState = RulesetNotesUiState(),
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
 * so the empty state can tell "no accounts" apart from "none open this year". The selected year's
 * notes are resolved from [exceptions] and the [productTypes] that name them, and [orders] puts the
 * accounts in the order the user dragged them into.
 */
internal fun resolveAccountsUiState(
  accounts: List<AccountSummary>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  selectedId: String?,
  exceptions: List<RulesetExceptionEntity> = emptyList(),
  productTypes: List<ProductTypeEntity> = emptyList(),
  orders: List<AccountOrderEntity> = emptyList(),
): AccountsUiState {
  val selection = resolveRuleset(rulesets.map { it.id }, selectedId)
  val currentRuleset = selection.current.orEmpty()
  val recorded =
    balances.associate { (it.productId to it.rulesetId) to it.deductedFromAllowancePence }
  val open =
    accounts
      .filter { isOpenDuringRuleset(currentRuleset, it.startTaxYear, it.endTaxYear) }
      .map { account ->
        account.toListItemUiState(recorded[account.productId to currentRuleset] ?: 0L)
      }
  val items = orderAccountsForRuleset(open, orders, currentRuleset)
  val allowances =
    resolveAllowanceStanding(
      rulesetId = currentRuleset,
      accounts = accounts,
      balances = balances,
      rulesets = rulesets,
      exceptions = exceptions,
      productTypes = productTypes,
    )
  return AccountsUiState(
    accounts = items,
    ruleset = selection,
    remainingPence = allowances.headlinePence,
    heroIsJunior = allowances.headlineIsJunior,
    juniorAllowances = allowances.juniorAllowances,
    notes = resolveRulesetNotes(selection.current, rulesets, exceptions, productTypes),
    hasAccounts = accounts.isNotEmpty(),
    isLoading = false,
  )
}

/**
 * Puts [accounts] in the order saved for [rulesetId]. Accounts with no saved place — a freshly
 * added account, or a year nobody has reordered yet — follow the placed ones in name order, so an
 * addition joins at the bottom without disturbing the rest.
 */
internal fun orderAccountsForRuleset(
  accounts: List<AccountListItemUiState>,
  orders: List<AccountOrderEntity>,
  rulesetId: String,
): List<AccountListItemUiState> {
  val positions =
    orders.filter { it.rulesetId == rulesetId }.associate { it.productId to it.position }
  return accounts.sortedWith(
    compareBy<AccountListItemUiState> { positions[it.productId] ?: Int.MAX_VALUE }
      .thenBy { it.name },
  )
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
