package me.dylmye.isa.ui.accounts

/** Sentinel id for the "still open" choice in the closing-year picker. */
internal const val STILL_OPEN_ID = "open"

/** Immutable snapshot of the edit-account form. Provider and opening year are fixed. */
data class EditAccountUiState(
  val nickname: String = "",
  val providerName: String = "",
  val startTaxYear: String = "",
  val productTypeOptions: List<PickerOption> = emptyList(),
  val selectedProductTypeId: String? = null,
  val flexible: Boolean = false,
  val closingYearOptions: List<PickerOption> = emptyList(),
  val selectedClosingYearId: String? = null,
  val isLoading: Boolean = true,
  val isSaving: Boolean = false,
)

/** The form is complete once an ISA type is chosen. */
val EditAccountUiState.canSave: Boolean
  get() = selectedProductTypeId != null

/** Closing years: "still open", then every ruleset from the opening year onwards. */
internal fun closingYearOptions(
  rulesetIds: List<String>,
  startTaxYear: String,
): List<PickerOption> {
  val years = rulesetIds.filter { it >= startTaxYear }.map { PickerOption(it, it) }
  return listOf(PickerOption(STILL_OPEN_ID, "Still open")) + years
}
