package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.icons.edit

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
    // Contributions take every column but one, leaving room for the actions card beside them; on a
    // single-column phone the span collapses and every card is full width.
    item(key = "contributions", span = { GridItemSpan((maxLineSpan - 1).coerceAtLeast(1)) }) {
      ContributionsCard(uiState)
    }
    item(key = "actions") { ActionsCard() }
    val description = uiState.longDescription
    if (!description.isNullOrBlank()) {
      item(key = "info", span = { GridItemSpan(maxLineSpan) }) {
        InfoCalloutCard(title = infoTitle(uiState.typeName), text = description)
      }
    }
  }
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
