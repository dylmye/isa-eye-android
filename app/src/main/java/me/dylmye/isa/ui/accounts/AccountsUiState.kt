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
  val isLoading: Boolean = false,
)

/** A single account row in the list. */
data class AccountListItemUiState(
  val productId: String,
  val name: String,
  val providerName: String,
  val providerColour: String,
  val balancePence: Long = 0L,
)

/** Maps a flattened data-layer row into its UI representation. */
internal fun AccountSummary.toListItemUiState(): AccountListItemUiState = AccountListItemUiState(
  productId = productId,
  name = friendlyName,
  providerName = providerName,
  providerColour = providerColour,
  // Placeholder until the current tax year's allowance is modelled.
  balancePence = 0L,
)
