package me.dylmye.isa.ui.insights

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
import me.dylmye.isa.data.GoalRepository
import me.dylmye.isa.ui.sanitizePoundsInput

/**
 * State holder for the goal form. It sets or clears the goal for today's tax year, which is the only
 * year money can be contributed to.
 */
class SetGoalViewModel(
  private val accountRepository: AccountRepository,
  private val goalRepository: GoalRepository,
) : ViewModel() {
  private val draft = MutableStateFlow(SetGoalDraft())
  private val savedEvents = Channel<SetGoalEvent>(Channel.BUFFERED)

  /** One-off events, e.g. the goal was saved and the form should close. */
  val events: Flow<SetGoalEvent> = savedEvents.receiveAsFlow()

  val uiState: StateFlow<SetGoalUiState> =
    combine(
      accountRepository.observeRulesets(),
      goalRepository.observeGoals(),
      draft,
    ) { rulesets, goals, form ->
      resolveSetGoal(rulesets, goals, System.currentTimeMillis(), form)
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        SetGoalUiState(),
      )

  fun onAmountChange(value: String) = draft.update { it.copy(input = sanitizePoundsInput(value)) }

  fun save() {
    val state = uiState.value
    val rulesetId = state.ruleset
    val target = state.enteredPence
    if (rulesetId == null || target == null || !state.canSave) return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      goalRepository.setGoal(rulesetId, target)
      savedEvents.send(SetGoalEvent.Saved)
    }
  }

  fun remove() {
    val state = uiState.value
    val rulesetId = state.ruleset ?: return
    if (!state.canRemove) return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      goalRepository.clearGoal(rulesetId)
      savedEvents.send(SetGoalEvent.Saved)
    }
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory() = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        SetGoalViewModel(application.accountRepository, application.goalRepository)
      }
    }
  }
}

sealed interface SetGoalEvent {
  data object Saved : SetGoalEvent
}
