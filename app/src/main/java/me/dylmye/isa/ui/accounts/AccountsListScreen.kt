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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.ProviderAvatar
import me.dylmye.isa.ui.components.containerTint

private data class SampleAccount(val name: String, val provider: String, val tint: Color)

private val SampleAccounts =
  listOf(
    SampleAccount("Cash ISA 2025/26", "Lloyds Bank", Color(0xFF006A4D)),
    SampleAccount("Stocks & Shares ISA", "Vanguard", Color(0xFF8A1E2D)),
    SampleAccount("Help to Buy ISA", "Nationwide", Color(0xFF004B87)),
    SampleAccount("Junior ISA", "Hargreaves Lansdown", Color(0xFF003A70)),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsListScreen(onAccountClick: (String) -> Unit, modifier: Modifier = Modifier) {
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
      items(SampleAccounts) { account ->
        Surface(
          onClick = { onAccountClick(account.name) },
          shape = MaterialTheme.shapes.large,
          color = account.tint.containerTint(),
          contentColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth(),
        ) {
          ListItem(
            headlineContent = { Text(account.name) },
            supportingContent = { Text(account.provider) },
            leadingContent = { ProviderAvatar(provider = account.provider, tint = account.tint) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
        }
      }
    }
  }
}

@FormFactorPreviews
@Composable
private fun AccountsListScreenPreview() {
  ISAEyeTheme { AccountsListScreen(onAccountClick = {}) }
}
