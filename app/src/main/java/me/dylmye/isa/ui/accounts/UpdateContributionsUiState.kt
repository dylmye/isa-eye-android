package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.ui.parsePoundsToPence
import me.dylmye.isa.ui.penceToPoundsInput

/**
 * The mutable parts of the update-contributions form.
 *
 * A `null` [amountInput] means the field has not been touched, so it tracks the selected year and
 * shows that year's recorded figure. Picking another year clears the field back to `null`, which is
 * what makes the amount follow the year until the user overrides it.
 */
internal data class UpdateContributionsDraft(
  val yearId: String? = null,
  val amountInput: String? = null,
  val isSaving: Boolean = false,
)

/** Immutable snapshot of the update-contributions form. */
data class UpdateContributionsUiState(
  val accountName: String = "",
  val isFlexible: Boolean = false,
  val yearOptions: List<PickerOption> = emptyList(),
  val selectedYearId: String? = null,
  val amountInput: String = "",
  val recordedPence: Long = 0L,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
)

/** The entered amount in pence, or null while the field is empty or not a number. */
val UpdateContributionsUiState.amountPence: Long?
  get() = parsePoundsToPence(amountInput)

/** Saving needs a chosen year and a valid, non-negative amount. */
val UpdateContributionsUiState.canSave: Boolean
  get() = selectedYearId != null && amountPence != null

/**
 * A non-flexible ISA cannot have contributions withdrawn, so an amount below the figure already
 * recorded for the year is worth flagging. It never blocks saving: the recorded figure may itself
 * have been a mistake.
 */
val UpdateContributionsUiState.showReductionWarning: Boolean
  get() {
    val entered = amountPence ?: return false
    return !isFlexible && entered < recordedPence
  }

/** The tax years this account was open in, in order. */
internal fun contributionYearOptions(
  rulesetIds: List<String>,
  startTaxYear: String,
  endTaxYear: String?,
): List<PickerOption> {
  val open = rulesetIds.filter { it >= startTaxYear && (endTaxYear == null || it <= endTaxYear) }
  return open.map { PickerOption(it, it) }
}

/**
 * Resolves the form from the account, its recorded [balances] and the [currentRulesetId] the rest of
 * the accounts view is scoped to — the year the form opens on.
 */
internal fun resolveUpdateContributions(
  product: ProductEntity?,
  balances: List<AnnualBalanceEntity>,
  rulesetIds: List<String>,
  currentRulesetId: String?,
  draft: UpdateContributionsDraft,
): UpdateContributionsUiState {
  val options =
    contributionYearOptions(rulesetIds, product?.startTaxYear.orEmpty(), product?.endTaxYear)
  val optionIds = options.map { it.id }
  val requested = draft.yearId ?: resolveRuleset(rulesetIds, currentRulesetId).current
  val selectedYear = requested?.takeIf { it in optionIds } ?: options.lastOrNull()?.id
  val recordedPence =
    balances.firstOrNull { it.rulesetId == selectedYear }?.deductedFromAllowancePence ?: 0L
  return UpdateContributionsUiState(
    accountName = product?.friendlyName.orEmpty(),
    isFlexible = product?.flexible ?: false,
    yearOptions = options,
    selectedYearId = selectedYear,
    amountInput = draft.amountInput ?: penceToPoundsInput(recordedPence),
    recordedPence = recordedPence,
    isLoading = product == null,
    isSaving = draft.isSaving,
  )
}
