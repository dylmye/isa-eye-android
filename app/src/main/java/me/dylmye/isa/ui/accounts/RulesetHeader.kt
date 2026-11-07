package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.TooltipIconButton
import me.dylmye.isa.ui.icons.chevronBackward
import me.dylmye.isa.ui.icons.chevronForward
import me.dylmye.isa.ui.icons.quickReference
import me.dylmye.isa.ui.icons.settingsBackupRestore

/**
 * Tax-year context header for the accounts view, styled after Google Health's metrics date picker:
 * the selected ruleset leads, with its notes and the previous / next / reset controls trailing.
 */
@Composable
fun RulesetHeader(
  state: RulesetState,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onReset: () -> Unit,
  onShowNotes: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = state.current.orEmpty()
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
      )
      TooltipIconButton(
        icon = quickReference,
        contentDescription = "Allowances for the $label tax year",
        onClick = onShowNotes,
      )
    }
    TooltipIconButton(
      icon = chevronBackward,
      contentDescription = "Previous tax year",
      onClick = onPrevious,
      enabled = state.canGoBack,
    )
    TooltipIconButton(
      icon = chevronForward,
      contentDescription = "Next tax year",
      onClick = onNext,
      enabled = state.canGoForward,
    )
    TooltipIconButton(
      icon = settingsBackupRestore,
      contentDescription = "Reset to the latest tax year",
      onClick = onReset,
      enabled = state.canReset,
    )
  }
}

@FormFactorPreviews
@Composable
private fun RulesetHeaderPreview() {
  ISAEyeTheme {
    RulesetHeader(
      state = RulesetState(current = "2026/2027", canGoBack = true, canReset = true),
      onPrevious = {},
      onNext = {},
      onReset = {},
      onShowNotes = {},
    )
  }
}
