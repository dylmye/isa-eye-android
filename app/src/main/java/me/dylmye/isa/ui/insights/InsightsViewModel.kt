package me.dylmye.isa.ui.insights

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

/**
 * Builds the insights tab from every account, product type, contribution and ruleset. It reads no
 * user preference: the tab always reports today's tax year, never the one the accounts view is
 * scoped to, because money can only go into the year that is running.
 */
class InsightsViewModel(repository: AccountRepository) : ViewModel() {
  val uiState: StateFlow<InsightsUiState> =
    combine(
      repository.observeAccounts(),
      repository.observeProductTypes(),
      repository.observeBalances(),
      repository.observeRulesets(),
    ) { accounts, productTypes, balances, rulesets ->
      resolveInsights(accounts, productTypes, balances, rulesets, System.currentTimeMillis())
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        InsightsUiState(),
      )

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory() = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        InsightsViewModel(application.accountRepository)
      }
    }
  }
}
