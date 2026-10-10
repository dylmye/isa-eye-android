package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews

/** Stateful entry point: owns the edit-account ViewModel and hoists its state into the content. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EditAccountScreen(productId: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: EditAccountViewModel =
    viewModel(factory = EditAccountViewModel.factory(productId))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        EditAccountEvent.Saved -> onClose()
      }
    }
  }

  val isCompact =
    !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )

  EditAccountContent(
    uiState = uiState,
    onNicknameChange = viewModel::onNicknameChange,
    onProductTypeSelected = viewModel::onProductTypeSelected,
    onFlexibleChange = viewModel::onFlexibleChange,
    onClosingYearSelected = viewModel::onClosingYearSelected,
    onSave = viewModel::save,
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  )
}

/** Stateless edit-account form. Renders [uiState] and reports events upwards. */
@Composable
fun EditAccountContent(
  uiState: EditAccountUiState,
  onNicknameChange: (String) -> Unit,
  onProductTypeSelected: (String) -> Unit,
  onFlexibleChange: (Boolean) -> Unit,
  onClosingYearSelected: (String) -> Unit,
  onSave: () -> Unit,
  onClose: () -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  FormDialogScaffold(
    title = "Edit account",
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    } else {
      EditAccountForm(
        uiState = uiState,
        onNicknameChange = onNicknameChange,
        onProductTypeSelected = onProductTypeSelected,
        onFlexibleChange = onFlexibleChange,
        onClosingYearSelected = onClosingYearSelected,
        onSave = onSave,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun EditAccountForm(
  uiState: EditAccountUiState,
  onNicknameChange: (String) -> Unit,
  onProductTypeSelected: (String) -> Unit,
  onFlexibleChange: (Boolean) -> Unit,
  onClosingYearSelected: (String) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp).imePadding(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OutlinedTextField(
      value = uiState.nickname,
      onValueChange = onNicknameChange,
      label = { Text("Nickname (optional)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    ReadOnlyField(label = "Bank", value = uiState.providerName)
    ReadOnlyField(label = "Year of opening", value = uiState.startTaxYear)
    OptionDropdownField(
      label = "Type of ISA",
      options = uiState.productTypeOptions,
      selectedId = uiState.selectedProductTypeId,
      onSelected = onProductTypeSelected,
    )
    FlexibleField(flexible = uiState.flexible, onFlexibleChange = onFlexibleChange)
    OptionDropdownField(
      label = "Closing year",
      options = uiState.closingYearOptions,
      selectedId = uiState.selectedClosingYearId,
      onSelected = onClosingYearSelected,
    )
    Spacer(Modifier.weight(1f))
    Button(
      onClick = onSave,
      enabled = uiState.canSave && !uiState.isSaving,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Save")
    }
  }
}

@FormFactorPreviews
@Composable
private fun EditAccountContentPreview() {
  ISAEyeTheme {
    EditAccountContent(
      uiState =
        EditAccountUiState(
          nickname = "My ISA",
          providerName = "Lloyds Bank",
          startTaxYear = "2025/2026",
          productTypeOptions = listOf(PickerOption("CASH", "Cash ISA")),
          selectedProductTypeId = "CASH",
          closingYearOptions = listOf(PickerOption(STILL_OPEN_ID, "Still open")),
          selectedClosingYearId = STILL_OPEN_ID,
          isLoading = false,
        ),
      onNicknameChange = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onClosingYearSelected = {},
      onSave = {},
      onClose = {},
      isCompact = true,
    )
  }
}
