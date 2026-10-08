package me.dylmye.isa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository

class AccountsViewModel(repository: AccountRepository) : ViewModel() {
  val uiState: StateFlow<AccountsUiState> =
    repository
      .observeAccounts()
      .map { summaries -> AccountsUiState(accounts = summaries.map { it.toListItemUiState() }) }
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
        AccountsViewModel(application.accountRepository)
      }
    }
  }
}
