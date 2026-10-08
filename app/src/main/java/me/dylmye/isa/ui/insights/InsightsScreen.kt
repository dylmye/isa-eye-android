package me.dylmye.isa.ui.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Insights") }, scrollBehavior = scrollBehavior) },
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
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(selected = true, onClick = {}, label = { Text("All accounts") })
        FilterChip(selected = false, onClick = {}, label = { Text("This tax year") })
        FilterChip(selected = false, onClick = {}, label = { Text("Last 12 months") })
      }
      ChartPlaceholder(title = "Balance over time")
      ChartPlaceholder(title = "ISA allowance used")
      Spacer(Modifier.height(88.dp))
    }
  }
}

@Composable
private fun ChartPlaceholder(title: String, modifier: Modifier = Modifier) {
  Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Surface(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            "Chart coming soon",
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
private fun InsightsScreenPreview() {
  ISAEyeTheme { InsightsScreen() }
}
