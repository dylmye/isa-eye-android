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
import me.dylmye.isa.data.db.entity.ProductEntity
import java.util.UUID

/**
 * State holder for the add-account flow. Reference data is observed from Room; the in-progress form
 * lives only here as [draft] until the account is saved.
 */
class AddAccountViewModel(private val repository: AccountRepository) : ViewModel() {
  private val draft = MutableStateFlow(AddAccountDraft())
  private val savedEvents = Channel<AddAccountEvent>(Channel.BUFFERED)

  /** One-off events, e.g. the account was saved and the flow should close. */
  val events: Flow<AddAccountEvent> = savedEvents.receiveAsFlow()

  val uiState: StateFlow<AddAccountUiState> =
    combine(
      repository.observeProviders(),
      repository.observeProviderAliases(),
      repository.observeRulesets(),
      repository.observeProductTypes(),
      draft,
    ) { providers, aliases, rulesets, productTypes, draft ->
      val aliasesByProvider = aliases.groupBy { it.providerId }
      AddAccountUiState(
        step = draft.step,
        nickname = draft.nickname,
        providerOptions =
          providers.map { provider ->
            PickerOption(
              id = provider.id,
              label = provider.name,
              searchTerms = aliasesByProvider[provider.id].orEmpty().map { it.alias },
            )
          },
        selectedProviderId = draft.providerId,
        rulesetOptions = rulesets.map { PickerOption(it.id, it.id) },
        // Default to the most recent tax year, matching the web app.
        selectedRulesetId = draft.rulesetId ?: rulesets.lastOrNull()?.id,
        productTypeOptions = productTypes.map { PickerOption(it.id, it.name, it.shortDescription) },
        selectedProductTypeId = draft.productTypeId,
        flexible = draft.flexible,
        isSaving = draft.isSaving,
      )
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AddAccountUiState(isLoading = true),
      )

  fun onNicknameChange(value: String) = draft.update { it.copy(nickname = value) }

  fun onProviderSelected(id: String) = draft.update { it.copy(providerId = id) }

  fun onRulesetSelected(id: String) = draft.update { it.copy(rulesetId = id) }

  fun onProductTypeSelected(id: String) = draft.update { it.copy(productTypeId = id) }

  fun onFlexibleChange(value: Boolean) = draft.update { it.copy(flexible = value) }

  fun onNext() = draft.update { it.copy(step = AddAccountStep.FollowUp) }

  fun onBack() = draft.update { it.copy(step = AddAccountStep.Details) }

  fun save() {
    val product = buildProduct(uiState.value) ?: return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.addProduct(product)
      savedEvents.send(AddAccountEvent.Saved)
    }
  }

  private fun buildProduct(state: AddAccountUiState): ProductEntity? {
    val providerId = state.selectedProviderId
    val productTypeId = state.selectedProductTypeId
    val rulesetId = state.selectedRulesetId
    if (providerId == null || productTypeId == null || rulesetId == null) return null
    return ProductEntity(
      id = UUID.randomUUID().toString(),
      startTaxYear = rulesetId,
      endTaxYear = null,
      providerId = providerId,
      friendlyName =
        accountDisplayName(
          nickname = state.nickname,
          providerName = state.selectedProvider?.label ?: providerId,
          productTypeName = state.selectedProductType?.label ?: productTypeId,
        ),
      productTypeId = productTypeId,
      flexible = state.flexible,
    )
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    val Factory = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        AddAccountViewModel(application.accountRepository)
      }
    }
  }
}

private data class AddAccountDraft(
  val step: AddAccountStep = AddAccountStep.Details,
  val nickname: String = "",
  val providerId: String? = null,
  val rulesetId: String? = null,
  val productTypeId: String? = null,
  val flexible: Boolean = false,
  val isSaving: Boolean = false,
)

sealed interface AddAccountEvent {
  data object Saved : AddAccountEvent
}
