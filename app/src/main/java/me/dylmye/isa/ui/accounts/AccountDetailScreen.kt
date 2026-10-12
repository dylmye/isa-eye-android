package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.icons.edit
import me.dylmye.isa.ui.icons.quickReference
import me.dylmye.isa.ui.icons.trendingDown
import me.dylmye.isa.ui.icons.trendingFlat
import me.dylmye.isa.ui.icons.trendingUp

/** Stateful entry point: owns the detail ViewModel and surfaces the edit action. */
@Composable
fun AccountDetailScreen(productId: String, onEdit: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AccountDetailViewModel =
    viewModel(factory = AccountDetailViewModel.factory(productId))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  AccountDetailContent(uiState = uiState, onEdit = onEdit, modifier = modifier)
}

/**
 * Stateless account detail pane. The cards sit in an adaptive grid so a phone shows one column and a
 * tablet or desktop shows as many as fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailContent(
  uiState: AccountDetailUiState,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(uiState.name.orEmpty()) },
        actions = {
          IconButton(onClick = onEdit) {
            Icon(edit, contentDescription = "Edit account")
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    val contentPadding =
      PaddingValues(
        start = 16.dp,
        top = innerPadding.calculateTopPadding(),
        end = 16.dp,
        bottom = innerPadding.calculateBottomPadding() + 88.dp,
      )
    AccountDetailGrid(
      uiState = uiState,
      contentPadding = contentPadding,
      scrollBehavior = scrollBehavior,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailGrid(
  uiState: AccountDetailUiState,
  contentPadding: PaddingValues,
  scrollBehavior: TopAppBarScrollBehavior,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 320.dp),
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "contributions") { ContributionsCard(uiState) }
    val description = uiState.longDescription
    if (!description.isNullOrBlank()) {
      item(key = "info") {
        InfoCalloutCard(title = infoTitle(uiState.typeName), text = description)
      }
    }
  }
}

/**
 * Callout explaining what this type of ISA is: titled after the product type, with its body drawn
 * from the type's long description.
 */
@Composable
private fun InfoCalloutCard(title: String, text: String, modifier: Modifier = Modifier) {
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
private fun infoTitle(typeName: String?): String {
  val base = typeName.orEmpty().removeSuffix(" ISA").trim()
  return if (base.isEmpty()) "All about ISAs" else "All about $base ISAs"
}

/** Contributions towards the allowance for the selected year, marked against the previous year. */
@Composable
private fun ContributionsCard(uiState: AccountDetailUiState, modifier: Modifier = Modifier) {
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
        Text(
          text = formatGbp(uiState.contributionsPence),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.SemiBold,
        )
        uiState.trend?.let { trend ->
          TrendIndicator(trend = trend, previousRuleset = uiState.previousRuleset)
        }
      }
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

@Composable
fun AccountDetailPlaceholder(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      "Select an account to see its details",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private val TrendUpLight = Color(0xFF2E7D32)
private val TrendUpDark = Color(0xFF81C784)

@FormFactorPreviews
@Composable
private fun AccountDetailContentPreview() {
  ISAEyeTheme {
    AccountDetailContent(
      uiState =
        AccountDetailUiState(
          name = "Cash ISA",
          typeName = "Cash ISA",
          longDescription =
            "A deposit account, where your cash is guaranteed. The rules for accessing the cash " +
              "once deposited may differ by account.",
          contributionsPence = 125_000,
          ruleset = "2026/2027",
          previousRuleset = "2025/2026",
          isLatestRuleset = true,
          trend = ContributionTrend.Up,
        ),
      onEdit = {},
    )
  }
}

@FormFactorPreviews
@Composable
private fun AccountDetailContentEarlierYearPreview() {
  ISAEyeTheme {
    AccountDetailContent(
      uiState =
        AccountDetailUiState(
          name = "Stocks & Shares ISA",
          contributionsPence = 432_050,
          ruleset = "2024/2025",
          previousRuleset = "2023/2024",
          isLatestRuleset = false,
          trend = ContributionTrend.Down,
        ),
      onEdit = {},
    )
  }
}
