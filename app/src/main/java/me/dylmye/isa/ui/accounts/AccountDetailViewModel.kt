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

/** Everything the account detail pane needs to render. */
data class AccountDetailUiState(val name: String? = null)

/** Loads the selected account for the detail pane (currently just its display name). */
class AccountDetailViewModel(repository: AccountRepository, productId: String) : ViewModel() {
  val uiState: StateFlow<AccountDetailUiState> =
    repository
      .observeProduct(productId)
      .map { product -> AccountDetailUiState(name = product?.friendlyName) }
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
        AccountDetailViewModel(application.accountRepository, productId)
      }
    }
  }
}
