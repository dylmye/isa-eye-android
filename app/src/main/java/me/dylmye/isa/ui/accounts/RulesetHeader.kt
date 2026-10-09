package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.icons.chevronBackward
import me.dylmye.isa.ui.icons.chevronForward
import me.dylmye.isa.ui.icons.settingsBackupRestore

/**
 * Tax-year context header for the accounts view, styled after Google Health's metrics date picker:
 * the selected ruleset leads, with previous / next / reset controls trailing.
 */
@Composable
fun RulesetHeader(
  state: RulesetState,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = state.current.orEmpty(),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.weight(1f),
    )
    IconButton(onClick = onPrevious, enabled = state.canGoBack) {
      Icon(chevronBackward, contentDescription = "Previous ruleset")
    }
    IconButton(onClick = onNext, enabled = state.canGoForward) {
      Icon(chevronForward, contentDescription = "Next ruleset")
    }
    IconButton(onClick = onReset) {
      Icon(settingsBackupRestore, contentDescription = "Reset to current ruleset")
    }
  }
}

@FormFactorPreviews
@Composable
private fun RulesetHeaderPreview() {
  ISAEyeTheme {
    RulesetHeader(
      state = RulesetState(current = "2026/2027", canGoBack = true),
      onPrevious = {},
      onNext = {},
      onReset = {},
    )
  }
}
