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
import me.dylmye.isa.data.GoalRepository
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/**
 * Builds the insights tab from every account, product type, contribution, ruleset and goal. It
 * reads no user preference: the tab always reports today's tax year, never the one the accounts view
 * is scoped to, because money can only go into the year that is running.
 */
class InsightsViewModel(accountRepository: AccountRepository, goalRepository: GoalRepository) :
  ViewModel() {
  /** The reference data the cards need to name and size a Junior allowance. */
  private val reference =
    combine(
      accountRepository.observeProductTypes(),
      accountRepository.observeRulesetExceptions(),
    ) { productTypes, exceptions ->
      ReferenceData(productTypes, exceptions)
    }

  val uiState: StateFlow<InsightsUiState> =
    combine(
      accountRepository.observeAccounts(),
      reference,
      accountRepository.observeBalances(),
      accountRepository.observeRulesets(),
      goalRepository.observeGoals(),
    ) { accounts, data, balances, rulesets, goals ->
      resolveInsights(
        accounts = accounts,
        productTypes = data.productTypes,
        balances = balances,
        rulesets = rulesets,
        exceptions = data.exceptions,
        goals = goals,
        nowMillis = System.currentTimeMillis(),
      )
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
        InsightsViewModel(application.accountRepository, application.goalRepository)
      }
    }
  }
}

private data class ReferenceData(
  val productTypes: List<ProductTypeEntity>,
  val exceptions: List<RulesetExceptionEntity>,
)
