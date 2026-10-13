package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.icons.quickReference
import me.dylmye.isa.ui.icons.trendingDown
import me.dylmye.isa.ui.icons.trendingFlat
import me.dylmye.isa.ui.icons.trendingUp

/** Contributions towards the allowance for the selected year, marked against the previous year. */
@Composable
internal fun ContributionsCard(uiState: AccountDetailUiState, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = contributionLabel(uiState),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // The hero figure scales up to about three times the body heading, shrinking as needed to
        // stay on a single line.
        Text(
          text = formatGbp(uiState.contributionsPence),
          modifier = Modifier.weight(1f, fill = false),
          autoSize =
            TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 84.sp, stepSize = 4.sp),
          style =
            MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.SemiBold,
              lineHeight = TextUnit.Unspecified,
            ),
          maxLines = 1,
        )
        uiState.trend?.let { trend ->
          TrendIndicator(trend = trend, previousRuleset = uiState.previousRuleset)
        }
      }
    }
  }
}

/**
 * Placeholder for the account actions card. It occupies a single grid column, sitting beside the
 * contributions figure on wider layouts and stacking full-width on mobile.
 */
@Composable
internal fun ActionsCardPlaceholder(modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "Actions",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "Coming soon",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Names the year the contributions are for, or "so far" while the latest tax year is in progress. */
private fun contributionLabel(uiState: AccountDetailUiState): String = when {
  uiState.ruleset == null -> "Contributions"
  uiState.isLatestRuleset -> "Contributions so far"
  else -> "Contributions for ${uiState.ruleset.orEmpty()}"
}

/**
 * Direction-only marker for the change in contributions: green when up, error red when down,
 * neutral when level. The spoken description names the year it is compared against.
 */
@Composable
private fun TrendIndicator(
  trend: ContributionTrend,
  previousRuleset: String?,
  modifier: Modifier = Modifier,
) {
  val icon =
    when (trend) {
      ContributionTrend.Up -> trendingUp
      ContributionTrend.Down -> trendingDown
      ContributionTrend.Flat -> trendingFlat
    }
  val tint =
    when (trend) {
      ContributionTrend.Up -> if (isSystemInDarkTheme()) TrendUpDark else TrendUpLight
      ContributionTrend.Down -> MaterialTheme.colorScheme.error
      ContributionTrend.Flat -> MaterialTheme.colorScheme.onSurfaceVariant
    }
  val description =
    when (trend) {
      ContributionTrend.Up -> "Up on ${previousRuleset.orEmpty()}"
      ContributionTrend.Down -> "Down on ${previousRuleset.orEmpty()}"
      ContributionTrend.Flat -> "Unchanged from ${previousRuleset.orEmpty()}"
    }
  Icon(
    imageVector = icon,
    contentDescription = description,
    tint = tint,
    modifier = modifier.size(24.dp),
  )
}

/**
 * Callout explaining what this type of ISA is: titled after the product type, with its body drawn
 * from the type's long description.
 */
@Composable
internal fun InfoCalloutCard(title: String, text: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(imageVector = quickReference, contentDescription = null, modifier = Modifier.size(24.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

/** Titles the callout after the ISA type, e.g. "All about Cash ISAs". */
internal fun infoTitle(typeName: String?): String {
  val base = typeName.orEmpty().removeSuffix(" ISA").trim()
  return if (base.isEmpty()) "All about ISAs" else "All about $base ISAs"
}

private val TrendUpLight = Color(0xFF2E7D32)
private val TrendUpDark = Color(0xFF81C784)
