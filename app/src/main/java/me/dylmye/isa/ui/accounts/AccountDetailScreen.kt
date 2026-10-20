package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.InfoCalloutCard
import me.dylmye.isa.ui.icons.arrowBack
import me.dylmye.isa.ui.icons.edit

/** Stateful entry point: owns the detail ViewModel and surfaces the back and edit actions. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AccountDetailScreen(
  productId: String,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onUpdateContributions: () -> Unit,
  onSetUpAutoContribution: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: AccountDetailViewModel =
    viewModel(factory = AccountDetailViewModel.factory(productId))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var confirmDelete by rememberSaveable { mutableStateOf(false) }

  // Deleting removes the pane's subject, so it leaves the back stack rather than rendering an
  // account that no longer exists.
  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        AccountDetailEvent.Deleted -> onBack()
      }
    }
  }

  // A pane that has the list beside it needs no way back; a lone one does. This asks the same
  // question of the same directive that the list-detail scene strategy is built with.
  val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
  val showBackButton =
    remember(windowAdaptiveInfo) {
      calculatePaneScaffoldDirective(windowAdaptiveInfo).maxHorizontalPartitions == 1
    }
  AccountDetailContent(
    uiState = uiState,
    showBackButton = showBackButton,
    onBack = onBack,
    onEdit = onEdit,
    onUpdateContributions = onUpdateContributions,
    onSetUpAutoContribution = onSetUpAutoContribution,
    onDeleteAccount = { confirmDelete = true },
    modifier = modifier,
  )
  if (confirmDelete) {
    DeleteAccountDialog(
      accountName = uiState.name.orEmpty(),
      onConfirm = {
        confirmDelete = false
        viewModel.delete()
      },
      onDismiss = { confirmDelete = false },
    )
  }
}

/** Confirms the one action in the app that cannot be undone. */
@Composable
private fun DeleteAccountDialog(
  accountName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val name = accountName.ifBlank { "this account" }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete this account?") },
    text = { Text("This removes $name and everything recorded against it. It can't be undone.") },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) {
        Text("Delete")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

/**
 * Stateless account detail pane. The cards sit in an adaptive grid so a phone shows one column and a
 * tablet or desktop shows as many as fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailContent(
  uiState: AccountDetailUiState,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onUpdateContributions: () -> Unit,
  onSetUpAutoContribution: () -> Unit,
  onDeleteAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(uiState.name.orEmpty()) },
        navigationIcon = {
          if (showBackButton) {
            IconButton(onClick = onBack) {
              Icon(arrowBack, contentDescription = "Back to accounts")
            }
          }
        },
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
      onUpdateContributions = onUpdateContributions,
      onSetUpAutoContribution = onSetUpAutoContribution,
      onDeleteAccount = onDeleteAccount,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailGrid(
  uiState: AccountDetailUiState,
  contentPadding: PaddingValues,
  scrollBehavior: TopAppBarScrollBehavior,
  onUpdateContributions: () -> Unit,
  onSetUpAutoContribution: () -> Unit,
  onDeleteAccount: () -> Unit,
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
    item(key = "actions") {
      ActionsCard(
        autoContributionEnabled = uiState.isCurrentTaxYear,
        hasAutoContribution = uiState.hasAutoContribution,
        onUpdateContributions = onUpdateContributions,
        onSetUpAutoContribution = onSetUpAutoContribution,
        onDeleteAccount = onDeleteAccount,
      )
    }
    val description = uiState.longDescription
    if (!description.isNullOrBlank()) {
      item(key = "info", span = { GridItemSpan(maxLineSpan) }) {
        InfoCalloutCard(
          text = AnnotatedString(description),
          title = infoTitle(uiState.typeName),
        )
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
          lastUpdatedLabel = "3 days ago",
          isCurrentTaxYear = true,
          hasAutoContribution = true,
        ),
      showBackButton = true,
      onBack = {},
      onEdit = {},
      onUpdateContributions = {},
      onSetUpAutoContribution = {},
      onDeleteAccount = {},
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
          lastUpdatedLabel = "4 Jan 2025",
        ),
      showBackButton = false,
      onBack = {},
      onEdit = {},
      onUpdateContributions = {},
      onSetUpAutoContribution = {},
      onDeleteAccount = {},
    )
  }
}
