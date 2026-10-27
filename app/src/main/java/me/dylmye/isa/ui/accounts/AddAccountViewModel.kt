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
import me.dylmye.isa.billing.AccountLimitPolicy
import me.dylmye.isa.billing.RestoreResult
import me.dylmye.isa.data.AccountLimitRepository
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderAliasEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import java.util.UUID

/**
 * State holder for the add-account flow. Reference data is observed from Room; the in-progress form
 * lives only here as [draft] until the account is saved.
 */
class AddAccountViewModel(
  private val repository: AccountRepository,
  private val accountLimitRepository: AccountLimitRepository,
  private val policy: AccountLimitPolicy,
) : ViewModel() {
  private val draft = MutableStateFlow(AddAccountDraft())
  private val savedEvents = Channel<AddAccountEvent>(Channel.BUFFERED)

  /** One-off events, e.g. the account was saved and the flow should close. */
  val events: Flow<AddAccountEvent> = savedEvents.receiveAsFlow()

  private val referenceData =
    combine(
      repository.observeProviders(),
      repository.observeProviderAliases(),
      repository.observeRulesets(),
      repository.observeProductTypes(),
    ) { providers, aliases, rulesets, productTypes ->
      ReferenceData(providers, aliases, rulesets, productTypes)
    }

  private val entitlement =
    combine(
      accountLimitRepository.observeAccountCount(),
      policy.isUnlimited,
      policy.isRestoring,
    ) { count, unlimited, restoring ->
      EntitlementState(count, unlimited, restoring)
    }

  val uiState: StateFlow<AddAccountUiState> =
    combine(referenceData, entitlement, draft) { references, entitlements, draft ->
      val aliasesByProvider = references.aliases.groupBy { it.providerId }
      AddAccountUiState(
        step = draft.step,
        nickname = draft.nickname,
        providerOptions =
          references.providers.map { provider ->
            PickerOption(
              id = provider.id,
              label = provider.name,
              searchTerms = aliasesByProvider[provider.id].orEmpty().map { it.alias },
            )
          },
        selectedProviderId = draft.providerId,
        rulesetOptions = references.rulesets.map { PickerOption(it.id, it.id) },
        // Default to the most recent tax year, matching the web app.
        selectedRulesetId = draft.rulesetId ?: references.rulesets.lastOrNull()?.id,
        productTypeOptions =
          references.productTypes.map { PickerOption(it.id, it.name, it.shortDescription) },
        selectedProductTypeId = draft.productTypeId,
        flexible = draft.flexible,
        isSaving = draft.isSaving,
        accountCount = entitlements.count,
        freeAccountLimit = policy.freeAccountLimit,
        isUnlimited = entitlements.unlimited,
        isRestoring = entitlements.restoring,
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

  fun onRestorePurchases() {
    if (uiState.value.isRestoring) return
    viewModelScope.launch {
      val message =
        when (policy.restore()) {
          RestoreResult.Restored -> "Purchases restored."
          RestoreResult.NotFound -> "No previous purchase found."
          RestoreResult.Unavailable -> "Google Play billing isn't available on this device."
          RestoreResult.Failed -> "Purchases couldn't be restored. Try again later."
        }
      savedEvents.send(AddAccountEvent.ShowMessage(message))
    }
  }

  fun save() {
    val state = uiState.value
    if (!state.canCreateAccount) {
      viewModelScope.launch { savedEvents.send(AddAccountEvent.LimitReached) }
      return
    }
    val product = buildProduct(state) ?: return
    draft.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.upsertProduct(product)
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
        AddAccountViewModel(
          application.accountRepository,
          application.accountLimitRepository,
          application.accountLimitPolicy,
        )
      }
    }
  }
}

private data class ReferenceData(
  val providers: List<ProviderEntity>,
  val aliases: List<ProviderAliasEntity>,
  val rulesets: List<RulesetEntity>,
  val productTypes: List<ProductTypeEntity>,
)

private data class EntitlementState(val count: Int, val unlimited: Boolean, val restoring: Boolean)

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

  data object LimitReached : AddAccountEvent

  data class ShowMessage(val message: String) : AddAccountEvent
}
