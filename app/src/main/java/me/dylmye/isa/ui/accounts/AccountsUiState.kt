package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary

/**
 * Immutable snapshot of everything the accounts list needs to render.
 *
 * Following the UI layer guidance, a single UI state object is exposed as one stream and loading is
 * modelled as a boolean field on it rather than as a sealed hierarchy.
 */
data class AccountsUiState(
  val accounts: List<AccountListItemUiState> = emptyList(),
  val ruleset: RulesetState = RulesetState(),
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
  val balancePence: Long = 0L,
  val providerIconUrl: String? = null,
)

/** Maps a flattened data-layer row into its UI representation. */
internal fun AccountSummary.toListItemUiState(): AccountListItemUiState = AccountListItemUiState(
  productId = productId,
  name = friendlyName,
  providerName = providerName,
  providerColour = providerColour,
  // Placeholder until the current tax year's allowance is modelled.
  balancePence = 0L,
  providerIconUrl = providerIconUrl,
)

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
