package me.dylmye.isa.ui.accounts

/** The two pages of the add-account flow. */
enum class AddAccountStep {
  Details,
  FollowUp,
}

/** A selectable option shown in a picker. [searchTerms] holds extra text (e.g. aliases) to match. */
data class PickerOption(
  val id: String,
  val label: String,
  val description: String? = null,
  val searchTerms: List<String> = emptyList(),
)

/** Whether this option matches a search [query] against its label or any of its search terms. */
fun PickerOption.matches(query: String): Boolean {
  val matchesLabel = label.contains(query, ignoreCase = true)
  val matchesTerm = searchTerms.any { it.contains(query, ignoreCase = true) }
  return matchesLabel || matchesTerm
}

/** Immutable snapshot of everything the add-account flow needs to render. */
data class AddAccountUiState(
  val step: AddAccountStep = AddAccountStep.Details,
  val nickname: String = "",
  val providerOptions: List<PickerOption> = emptyList(),
  val selectedProviderId: String? = null,
  val rulesetOptions: List<PickerOption> = emptyList(),
  val selectedRulesetId: String? = null,
  val productTypeOptions: List<PickerOption> = emptyList(),
  val selectedProductTypeId: String? = null,
  val flexible: Boolean = false,
  val isLoading: Boolean = false,
  val isSaving: Boolean = false,
)

val AddAccountUiState.selectedProvider: PickerOption?
  get() = providerOptions.firstOrNull { it.id == selectedProviderId }

val AddAccountUiState.selectedProductType: PickerOption?
  get() = productTypeOptions.firstOrNull { it.id == selectedProductTypeId }

/** Basic details are complete once a bank and a tax year are chosen. */
val AddAccountUiState.canContinue: Boolean
  get() = selectedProviderId != null && selectedRulesetId != null

/** The follow-up is complete once an ISA type is chosen. */
val AddAccountUiState.canSave: Boolean
  get() = selectedProductTypeId != null

/** The account name: the nickname, or the product type when none was given. */
fun accountDisplayName(nickname: String, productTypeName: String): String =
  nickname.trim().ifEmpty { productTypeName }
