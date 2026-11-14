package me.dylmye.isa.ui.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
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
  private val savedStateHandle: SavedStateHandle,
  private val onDataChanged: () -> Unit = {},
) : ViewModel() {
  private val draft = MutableStateFlow(savedStateHandle.setGoalDraft())
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

  fun onAmountChange(value: String) = updateDraft { it.copy(input = sanitizePoundsInput(value)) }

  fun save() {
    // The amount is read from the draft because the form reports text asynchronously; pressing Done
    // forwards the field's current text into the draft first, so uiState can still lag behind it.
    val form = draft.value
    val state =
      uiState.value.copy(
        input = form.input ?: uiState.value.input,
        isSaving = form.isSaving,
      )
    val rulesetId = state.ruleset
    val target = state.enteredPence
    if (rulesetId == null || target == null || !state.canSave) return
    updateDraft { it.copy(isSaving = true) }
    viewModelScope.launch {
      goalRepository.setGoal(rulesetId, target)
      onDataChanged()
      savedEvents.send(SetGoalEvent.Saved)
    }
  }

  fun remove() {
    val state = uiState.value
    val rulesetId = state.ruleset ?: return
    if (!state.canRemove) return
    updateDraft { it.copy(isSaving = true) }
    viewModelScope.launch {
      goalRepository.clearGoal(rulesetId)
      onDataChanged()
      savedEvents.send(SetGoalEvent.Removed)
    }
  }

  private fun updateDraft(transform: (SetGoalDraft) -> SetGoalDraft) {
    draft.update { current ->
      transform(current).also { savedStateHandle.writeSetGoalDraft(it) }
    }
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory() = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        SetGoalViewModel(
          application.accountRepository,
          application.goalRepository,
          createSavedStateHandle(),
          application::refreshWidgets,
        )
      }
    }
  }
}

private const val DRAFT_INPUT = "setGoal.input"

private fun SavedStateHandle.setGoalDraft(): SetGoalDraft = SetGoalDraft(input = this[DRAFT_INPUT])

private fun SavedStateHandle.writeSetGoalDraft(draft: SetGoalDraft) {
  this[DRAFT_INPUT] = draft.input
}

sealed interface SetGoalEvent {
  /** A goal was saved, so the monthly reminder is now worth asking about. */
  data object Saved : SetGoalEvent

  /** The goal was cleared. */
  data object Removed : SetGoalEvent
}
