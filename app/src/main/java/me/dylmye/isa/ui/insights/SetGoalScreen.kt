package me.dylmye.isa.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.notifications.rememberNotificationPermissionRequest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.LoadingIndicator
import me.dylmye.isa.ui.formatGbp

/** Stateful entry point: owns the goal ViewModel and hoists its state into [SetGoalContent]. */
@Composable
fun SetGoalScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: SetGoalViewModel = viewModel(factory = SetGoalViewModel.factory())
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val requestNotifications = rememberNotificationPermissionRequest()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      // Clearing a goal is not a moment to ask; saving one is, because it is what the monthly
      // reminder is for.
      if (event == SetGoalEvent.Saved) requestNotifications()
      onClose()
    }
  }

  SetGoalContent(
    uiState = uiState,
    onAmountChange = viewModel::onAmountChange,
    onSave = viewModel::save,
    onRemove = viewModel::remove,
    onClose = onClose,
    modifier = modifier,
  )
}

/**
 * Stateless goal dialog. One field does not warrant a page, so this is a centred card at every
 * width rather than the full-screen treatment the multi-field account forms get. The card is capped
 * so it still reads as a dialog on a tablet, and scrolls rather than overflowing a short window.
 */
@Composable
fun SetGoalContent(
  uiState: SetGoalUiState,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  onRemove: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val title = if (uiState.savedPence == null) "Set a goal" else "Edit goal"
  Box(
    modifier = modifier.fillMaxSize().imePadding().padding(DIALOG_MARGIN),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
      modifier = Modifier.semantics { paneTitle = title }.widthIn(max = DIALOG_MAX_WIDTH),
    ) {
      if (uiState.isLoading) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(DIALOG_PADDING),
          contentAlignment = Alignment.Center,
        ) {
          LoadingIndicator(label = "Loading goal")
        }
      } else {
        SetGoalDialog(
          uiState = uiState,
          onAmountChange = onAmountChange,
          onSave = onSave,
          onRemove = onRemove,
          onClose = onClose,
        )
      }
    }
  }
}

@Composable
private fun SetGoalDialog(
  uiState: SetGoalUiState,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  onRemove: () -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(DIALOG_PADDING),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = if (uiState.savedPence == null) "Set a goal" else "Edit goal",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.semantics { heading() },
    )
    // The goal is the whole dialog, so the amount is set as a headline figure rather than the body
    // size a form field would use.
    OutlinedTextField(
      value = uiState.input,
      onValueChange = onAmountChange,
      label = { Text(goalLabel(uiState)) },
      prefix = { Text("£", style = MaterialTheme.typography.headlineMedium) },
      textStyle = MaterialTheme.typography.headlineMedium,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = uiState.error != null,
      supportingText = { Text(uiState.error ?: allowanceHint(uiState)) },
      modifier = Modifier.fillMaxWidth(),
    )
    // Low stakes and reversible, so removing needs no confirmation of its own.
    if (uiState.canRemove) {
      TextButton(onClick = onRemove) { Text("Remove goal") }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
      TextButton(onClick = onClose) { Text("Cancel") }
      Button(onClick = onSave, enabled = uiState.canSave) { Text("Save") }
    }
  }
}

/** The year the goal belongs to, when one is running. */
private fun goalLabel(uiState: SetGoalUiState): String =
  uiState.ruleset?.let { "Goal for $it" } ?: "Goal"

/** The cap the field accepts, stated before an entry goes over it rather than only after. */
private fun allowanceHint(uiState: SetGoalUiState): String =
  uiState.allowancePence?.let { "Up to this year's ${formatGbp(it)} allowance." }
    ?: "Your goal for the tax year."

@FormFactorPreviews
@Composable
private fun SetGoalContentPreview() {
  ISAEyeTheme {
    SetGoalContent(
      uiState = PreviewSetGoal,
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

private val PreviewSetGoal =
  SetGoalUiState(
    ruleset = "2026/2027",
    allowancePence = 2_000_000,
    input = "12000",
    savedPence = 1_200_000,
    isLoading = false,
  )

private val DIALOG_MARGIN = 24.dp
private val DIALOG_MAX_WIDTH = 420.dp
private val DIALOG_PADDING = 24.dp
