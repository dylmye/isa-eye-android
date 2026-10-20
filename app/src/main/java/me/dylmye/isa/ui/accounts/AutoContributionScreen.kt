package me.dylmye.isa.ui.accounts

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.time.formatLongDate
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.formatGbp

/** Stateful entry point: owns the auto-contribution ViewModel and hoists its state down. */
@Composable
fun AutoContributionScreen(productId: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AutoContributionViewModel =
    viewModel(factory = AutoContributionViewModel.factory(productId))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        AutoContributionEvent.Saved -> onClose()
      }
    }
  }

  AutoContributionContent(
    uiState = uiState,
    onAmountChange = viewModel::onAmountChange,
    onSave = viewModel::save,
    onRemove = viewModel::remove,
    onClose = onClose,
    modifier = modifier,
  )
}

/**
 * Stateless auto-contribution dialog. One field, so it is a centred card at every width rather than
 * the full-screen treatment the multi-field account forms get, and it scrolls instead of overflowing
 * a short window.
 */
@Composable
fun AutoContributionContent(
  uiState: AutoContributionUiState,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  onRemove: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().imePadding().padding(DIALOG_MARGIN),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
      modifier = Modifier.widthIn(max = DIALOG_MAX_WIDTH),
    ) {
      if (uiState.isLoading) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(DIALOG_PADDING),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        AutoContributionDialog(
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
private fun AutoContributionDialog(
  uiState: AutoContributionUiState,
  onAmountChange: (String) -> Unit,
  onSave: () -> Unit,
  onRemove: () -> Unit,
  onClose: () -> Unit,
) {
  val isNew = uiState.savedMonthlyPence == null
  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(DIALOG_PADDING),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = if (isNew) "Set up auto-contribution" else "Edit auto-contribution",
      style = MaterialTheme.typography.headlineSmall,
    )
    // The amount is the whole point of the dialog, so it is set as a headline figure rather than the
    // body size a form field would use.
    OutlinedTextField(
      value = uiState.input,
      onValueChange = onAmountChange,
      label = { Text("Monthly amount") },
      prefix = { Text("£", style = MaterialTheme.typography.headlineMedium) },
      textStyle = MaterialTheme.typography.headlineMedium,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = uiState.error != null,
      supportingText = {
        Text(uiState.error ?: "Recorded automatically on the 1st of each month.")
      },
      modifier = Modifier.fillMaxWidth(),
    )
    ScheduleLines(uiState)
    // Low stakes and reversible, so turning it off needs no confirmation of its own.
    if (uiState.canRemove) {
      TextButton(onClick = onRemove) { Text("Turn off") }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
      TextButton(onClick = onClose) { Text("Cancel") }
      Button(onClick = onSave, enabled = uiState.canSave) {
        Text(if (isNew) "Set up" else "Save")
      }
    }
  }
}

/** What the schedule would do: how many credits, how much, and when they start and end. */
@Composable
private fun ScheduleLines(uiState: AutoContributionUiState) {
  val first = uiState.firstCreditDate?.let(::formatLongDate)
  val last = uiState.lastCreditDate?.let(::formatLongDate)
  val entered = uiState.enteredPence
  val total = uiState.projectedTotalPence
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    when {
      uiState.credits == 0 -> {
        Text(
          text = "No months are left in this tax year.",
          style = MaterialTheme.typography.bodyMedium,
          color = muted,
        )
      }

      entered != null && total != null -> {
        val summary = "${formatGbp(entered)} a month, ${formatGbp(total)} in total."
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = muted)
        if (first != null && last != null) {
          Text(
            text = "First on $first, last on $last.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
          )
        }
      }

      first != null && last != null -> {
        Text(
          text = "First on $first, last on $last.",
          style = MaterialTheme.typography.bodyMedium,
          color = muted,
        )
      }
    }
  }
}

@FormFactorPreviews
@Composable
private fun AutoContributionContentPreview() {
  ISAEyeTheme {
    AutoContributionContent(
      uiState = PreviewAutoContribution,
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

private val PreviewAutoContribution =
  AutoContributionUiState(
    ruleset = "2026/2027",
    input = "500",
    savedMonthlyPence = 50_000,
    firstCreditDate = "2026-11-01",
    lastCreditDate = "2027-04-01",
    credits = 6,
    isLoading = false,
  )

private val DIALOG_MARGIN = 24.dp
private val DIALOG_MAX_WIDTH = 420.dp
private val DIALOG_PADDING = 24.dp
