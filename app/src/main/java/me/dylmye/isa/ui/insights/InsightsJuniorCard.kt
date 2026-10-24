package me.dylmye.isa.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.accounts.JuniorAllowanceUiState
import me.dylmye.isa.ui.formatGbp

/**
 * The year's Junior ISA allowance. The limit is per child and sits outside the shared allowance, so
 * each open Junior account gets a bar of its own.
 */
@Composable
internal fun JuniorAllowanceCard(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  val allowances = uiState.juniorAllowances
  if (allowances.isEmpty()) return
  val typeName = allowances.first().typeName.ifBlank { "Junior ISA" }
  val title = if (allowances.size == 1) "$typeName allowance" else "$typeName allowances"
  InsightsCard(title = title, modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      allowances.forEach { allowance ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          if (allowances.size > 1) {
            Text(allowance.accountName, style = MaterialTheme.typography.titleSmall)
          }
          JuniorAllowanceSection(allowance)
        }
      }
    }
  }
}

/** One Junior account's bar, its used-of-allowance figure and what is left. */
@Composable
private fun JuniorAllowanceSection(allowance: JuniorAllowanceUiState) {
  LinearProgressIndicator(
    progress = { allowance.fraction },
    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
    gapSize = 0.dp,
    drawStopIndicator = {},
  )
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(
      text = "${formatGbp(allowance.usedPence)} of ${formatGbp(allowance.allowancePence)}",
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text =
        percentOf(
          allowance.usedPence.coerceAtMost(allowance.allowancePence),
          allowance.allowancePence,
        ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  Text(
    text =
      if (allowance.isUsedUp) {
        "Allowance used up for this year."
      } else {
        "${formatGbp(allowance.remainingPence)} left this year."
      },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
