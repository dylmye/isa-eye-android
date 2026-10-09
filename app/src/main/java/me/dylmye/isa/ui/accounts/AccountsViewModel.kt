package me.dylmye.isa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences

class AccountsViewModel(
  repository: AccountRepository,
  private val userPreferences: UserPreferences,
) : ViewModel() {
  val uiState: StateFlow<AccountsUiState> =
    repository
      .observeAccounts()
      .map { summaries -> AccountsUiState(accounts = summaries.map { it.toListItemUiState() }) }
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
