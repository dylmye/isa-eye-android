package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.LoadingIndicator
import me.dylmye.isa.ui.formatGbp

private const val REDUCTION_WARNING =
  "This ISA isn't flexible, so its contributions shouldn't go down. Save only if the recorded " +
    "figure was wrong."

/** Stateful entry point: owns the update-contributions ViewModel and hoists its state down. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UpdateContributionsScreen(
  productId: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: UpdateContributionsViewModel =
    viewModel(factory = UpdateContributionsViewModel.factory(productId))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        UpdateContributionsEvent.Saved -> onClose()
      }
    }
  }

  val isCompact =
    !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )

  UpdateContributionsContent(
    uiState = uiState,
    onYearSelected = viewModel::onYearSelected,
    onAmountChange = viewModel::onAmountChange,
    onSave = viewModel::save,
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  )
}

/** Stateless update-contributions form. Renders [uiState] and reports events upwards. */
@Composable
fun UpdateContributionsContent(
  uiState: UpdateContributionsUiState,
  onYearSelected: (String) -> Unit,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  onClose: () -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  FormDialogScaffold(
    title = "Update contributions",
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator(label = "Loading contributions")
      }
    } else {
      UpdateContributionsForm(
        uiState = uiState,
        onYearSelected = onYearSelected,
        onAmountChange = onAmountChange,
        onSave = onSave,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun UpdateContributionsForm(
  uiState: UpdateContributionsUiState,
  onYearSelected: (String) -> Unit,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val warnings = warningMessages(uiState)
  Column(modifier = modifier.fillMaxSize().imePadding()) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ReadOnlyField(label = "Account", value = uiState.accountName)
      OptionDropdownField(
        label = "Tax year",
        options = uiState.yearOptions,
        selectedId = uiState.selectedYearId,
        onSelected = onYearSelected,
      )
      OutlinedTextField(
        value = uiState.amountInput,
        onValueChange = onAmountChange,
        label = { Text("Contributions") },
        prefix = { Text("£") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = warnings.isNotEmpty(),
        supportingText =
          if (warnings.isEmpty()) {
            null
          } else {
            {
              Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                warnings.forEach { message -> Text(message) }
              }
            }
          },
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Button(
      onClick = onSave,
      enabled = uiState.canSave && !uiState.isSaving,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Text("Save")
    }
  }
}

/** Field cautions for [uiState]; none block saving, they only explain the entered figure. */
private fun warningMessages(uiState: UpdateContributionsUiState): List<String> = buildList {
  val allowance = uiState.allowancePence
  if (uiState.showAllowanceWarning && allowance != null) {
    add("The maximum contribution for this year is ${formatGbp(allowance)}.")
  }
  if (uiState.showReductionWarning) add(REDUCTION_WARNING)
}

@FormFactorPreviews
@Composable
private fun UpdateContributionsContentPreview() {
  ISAEyeTheme {
    UpdateContributionsContent(
      uiState =
        UpdateContributionsUiState(
          accountName = "Cash ISA",
          yearOptions =
            listOf(
              PickerOption("2025/2026", "2025/2026"),
              PickerOption("2026/2027", "2026/2027"),
            ),
          selectedYearId = "2026/2027",
          amountInput = "1000",
          recordedPence = 125_000,
          isLoading = false,
        ),
      onYearSelected = {},
      onAmountChange = {},
      onSave = {},
      onClose = {},
      isCompact = true,
    )
  }
}

@FormFactorPreviews
@Composable
private fun UpdateContributionsContentFlexiblePreview() {
  ISAEyeTheme {
    UpdateContributionsContent(
      uiState =
        UpdateContributionsUiState(
          accountName = "Stocks & Shares ISA",
          isFlexible = true,
          yearOptions = listOf(PickerOption("2026/2027", "2026/2027")),
          selectedYearId = "2026/2027",
          amountInput = "4320.50",
          recordedPence = 432_050,
          isLoading = false,
        ),
      onYearSelected = {},
      onAmountChange = {},
      onSave = {},
      onClose = {},
      isCompact = false,
    )
  }
}
