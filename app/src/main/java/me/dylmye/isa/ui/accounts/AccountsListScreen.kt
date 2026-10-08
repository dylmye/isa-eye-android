package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.ProviderAvatar
import me.dylmye.isa.ui.components.containerTint

@Composable
fun AccountsRoute(onAccountClick: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory)
  val accounts by viewModel.accounts.collectAsStateWithLifecycle()
  AccountsListScreen(accounts = accounts, onAccountClick = onAccountClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsListScreen(
  accounts: List<AccountSummary>,
  onAccountClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(title = { Text("Accounts") }, scrollBehavior = scrollBehavior)
    },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
      state = rememberLazyListState(),
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentPadding =
        PaddingValues(
          start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
          top = innerPadding.calculateTopPadding(),
          end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
          bottom = innerPadding.calculateBottomPadding() + 88.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(accounts, key = { it.productId }) { account ->
        val tint = account.providerColour.toTint()
        Surface(
          onClick = { onAccountClick(account.friendlyName) },
          shape = MaterialTheme.shapes.large,
          color = tint.containerTint(),
          contentColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          ListItem(
            headlineContent = { Text(account.friendlyName) },
            supportingContent = { Text(account.providerName) },
            leadingContent = { ProviderAvatar(provider = account.providerName, tint = tint) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
        }
      }
    }
  }
}

private val FallbackTint = Color(0xFF9E9E9E)

private fun String.toTint(): Color = runCatching { Color(toColorInt()) }.getOrDefault(FallbackTint)

@FormFactorPreviews
@Composable
private fun AccountsListScreenPreview() {
  ISAEyeTheme {
    AccountsListScreen(accounts = PreviewAccounts, onAccountClick = {})
  }
}

private val PreviewAccounts =
  listOf(
    AccountSummary("1", "Cash ISA 2025/26", "cash", "lloyds", "Lloyds Bank", "#006A4D"),
    AccountSummary("2", "Stocks & Shares ISA", "stocks", "vanguard", "Vanguard", "#8A1E2D"),
    AccountSummary("3", "Help to Buy ISA", "htb", "nationwide", "Nationwide", "#004B87"),
    AccountSummary("4", "Junior ISA", "junior", "hl", "Hargreaves Lansdown", "#003A70"),
  )
