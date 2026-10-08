package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(name: String, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text(name) }) },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    Box(
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text("Account details", style = MaterialTheme.typography.titleLarge)
        Text(
          "Coming soon",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun AccountDetailScreenPreview() {
  ISAEyeTheme { AccountDetailScreen(name = "Cash ISA 2025/26") }
}
