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
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.RulesetEntity

/**
 * Loads the selected account and its contributions for the detail pane. The tax year comes from the
 * same preference the accounts list is scoped to, so the pane stays in step as the year changes.
 */
class AccountDetailViewModel(
  accountRepository: AccountRepository,
  autoContributionRepository: AutoContributionRepository,
  userPreferences: UserPreferences,
  productId: String,
) : ViewModel() {
  /** Rulesets and this account's schedule, combined to keep the form's flow count down. */
  private val scheduleData =
    combine(
      accountRepository.observeRulesets(),
      autoContributionRepository.observeForProduct(productId),
    ) { rulesets, schedules ->
      ScheduleData(rulesets, schedules)
    }

  val uiState: StateFlow<AccountDetailUiState> =
    combine(
      accountRepository.observeProduct(productId),
      accountRepository.observeProductTypes(),
      accountRepository.observeBalances(productId),
      scheduleData,
      userPreferences.currentRulesetId,
    ) { product, productTypes, balances, data, selectedId ->
      resolveAccountDetail(
        product = product,
        productTypes = productTypes,
        balances = balances,
        rulesets = data.rulesets,
        selectedId = selectedId,
        autoContributions = data.schedules,
        nowMillis = System.currentTimeMillis(),
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
          application.autoContributionRepository,
          application.userPreferences,
          productId,
        )
      }
    }
  }
}

private data class ScheduleData(
  val rulesets: List<RulesetEntity>,
  val schedules: List<AutoContributionEntity>,
)
