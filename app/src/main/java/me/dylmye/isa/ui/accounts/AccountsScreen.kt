package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.ProviderAvatar
import me.dylmye.isa.ui.components.containerTint
import java.text.NumberFormat
import java.util.Locale

/** Stateful entry point: owns the ViewModel and hoists its state down into [AccountsContent]. */
@Composable
fun AccountsScreen(onAccountClick: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  AccountsContent(uiState = uiState, onAccountClick = onAccountClick, modifier = modifier)
}

/**
 * Stateless accounts list. Renders [uiState] and reports events upwards; it holds no state of its
 * own, so it can be previewed and tested with any UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsContent(
  uiState: AccountsUiState,
  onAccountClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Accounts") }, scrollBehavior = scrollBehavior) },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding =
      PaddingValues(
        start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
        top = innerPadding.calculateTopPadding(),
        end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
        bottom = innerPadding.calculateBottomPadding() + 88.dp,
      )
    when {
      uiState.isLoading -> AccountsLoading(contentPadding)
      uiState.accounts.isEmpty() -> AccountsEmpty(Modifier.fillMaxSize().padding(contentPadding))
      else -> AccountsList(uiState.accounts, onAccountClick, scrollBehavior, contentPadding)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsList(
  accounts: List<AccountListItemUiState>,
  onAccountClick: (String) -> Unit,
  scrollBehavior: TopAppBarScrollBehavior,
  contentPadding: PaddingValues,
) {
  LazyColumn(
    state = rememberLazyListState(),
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(accounts, key = { it.productId }) { account ->
      AccountRow(account = account, onClick = { onAccountClick(account.name) })
    }
  }
}

@Composable
private fun AccountsLoading(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize().padding(contentPadding),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun AccountRow(
  account: AccountListItemUiState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tint = account.providerColour.toTint()
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = tint.containerTint(),
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier.fillMaxWidth(),
  ) {
    ListItem(
      headlineContent = { Text(account.name) },
      supportingContent = { Text(account.providerName) },
      leadingContent = { ProviderAvatar(provider = account.providerName, tint = tint) },
      trailingContent = { Text(formatGbp(account.balancePence)) },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }
}

@Composable
private fun AccountsEmpty(modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text("Nothing to see here", style = MaterialTheme.typography.titleMedium)
      Text(
        text = "Add an account to get started!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun formatGbp(pence: Long): String {
  val format = NumberFormat.getCurrencyInstance(Locale.UK)
  return format.format(pence / 100.0)
}

private val FallbackTint = Color(0xFF9E9E9E)

private fun String.toTint(): Color = runCatching { Color(toColorInt()) }.getOrDefault(FallbackTint)

@FormFactorPreviews
@Composable
private fun AccountsContentPreview() {
  ISAEyeTheme {
    AccountsContent(uiState = PreviewAccountsUiState, onAccountClick = {})
  }
}

@FormFactorPreviews
@Composable
private fun AccountsEmptyPreview() {
  ISAEyeTheme {
    AccountsContent(uiState = AccountsUiState(), onAccountClick = {})
  }
}

private val PreviewAccountsUiState =
  AccountsUiState(
    accounts =
      listOf(
        AccountListItemUiState("1", "Cash ISA 2025/26", "Lloyds Bank", "#006A4D", 125_000),
        AccountListItemUiState("2", "Stocks & Shares ISA", "Vanguard", "#8A1E2D", 432_050),
        AccountListItemUiState("3", "Help to Buy ISA", "Nationwide", "#004B87", 0),
        AccountListItemUiState("4", "Junior ISA", "Hargreaves Lansdown", "#003A70", 0),
      ),
  )
