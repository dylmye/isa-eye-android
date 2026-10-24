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
import me.dylmye.isa.data.AccountOrderRepository
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.entity.AccountOrderEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

class AccountsViewModel(
  private val repository: AccountRepository,
  private val userPreferences: UserPreferences,
  private val accountOrderRepository: AccountOrderRepository,
) : ViewModel() {
  private val rulesets = MutableStateFlow<List<RulesetEntity>>(emptyList())

  /** The year's supporting data, combined to keep the view's flow count down. */
  private val yearData =
    combine(
      repository.observeRulesetExceptions(),
      repository.observeProductTypes(),
      accountOrderRepository.observeAll(),
    ) { exceptions, productTypes, order ->
      YearData(exceptions, productTypes, order)
    }

  val uiState: StateFlow<AccountsUiState> =
    combine(
      repository.observeAccounts(),
      repository.observeBalances(),
      rulesets,
      yearData,
      userPreferences.currentRulesetId,
    ) { accounts, balances, availableRulesets, year, selectedId ->
      resolveAccountsUiState(
        accounts = accounts,
        balances = balances,
        rulesets = availableRulesets,
        selectedId = selectedId,
        exceptions = year.exceptions,
        productTypes = year.productTypes,
        orders = year.order,
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

  /** Saves the year's list order, top to bottom as [productIds] are given. */
  fun onReorderAccounts(productIds: List<String>) {
    val rulesetId = uiState.value.ruleset.current ?: return
    viewModelScope.launch { accountOrderRepository.save(rulesetId, productIds) }
  }

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
        AccountsViewModel(
          application.accountRepository,
          application.userPreferences,
          application.accountOrderRepository,
        )
      }
    }
  }
}

private data class YearData(
  val exceptions: List<RulesetExceptionEntity>,
  val productTypes: List<ProductTypeEntity>,
  val order: List<AccountOrderEntity>,
)
