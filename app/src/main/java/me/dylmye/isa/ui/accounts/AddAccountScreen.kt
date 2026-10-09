package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

/** Stateful entry point: owns the ViewModel and hoists its state into [AddAccountContent]. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AddAccountScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AddAccountViewModel = viewModel(factory = AddAccountViewModel.Factory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        AddAccountEvent.Saved -> onClose()
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
    onClose = onClose,
    isCompact = isCompact,
    modifier = modifier,
  )
}

/** Stateless add-account flow. Renders [uiState] and reports events upwards. */
@OptIn(ExperimentalMaterial3Api::class)
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
  onClose: () -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Surface(
      shape = if (isCompact) RectangleShape else MaterialTheme.shapes.extraLarge,
      tonalElevation = if (isCompact) 0.dp else 6.dp,
      modifier =
        if (isCompact) {
          Modifier.fillMaxSize()
        } else {
          Modifier.widthIn(max = 560.dp).fillMaxHeight(0.9f)
        },
    ) {
      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          TopAppBar(
            title = { Text("Add account") },
            actions = { TextButton(onClick = onClose) { Text("Cancel") } },
          )
        },
        contentWindowInsets =
          if (isCompact) ScaffoldDefaults.contentWindowInsets else WindowInsets(0.dp),
      ) { innerPadding ->
        when (uiState.step) {
          AddAccountStep.Details -> {
            AddAccountDetailsContent(
              uiState = uiState,
              onNicknameChange = onNicknameChange,
              onProviderSelected = onProviderSelected,
              onRulesetSelected = onRulesetSelected,
              onNext = onNext,
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
  modifier: Modifier = Modifier,
) {
  var showProviderSheet by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp).imePadding(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OutlinedTextField(
      value = uiState.nickname,
      onValueChange = onNicknameChange,
      label = { Text("Nickname (optional)") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      modifier = Modifier.fillMaxWidth(),
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
    Spacer(Modifier.weight(1f))
    Button(
      onClick = onNext,
      enabled = uiState.canContinue,
      modifier = Modifier.fillMaxWidth(),
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
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp).imePadding(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OptionDropdownField(
      label = "Type of ISA",
      options = uiState.productTypeOptions,
      selectedId = uiState.selectedProductTypeId,
      onSelected = onProductTypeSelected,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Flexible", style = MaterialTheme.typography.bodyLarge)
        Text(
          text = "Check with your bank whether this ISA is flexible.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(checked = uiState.flexible, onCheckedChange = onFlexibleChange)
    }
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
      Button(
        onClick = onSave,
        enabled = uiState.canSave && !uiState.isSaving,
        modifier = Modifier.weight(1f),
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
      value = selected?.label ?: "",
      onValueChange = {},
      readOnly = true,
      label = { Text("Bank") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdownField(
  label: String,
  options: List<PickerOption>,
  selectedId: String?,
  onSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.id == selectedId }?.label

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedLabel ?: "",
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Column {
              Text(option.label)
              option.description?.let { description ->
                Text(
                  text = description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          onClick = {
            onSelected(option.id)
            expanded = false
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPickerSheet(
  options: List<PickerOption>,
  onSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
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
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      item {
        Column(Modifier.padding(horizontal = 16.dp)) {
          Text("Bank", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search banks") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          )
        }
      }
      items(filtered, key = { it.id }) { option ->
        ListItem(
          headlineContent = { Text(option.label) },
          modifier = Modifier.clickable {
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
      onClose = {},
    )
  }
}
