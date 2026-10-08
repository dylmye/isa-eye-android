package me.dylmye.isa.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews

private data class HelpSection(val title: String, val body: String)

private val HelpSections =
  listOf(
    HelpSection(
      title = "Individual Savings Accounts",
      body =
        "Coming soon.",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Help") }, scrollBehavior = scrollBehavior) },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.padding(innerPadding)
          .padding(horizontal = 16.dp)
          .nestedScroll(scrollBehavior.nestedScrollConnection)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      HelpSections.forEachIndexed { index, section ->
        if (index > 0) HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(section.title, style = MaterialTheme.typography.titleLarge)
          Text(
            section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@FormFactorPreviews
@Composable
private fun HelpScreenPreview() {
  ISAEyeTheme { HelpScreen() }
}
