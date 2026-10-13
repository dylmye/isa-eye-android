package me.dylmye.isa.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.ui.sanitizePoundsInput

/**
 * State holder for the update-contributions form. It records one tax year's contribution total,
 * opening on the year the accounts view is already scoped to.
 */
class UpdateContributionsViewModel(
  private val repository: AccountRepository,
  userPreferences: UserPreferences,
  private val productId: String,
) : ViewModel() {
  private val draft = MutableStateFlow(UpdateContributionsDraft())
  private val savedEvents = Channel<UpdateContributionsEvent>(Channel.BUFFERED)

  /** One-off events, e.g. the contribution was saved and the form should close. */
  val events: Flow<UpdateContributionsEvent> = savedEvents.receiveAsFlow()

  val uiState: StateFlow<UpdateContributionsUiState> =
    combine(
      repository.observeProduct(productId),
      repository.observeBalances(productId),
      repository.observeRulesets(),
      userPreferences.currentRulesetId,
      draft,
    ) { product, balances, rulesets, currentRulesetId, form ->
      resolveUpdateContributions(product, balances, rulesets.map { it.id }, currentRulesetId, form)
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        UpdateContributionsUiState(),
      )

  /** Choosing a year clears the amount so it follows that year's recorded figure. */
  fun onYearSelected(id: String) = draft.update { it.copy(yearId = id, amountInput = null) }

  fun onAmountChange(value: String) =
    draft.update { it.copy(amountInput = sanitizePoundsInput(value)) }

  fun save() {
    val state = uiState.value
    val year = state.selectedYearId ?: return
    val pence = state.amountPence ?: return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.upsertBalance(
        AnnualBalanceEntity(
          productId = productId,
          rulesetId = year,
          lastUpdatedDateUnix = System.currentTimeMillis(),
          deductedFromAllowancePence = pence,
        ),
      )
      savedEvents.send(UpdateContributionsEvent.Saved)
    }
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory(productId: String) = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        UpdateContributionsViewModel(
          application.accountRepository,
          application.userPreferences,
          productId,
        )
      }
    }
  }
}

sealed interface UpdateContributionsEvent {
  data object Saved : UpdateContributionsEvent
}
