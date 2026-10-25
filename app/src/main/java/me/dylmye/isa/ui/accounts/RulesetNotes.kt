package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FINANCIAL_DISCLAIMER
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.formatGbp

/**
 * The tax year's notes, in the container that suits the width: a bottom sheet on compact widths,
 * where it is thumb-reachable and drags away, and a centred dialog card on wider ones. Both show the
 * same [RulesetNotesBody].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesetNotesSurface(
  notes: RulesetNotesUiState,
  isCompact: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isCompact) {
    ModalBottomSheet(
      modifier = modifier.semantics { paneTitle = "Tax year notes" },
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      RulesetNotesBody(notes)
    }
  } else {
    Dialog(
      onDismissRequest = onDismiss,
      properties =
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
      Surface(
        modifier =
          modifier
            .semantics { paneTitle = "Tax year notes" }
            .widthIn(max = 560.dp)
            .heightIn(max = 560.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
      ) {
        Column {
          // The card wraps short notes but caps long ones, scrolling the body so Close stays put.
          RulesetNotesBody(notes = notes, modifier = Modifier.weight(1f, fill = false))
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = onDismiss) { Text("Close") }
          }
        }
      }
    }
  }
}

/** The year's changelog, its shared allowance and any per-type exceptions, then the disclaimer. */
@Composable
fun RulesetNotesBody(notes: RulesetNotesUiState, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "Notes for ${notes.ruleset.orEmpty()} tax year",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.semantics { heading() },
    )
    Text(
      text = notes.notes ?: "No notes for this tax year.",
      style = MaterialTheme.typography.bodyMedium,
    )
    AllowanceRow(
      label = "Main allowance",
      amountPence = notes.sharedAllowancePence ?: 0L,
      emphasised = true,
    )
    notes.exceptions.forEach { exception ->
      AllowanceRow(
        label = exception.name,
        amountPence = exception.allowancePence,
        note = if (exception.includedInShared) null else "Additional to main allowance",
      )
    }
    Text(
      text = FINANCIAL_DISCLAIMER,
      style = MaterialTheme.typography.bodySmall,
      fontStyle = FontStyle.Italic,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp),
    )
  }
}

/** A labelled figure, with an optional note beneath it qualifying how the allowance applies. */
@Composable
private fun AllowanceRow(
  label: String,
  amountPence: Long,
  modifier: Modifier = Modifier,
  note: String? = null,
  emphasised: Boolean = false,
) {
  Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = formatGbp(amountPence),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (emphasised) FontWeight.SemiBold else null,
      )
    }
    if (note != null) {
      Text(
        text = note,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@FormFactorPreviews
@Composable
private fun RulesetNotesBodyPreview() {
  ISAEyeTheme {
    RulesetNotesBody(PreviewNotesUiState)
  }
}

@FormFactorPreviews
@Composable
private fun RulesetNotesBodyWithoutNotesPreview() {
  ISAEyeTheme {
    RulesetNotesBody(PreviewNotesUiState.copy(notes = null, exceptions = emptyList()))
  }
}

private val PreviewNotesUiState =
  RulesetNotesUiState(
    ruleset = "2026/2027",
    notes =
      "Long-Term Asset Funds (LTAFs) must now be held in Stocks & Shares ISAs only, not " +
        "Innovative Finance ISAs. Crypto Exchange Traded Notes (c-ETNs) must be held in " +
        "Innovative Finance ISAs only. There were no changes to allowances.",
    sharedAllowancePence = 2_000_000,
    exceptions =
      listOf(
        RulesetExceptionUiState("Lifetime ISA", 400_000, includedInShared = true),
        RulesetExceptionUiState("Junior ISA", 900_000, includedInShared = false),
      ),
  )
