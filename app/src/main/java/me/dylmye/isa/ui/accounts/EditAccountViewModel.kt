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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.db.entity.ProductEntity

/**
 * State holder for the edit-account form. The provider and opening year are fixed; the editable
 * fields are the nickname, ISA type, flexibility and closing year.
 */
class EditAccountViewModel(
  private val repository: AccountRepository,
  private val productId: String,
) : ViewModel() {
  private val draft = MutableStateFlow<EditAccountDraft?>(null)
  private val savedEvents = Channel<EditAccountEvent>(Channel.BUFFERED)

  private var product: ProductEntity? = null

  /** One-off events, e.g. the account was saved and the flow should close. */
  val events: Flow<EditAccountEvent> = savedEvents.receiveAsFlow()

  val uiState: StateFlow<EditAccountUiState> =
    combine(
      repository.observeProviders(),
      repository.observeRulesets(),
      repository.observeProductTypes(),
      draft,
    ) { providers, rulesets, productTypes, form ->
      val loaded = product
      if (form == null || loaded == null) {
        EditAccountUiState(isLoading = true)
      } else {
        EditAccountUiState(
          nickname = form.nickname,
          providerName = providers.firstOrNull { it.id == loaded.providerId }?.name.orEmpty(),
          startTaxYear = loaded.startTaxYear,
          productTypeOptions =
            productTypes.map { PickerOption(it.id, it.name, it.shortDescription) },
          selectedProductTypeId = form.productTypeId,
          flexible = form.flexible,
          closingYearOptions = closingYearOptions(rulesets.map { it.id }, loaded.startTaxYear),
          selectedClosingYearId = form.closingYearId,
          isLoading = false,
          isSaving = form.isSaving,
        )
      }
    }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        EditAccountUiState(isLoading = true),
      )

  init {
    viewModelScope.launch {
      val loaded = repository.observeProduct(productId).first() ?: return@launch
      product = loaded
      // friendlyName stores the display name, so a nickname is only present when it differs from
      // the product type name.
      val typeName = repository.productTypeName(loaded.productTypeId)
      draft.value =
        EditAccountDraft(
          nickname = loaded.friendlyName.takeIf { it != typeName }.orEmpty(),
          productTypeId = loaded.productTypeId,
          flexible = loaded.flexible,
          closingYearId = loaded.endTaxYear ?: STILL_OPEN_ID,
        )
    }
  }

  fun onNicknameChange(value: String) = updateDraft { it.copy(nickname = value) }

  fun onProductTypeSelected(id: String) = updateDraft { it.copy(productTypeId = id) }

  fun onFlexibleChange(value: Boolean) = updateDraft { it.copy(flexible = value) }

  fun onClosingYearSelected(id: String) = updateDraft { it.copy(closingYearId = id) }

  fun save() {
    val loaded = product ?: return
    val state = uiState.value
    val typeId = state.selectedProductTypeId ?: return
    val typeName = state.productTypeOptions.firstOrNull { it.id == typeId }?.label ?: typeId
    updateDraft { it.copy(isSaving = true) }
    viewModelScope.launch {
      repository.upsertProduct(
        loaded.copy(
          friendlyName = accountDisplayName(state.nickname, typeName),
          productTypeId = typeId,
          flexible = state.flexible,
          endTaxYear = state.selectedClosingYearId?.takeIf { it != STILL_OPEN_ID },
        ),
      )
      savedEvents.send(EditAccountEvent.Saved)
    }
  }

  private fun updateDraft(transform: (EditAccountDraft) -> EditAccountDraft) {
    draft.update { it?.let(transform) }
  }

  companion object {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    fun factory(productId: String) = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        EditAccountViewModel(application.accountRepository, productId)
      }
    }
  }
}

private data class EditAccountDraft(
  val nickname: String,
  val productTypeId: String?,
  val flexible: Boolean,
  val closingYearId: String,
  val isSaving: Boolean = false,
)

sealed interface EditAccountEvent {
  data object Saved : EditAccountEvent
}
