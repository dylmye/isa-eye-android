package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.icons.chevronBackward
import me.dylmye.isa.ui.icons.chevronForward
import me.dylmye.isa.ui.icons.quickReference
import me.dylmye.isa.ui.icons.settingsBackupRestore

/**
 * Tax-year context header for the accounts view, styled after Google Health's metrics date picker:
 * the selected ruleset leads, with its notes and the previous / next / reset controls trailing.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
      Text(text = label, style = MaterialTheme.typography.titleMedium)
      // Icon-only, so it carries a hover label as well as its content description.
      TooltipBox(
        positionProvider =
          TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text("Allowances for $label") } },
        state = rememberTooltipState(),
      ) {
        IconButton(onClick = onShowNotes) {
          Icon(quickReference, contentDescription = "Allowances for $label")
        }
      }
    }
    IconButton(onClick = onPrevious, enabled = state.canGoBack) {
      Icon(chevronBackward, contentDescription = "Previous ruleset")
    }
    IconButton(onClick = onNext, enabled = state.canGoForward) {
      Icon(chevronForward, contentDescription = "Next ruleset")
    }
    IconButton(onClick = onReset, enabled = state.canReset) {
      Icon(settingsBackupRestore, contentDescription = "Reset to current ruleset")
    }
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
