package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
  AccountDetailContent(name = uiState.name, onEdit = onEdit, modifier = modifier)
}

/** Stateless account detail pane. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailContent(name: String?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(name.orEmpty()) },
        actions = {
          IconButton(onClick = onEdit) {
            Icon(edit, contentDescription = "Edit account")
          }
        },
      )
    },
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
private fun AccountDetailContentPreview() {
  ISAEyeTheme { AccountDetailContent(name = "Cash ISA", onEdit = {}) }
}
