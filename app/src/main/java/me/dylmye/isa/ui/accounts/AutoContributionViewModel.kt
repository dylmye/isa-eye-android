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
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.ui.sanitizePoundsInput

/**
 * State holder for the auto-contribution form. It sets or clears the monthly amount for today's tax
 * year, which is the only year credits can land in.
 */
class AutoContributionViewModel(
  private val accountRepository: AccountRepository,
  private val autoContributionRepository: AutoContributionRepository,
  private val productId: String,
) : ViewModel() {
  private val draft = MutableStateFlow(AutoContributionDraft())
  private val savedEvents = Channel<AutoContributionEvent>(Channel.BUFFERED)

  /** One-off events, e.g. the schedule was saved and the form should close. */
  val events: Flow<AutoContributionEvent> = savedEvents.receiveAsFlow()

  private val scheduleData =
    combine(
      accountRepository.observeRulesets(),
      autoContributionRepository.observeForProduct(productId),
    ) { rulesets, schedules ->
      AutoContributionData(rulesets, schedules)
    }

  val uiState: StateFlow<AutoContributionUiState> =
    combine(
      scheduleData,
      draft,
    ) { data, form ->
      resolveAutoContribution(
        rulesets = data.rulesets,
        schedules = data.schedules,
        nowMillis = System.currentTimeMillis(),
        draft = form,
      )
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AutoContributionUiState(),
      )

  fun onAmountChange(value: String) = draft.update { it.copy(input = sanitizePoundsInput(value)) }

  fun save() {
    val state = uiState.value
    val rulesetId = state.ruleset
    val monthly = state.enteredPence
    val first = state.firstCreditDate
    if (rulesetId == null || monthly == null || first == null) return
    if (!state.canSave) return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      autoContributionRepository.setSchedule(productId, rulesetId, monthly, first)
      // A schedule set up on the 1st is due straight away, and the start-up catch-up has already run
      // by the time this dialog opens, so apply it here rather than leaving it until next launch.
      autoContributionRepository.applyDue(System.currentTimeMillis())
      savedEvents.send(AutoContributionEvent.Saved)
    }
  }

  fun remove() {
    val state = uiState.value
    val rulesetId = state.ruleset ?: return
    if (!state.canRemove) return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      autoContributionRepository.clear(productId, rulesetId)
      savedEvents.send(AutoContributionEvent.Removed)
    }
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory(productId: String) = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        AutoContributionViewModel(
          application.accountRepository,
          application.autoContributionRepository,
          productId,
        )
      }
    }
  }
}

private data class AutoContributionData(
  val rulesets: List<RulesetEntity>,
  val schedules: List<AutoContributionEntity>,
)

sealed interface AutoContributionEvent {
  /** A schedule was saved, so the monthly reminder is now worth asking about. */
  data object Saved : AutoContributionEvent

  /** The schedule was turned off. */
  data object Removed : AutoContributionEvent
}
