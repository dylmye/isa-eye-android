package me.dylmye.isa.ui.accounts

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.window.core.layout.WindowSizeClass
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.rememberDisplayTextState
import me.dylmye.isa.ui.components.reportTextChanges
import me.dylmye.isa.ui.components.restoreFocusAfterConfigChange

/** Stateful entry point: owns the ViewModel and hoists its state into [AddAccountContent]. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AddAccountScreen(onClose: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AddAccountViewModel = viewModel(factory = AddAccountViewModel.Factory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        AddAccountEvent.Saved -> {
          onSaved()
        }

        AddAccountEvent.LimitReached -> {
          Toast.makeText(
            context,
            "You've reached the free account limit.",
            Toast.LENGTH_SHORT,
          )
            .show()
        }

        is AddAccountEvent.ShowMessage -> {
          Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  // System back returns to the first page while on the second, then falls through to close.
  val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
  NavigationBackHandler(
    state = navigationState,
    isBackEnabled = uiState.step == AddAccountStep.FollowUp,
    onBackCompleted = viewModel::onBack,
  )

  val isCompact =
    !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )

  AddAccountContent(
    uiState = uiState,
    onNicknameChange = viewModel::onNicknameChange,
    onProviderSelected = viewModel::onProviderSelected,
    onRulesetSelected = viewModel::onRulesetSelected,
    onProductTypeSelected = viewModel::onProductTypeSelected,
    onFlexibleChange = viewModel::onFlexibleChange,
    onNext = viewModel::onNext,
    onBack = viewModel::onBack,
    onSave = viewModel::save,
    onRestorePurchases = viewModel::onRestorePurchases,
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  )
}

/** Stateless add-account flow. Renders [uiState] and reports events upwards. */
@Composable
fun AddAccountContent(
  uiState: AddAccountUiState,
  onNicknameChange: (String) -> Unit,
  onProviderSelected: (String) -> Unit,
  onRulesetSelected: (String) -> Unit,
  onProductTypeSelected: (String) -> Unit,
  onFlexibleChange: (Boolean) -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
  onSave: () -> Unit,
  onRestorePurchases: () -> Unit,
  onClose: () -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  FormDialogScaffold(
    title = "Add account",
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  ) { innerPadding ->
    when (uiState.step) {
      AddAccountStep.Details -> {
        AddAccountDetailsContent(
          uiState = uiState,
          onNicknameChange = onNicknameChange,
          onProviderSelected = onProviderSelected,
          onRulesetSelected = onRulesetSelected,
          onNext = onNext,
          onRestorePurchases = onRestorePurchases,
          modifier = Modifier.padding(innerPadding),
        )
      }

      AddAccountStep.FollowUp -> {
        AddAccountFollowUpContent(
          uiState = uiState,
          onProductTypeSelected = onProductTypeSelected,
          onFlexibleChange = onFlexibleChange,
          onBack = onBack,
          onSave = onSave,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDetailsContent(
  uiState: AddAccountUiState,
  onNicknameChange: (String) -> Unit,
  onProviderSelected: (String) -> Unit,
  onRulesetSelected: (String) -> Unit,
  onNext: () -> Unit,
  onRestorePurchases: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showProviderSheet by rememberSaveable { mutableStateOf(false) }
  val nicknameState = rememberTextFieldState(uiState.nickname)
  nicknameState.reportTextChanges(onNicknameChange)

  Column(modifier = modifier.fillMaxSize().imePadding()) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      val limit = uiState.freeAccountLimit
      if (uiState.showLimitCard && limit != null) {
        FreeAccountLimitCard(
          accountCount = uiState.accountCount,
          limit = limit,
          isRestoring = uiState.isRestoring,
          onRestorePurchases = onRestorePurchases,
        )
      }
      OutlinedTextField(
        state = nicknameState,
        label = { Text("Nickname (optional)") },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, showKeyboardOnFocus = true),
        modifier =
          Modifier.fillMaxWidth()
            .restoreFocusAfterConfigChange(focusOnOpen = true)
            .testTag("robo_add_nickname"),
      )
      ProviderField(
        selected = uiState.selectedProvider,
        onClick = { showProviderSheet = true },
      )
      OptionDropdownField(
        label = "Year of opening",
        options = uiState.rulesetOptions,
        selectedId = uiState.selectedRulesetId,
        onSelected = onRulesetSelected,
      )
    }
    Button(
      onClick = onNext,
      enabled = uiState.canContinue,
      modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("robo_add_continue"),
    ) {
      Text("Continue")
    }
  }

  if (showProviderSheet) {
    ProviderPickerSheet(
      options = uiState.providerOptions,
      onSelected = onProviderSelected,
      onDismiss = { showProviderSheet = false },
    )
  }
}

/**
 * Explains the free-tier limit before the user reaches it. The purchase itself is not offered here:
 * the add FAB remains the gate once the limit is met.
 */
@Composable
private fun FreeAccountLimitCard(
  accountCount: Int,
  limit: Int,
  isRestoring: Boolean,
  onRestorePurchases: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Free version: $accountCount of $limit accounts used",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          "Accounts count across all tax years. You'll need to unlock unlimited accounts to add " +
            "more than $limit.",
        style = MaterialTheme.typography.bodyMedium,
      )
      TextButton(onClick = onRestorePurchases, enabled = !isRestoring) {
        Text(if (isRestoring) "Restoring..." else "Restore purchases")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountFollowUpContent(
  uiState: AddAccountUiState,
  onProductTypeSelected: (String) -> Unit,
  onFlexibleChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().imePadding()) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      OptionDropdownField(
        label = "Type of ISA",
        options = uiState.productTypeOptions,
        selectedId = uiState.selectedProductTypeId,
        onSelected = onProductTypeSelected,
        testTag = "robo_add_type",
        // The follow-up step has no text field, so its first control takes the opening focus.
        fieldModifier = Modifier.restoreFocusAfterConfigChange(focusOnOpen = true),
      )
      FlexibleField(flexible = uiState.flexible, onFlexibleChange = onFlexibleChange)
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
      Button(
        onClick = onSave,
        enabled = uiState.canSave && !uiState.isSaving,
        modifier = Modifier.weight(1f).testTag("robo_add_save"),
      ) {
        Text("Save")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderField(
  selected: PickerOption?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // A read-only text field (styled like the other inputs) whose anchor opens the picker sheet.
  ExposedDropdownMenuBox(
    expanded = false,
    onExpandedChange = { onClick() },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      state = rememberDisplayTextState(selected?.label ?: ""),
      readOnly = true,
      label = { Text("Bank") },
      lineLimits = TextFieldLineLimits.SingleLine,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag("robo_add_bank"),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPickerSheet(
  options: List<PickerOption>,
  onSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val queryState = rememberTextFieldState()
  val query = queryState.text.toString()
  val filtered = remember(options, query) {
    if (query.isBlank()) {
      options
    } else {
      options.filter { it.matches(query) }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    LazyColumn(
      modifier =
        Modifier.fillMaxWidth()
          .semantics { testTagsAsResourceId = true }
          .testTag("robo_bank_list"),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      item(key = "header", contentType = "header") {
        Column(Modifier.padding(horizontal = 16.dp)) {
          Text(
            "Bank",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
          )
          OutlinedTextField(
            state = queryState,
            label = { Text("Search banks") },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(showKeyboardOnFocus = true),
            modifier =
              Modifier.fillMaxWidth()
                .restoreFocusAfterConfigChange()
                .padding(vertical = 8.dp)
                .testTag("robo_bank_search"),
          )
        }
      }
      items(filtered, key = { it.id }, contentType = { "provider" }) { option ->
        ListItem(
          headlineContent = { Text(option.label) },
          modifier =
            Modifier.testTag("robo_bank_${option.id}")
              .clickable(onClickLabel = "Choose ${option.label}") {
                onSelected(option.id)
                onDismiss()
              },
        )
      }
    }
  }
}

@FormFactorPreviews
@Composable
private fun AddAccountDetailsPreview() {
  ISAEyeTheme {
    AddAccountContent(
      uiState =
        AddAccountUiState(
          providerOptions = listOf(PickerOption("lloyds", "Lloyds Bank")),
          selectedProviderId = "lloyds",
          rulesetOptions = listOf(PickerOption("2025/2026", "2025/2026")),
          selectedRulesetId = "2025/2026",
        ),
      isCompact = true,
      onNicknameChange = {},
      onProviderSelected = {},
      onRulesetSelected = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onNext = {},
      onBack = {},
      onSave = {},
      onRestorePurchases = {},
      onClose = {},
    )
  }
}

@FormFactorPreviews
@Composable
private fun AddAccountFollowUpPreview() {
  ISAEyeTheme {
    AddAccountContent(
      uiState =
        AddAccountUiState(
          step = AddAccountStep.FollowUp,
          productTypeOptions = listOf(PickerOption("CASH", "Cash ISA")),
          selectedProductTypeId = "CASH",
          flexible = true,
        ),
      isCompact = false,
      onNicknameChange = {},
      onProviderSelected = {},
      onRulesetSelected = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onNext = {},
      onBack = {},
      onSave = {},
      onRestorePurchases = {},
      onClose = {},
    )
  }
}

@FormFactorPreviews
@Composable
private fun AddAccountLimitCardPreview() {
  ISAEyeTheme {
    AddAccountContent(
      uiState =
        AddAccountUiState(
          providerOptions = listOf(PickerOption("lloyds", "Lloyds Bank")),
          selectedProviderId = "lloyds",
          rulesetOptions = listOf(PickerOption("2026/2027", "2026/2027")),
          selectedRulesetId = "2026/2027",
          accountCount = 2,
          freeAccountLimit = 4,
        ),
      isCompact = true,
      onNicknameChange = {},
      onProviderSelected = {},
      onRulesetSelected = {},
      onProductTypeSelected = {},
      onFlexibleChange = {},
      onNext = {},
      onBack = {},
      onSave = {},
      onRestorePurchases = {},
      onClose = {},
    )
  }
}
