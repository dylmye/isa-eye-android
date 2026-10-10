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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.entity.RulesetEntity

class AccountsViewModel(
  private val repository: AccountRepository,
  private val userPreferences: UserPreferences,
) : ViewModel() {
  private val rulesets = MutableStateFlow<List<RulesetEntity>>(emptyList())

  val uiState: StateFlow<AccountsUiState> =
    combine(
      repository.observeAccounts(),
      rulesets,
      userPreferences.currentRulesetId,
    ) { accounts, availableRulesets, selectedId ->
      val selection = resolveRuleset(availableRulesets.map { it.id }, selectedId)
      val currentRuleset = selection.current.orEmpty()
      AccountsUiState(
        accounts =
          accounts
            .filter { isOpenDuringRuleset(currentRuleset, it.startTaxYear, it.endTaxYear) }
            .map { it.toListItemUiState() },
        ruleset = selection,
        hasAccounts = accounts.isNotEmpty(),
        isLoading = false,
      )
    }
      .onEach { state ->
        // Remember that accounts exist so the next launch can open on Insights. This also heals
        // installs that predate the preference, and restored backups.
        if (state.accounts.isNotEmpty()) userPreferences.hasAccount = true
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
