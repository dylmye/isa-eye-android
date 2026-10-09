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
  val isLoading: Boolean = false,
)

/** The tax year the accounts view is scoped to, and whether the user can step either way. */
data class RulesetState(
  val current: String? = null,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
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
  )
}
