package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.creditDatesBetween
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.nextCreditOn
import me.dylmye.isa.time.isoDate
import me.dylmye.isa.ui.parsePoundsToPence
import me.dylmye.isa.ui.penceToPoundsInput

/**
 * The mutable parts of the auto-contribution form.
 *
 * A `null` [input] means the field has not been touched, so it shows the amount already saved for the
 * year. Once the user types, the draft wins until the form closes.
 */
internal data class AutoContributionDraft(val input: String? = null, val isSaving: Boolean = false)

/** Immutable snapshot of the auto-contribution form, which always describes today's tax year. */
data class AutoContributionUiState(
  val ruleset: String? = null,
  val input: String = "",
  val savedMonthlyPence: Long? = null,
  val firstCreditDate: String? = null,
  val lastCreditDate: String? = null,
  val credits: Int = 0,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
)

/** The entered monthly amount in pence, or null while the field is empty or not a number. */
val AutoContributionUiState.enteredPence: Long?
  get() = if (input.isBlank()) null else parsePoundsToPence(input)

/** What the schedule would credit across the rest of the year at the entered amount. */
val AutoContributionUiState.projectedTotalPence: Long?
  get() = enteredPence?.let { it * credits }

/** Why the entered amount cannot be saved, or null while it is blank or valid. */
val AutoContributionUiState.error: String?
  get() {
    val entered = enteredPence ?: return null
    return if (entered <= 0L) "Enter an amount above £0." else null
  }

/** Saving needs a running year, at least one credit left in it, and a positive amount. */
val AutoContributionUiState.canSave: Boolean
  get() = !isSaving && ruleset != null && credits > 0 && enteredPence != null && error == null

/** Turning the schedule off is only offered once one is saved. */
val AutoContributionUiState.canRemove: Boolean
  get() = !isSaving && savedMonthlyPence != null

/**
 * Resolves the form from the [rulesets], the account's existing [schedules] and the [draft], read
 * against [nowMillis]. Today's ruleset supplies the year the schedule belongs to and its end date;
 * an existing schedule keeps the first credit date it was created with.
 */
internal fun resolveAutoContribution(
  rulesets: List<RulesetEntity>,
  schedules: List<AutoContributionEntity>,
  nowMillis: Long,
  draft: AutoContributionDraft,
): AutoContributionUiState {
  val today = isoDate(nowMillis)
  val running = rulesets.firstOrNull { today >= it.startDate && today <= it.endDate }
  val existing = running?.let { year -> schedules.firstOrNull { it.rulesetId == year.id } }
  val first = existing?.firstCreditDate ?: nextCreditOn(today)
  val credits =
    if (running != null && first != null) {
      creditDatesBetween(first, running.endDate)
    } else {
      emptyList()
    }
  return AutoContributionUiState(
    ruleset = running?.id,
    input = draft.input ?: existing?.monthlyPence?.let(::penceToPoundsInput).orEmpty(),
    savedMonthlyPence = existing?.monthlyPence,
    firstCreditDate = first,
    lastCreditDate = credits.lastOrNull(),
    credits = credits.size,
    isLoading = false,
    isSaving = draft.isSaving,
  )
}
