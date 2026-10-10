package me.dylmye.isa.ui.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * Adaptive shell shared by the add and edit account forms: full-screen on compact widths, a
 * centred card on wider ones. [content] receives the inner padding for its own layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDialogScaffold(
  title: String,
  onClose: () -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable (PaddingValues) -> Unit,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Surface(
      shape = if (isCompact) RectangleShape else MaterialTheme.shapes.extraLarge,
      tonalElevation = if (isCompact) 0.dp else 6.dp,
      modifier =
        if (isCompact) {
          Modifier.fillMaxSize()
        } else {
          Modifier.widthIn(max = 560.dp).fillMaxHeight(0.9f)
        },
    ) {
      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          TopAppBar(
            title = { Text(title) },
            actions = { TextButton(onClick = onClose) { Text("Cancel") } },
          )
        },
        contentWindowInsets =
          if (isCompact) ScaffoldDefaults.contentWindowInsets else WindowInsets(0.dp),
      ) { innerPadding ->
        content(innerPadding)
      }
    }
  }
}
