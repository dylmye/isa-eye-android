package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.components.SkeletonBox
import me.dylmye.isa.ui.components.SkeletonGroup
import me.dylmye.isa.ui.components.SkeletonLine

/** The number of placeholder rows shown while the account list loads. */
private const val LOADING_ROW_COUNT = 5

/**
 * The list's loading state: labelled placeholder rows in the same stack where the real accounts
 * will appear. The placeholders take the row's shape so the list does not jump when data arrives.
 */
@Composable
internal fun AccountsLoading(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
  SkeletonGroup(
    label = "Loading accounts",
    modifier = modifier.fillMaxSize().testTag("robo_accounts_loading"),
  ) {
    LazyColumn(
      contentPadding = contentPadding,
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(
        count = LOADING_ROW_COUNT,
        key = { "loading-account-$it" },
        contentType = { "loading" },
      ) {
        SkeletonAccountRow()
      }
    }
  }
}

/** One placeholder row: the provider avatar, the two labels and the contribution figure. */
@Composable
private fun SkeletonAccountRow() {
  Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.55f), height = 16.dp)
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.3f), height = 12.dp)
      }
      SkeletonLine(modifier = Modifier.width(80.dp), height = 20.dp)
    }
  }
}
