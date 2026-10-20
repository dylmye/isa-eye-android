package me.dylmye.isa.ui.insights

import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.isoDate
import me.dylmye.isa.ui.parsePoundsToPence
import me.dylmye.isa.ui.penceToPoundsInput

/**
 * The mutable parts of the goal form.
 *
 * A `null` [input] means the field has not been touched, so it shows the goal already saved for the
 * year. Once the user types, the draft wins until the form closes.
 */
internal data class SetGoalDraft(val input: String? = null, val isSaving: Boolean = false)

/** Immutable snapshot of the goal form, which always describes today's tax year. */
data class SetGoalUiState(
  val ruleset: String? = null,
  val allowancePence: Long? = null,
  val input: String = "",
  val savedPence: Long? = null,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
)

/** The entered goal in pence, or null while the field is empty or not a number. */
val SetGoalUiState.enteredPence: Long?
  get() = if (input.isBlank()) null else parsePoundsToPence(input)

/**
 * Why the entered goal cannot be saved, or null while it is blank or valid. The allowance is a hard
 * limit here rather than a caution: money cannot be contributed beyond it, so a goal past it would
 * never be reachable.
 */
val SetGoalUiState.error: String?
  get() {
    val entered = enteredPence ?: return null
    val allowance = allowancePence
    return when {
      entered <= 0L -> "Enter an amount above £0."
      allowance != null && entered > allowance -> overAllowance(allowance)
      else -> null
    }
  }

/** Saving needs a running tax year and a goal that is positive and within that year's allowance. */
val SetGoalUiState.canSave: Boolean
  get() = !isSaving && ruleset != null && enteredPence != null && error == null

/** Removing is only offered once a goal has been saved for the year. */
val SetGoalUiState.canRemove: Boolean
  get() = !isSaving && savedPence != null

/**
 * Resolves the form from the [rulesets], the [goals] already saved and the [draft], read against
 * [nowMillis]. Today's ruleset supplies both the year the goal belongs to and its allowance.
 */
internal fun resolveSetGoal(
  rulesets: List<RulesetEntity>,
  goals: List<GoalEntity>,
  nowMillis: Long,
  draft: SetGoalDraft,
): SetGoalUiState {
  val today = isoDate(nowMillis)
  val current = rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
  val saved = current?.let { year -> goals.firstOrNull { it.rulesetId == year.id }?.targetPence }
  return SetGoalUiState(
    ruleset = current?.id,
    allowancePence = current?.sharedAllowancePence,
    input = draft.input ?: saved?.let(::penceToPoundsInput).orEmpty(),
    savedPence = saved,
    isLoading = false,
    isSaving = draft.isSaving,
  )
}

private fun overAllowance(allowance: Long): String =
  "That's more than this year's ${formatGbp(allowance)} allowance."
