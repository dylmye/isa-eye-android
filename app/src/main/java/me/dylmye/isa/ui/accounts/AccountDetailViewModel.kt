package me.dylmye.isa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences

/**
 * Loads the selected account and its contributions for the detail pane. The tax year comes from the
 * same preference the accounts list is scoped to, so the pane stays in step as the year changes.
 */
class AccountDetailViewModel(
  repository: AccountRepository,
  userPreferences: UserPreferences,
  productId: String,
) : ViewModel() {
  val uiState: StateFlow<AccountDetailUiState> =
    combine(
      repository.observeProduct(productId),
      repository.observeProductTypes(),
      repository.observeBalances(productId),
      repository.observeRulesets(),
      userPreferences.currentRulesetId,
    ) { product, productTypes, balances, rulesets, selectedId ->
      resolveAccountDetail(
        product,
        productTypes,
        balances,
        rulesets.map { it.id },
        selectedId,
        System.currentTimeMillis(),
      )
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AccountDetailUiState(),
      )

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory(productId: String) = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        AccountDetailViewModel(
          application.accountRepository,
          application.userPreferences,
          productId,
        )
      }
    }
  }
}
