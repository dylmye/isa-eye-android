package me.dylmye.isa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

class AccountsViewModel(
  private val repository: AccountRepository,
  private val userPreferences: UserPreferences,
) : ViewModel() {
  private val rulesets = MutableStateFlow<List<RulesetEntity>>(emptyList())

  /** Exceptions and the product types that name them, paired to keep the view's flow count down. */
  private val allowanceData =
    combine(
      repository.observeRulesetExceptions(),
      repository.observeProductTypes(),
    ) { exceptions, productTypes ->
      AllowanceData(exceptions, productTypes)
    }

  val uiState: StateFlow<AccountsUiState> =
    combine(
      repository.observeAccounts(),
      repository.observeBalances(),
      rulesets,
      allowanceData,
      userPreferences.currentRulesetId,
    ) { accounts, balances, availableRulesets, allowance, selectedId ->
      resolveAccountsUiState(
        accounts = accounts,
        balances = balances,
        rulesets = availableRulesets,
        selectedId = selectedId,
        exceptions = allowance.exceptions,
        productTypes = allowance.productTypes,
      )
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AccountsUiState(isLoading = true),
      )

  init {
    viewModelScope.launch {
      repository.observeRulesets().collect { rulesets.value = it }
    }
  }

  fun onPreviousRuleset() = shiftRuleset(-1)

  fun onNextRuleset() = shiftRuleset(1)

  fun onResetRuleset() = userPreferences.setCurrentRulesetId(null)

  private fun shiftRuleset(delta: Int) {
    val ids = rulesets.value.map { it.id }
    val selected = resolveRuleset(ids, userPreferences.currentRulesetId.value).current
    val target = ids.getOrNull(ids.indexOf(selected) + delta) ?: return
    userPreferences.setCurrentRulesetId(target)
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    val Factory = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        AccountsViewModel(application.accountRepository, application.userPreferences)
      }
    }
  }
}

private data class AllowanceData(
  val exceptions: List<RulesetExceptionEntity>,
  val productTypes: List<ProductTypeEntity>,
)
