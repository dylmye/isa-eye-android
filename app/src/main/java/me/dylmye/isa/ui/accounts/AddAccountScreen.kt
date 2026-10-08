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
fun AddAccountScreen(modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Add account") }) },
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
        Text("New account form", style = MaterialTheme.typography.titleLarge)
        Text(
          "Coming soon",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@FormFactorPreviews
@Composable
private fun AddAccountScreenPreview() {
  ISAEyeTheme { AddAccountScreen() }
}
