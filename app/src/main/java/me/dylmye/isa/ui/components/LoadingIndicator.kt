package me.dylmye.isa.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * An indeterminate progress indicator with a spoken label. A bare spinner is silent to a screen
 * reader, which cannot tell whether the screen is loading, saving or waiting on something else.
 */
@Composable
fun LoadingIndicator(label: String, modifier: Modifier = Modifier) {
  CircularProgressIndicator(modifier = modifier.semantics { contentDescription = label })
}
