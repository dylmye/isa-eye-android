package me.dylmye.isa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular avatar showing the initial of an account provider, tinted with its brand colour.
 *
 * The letter colour is chosen automatically to stay legible against the tint.
 */
@Composable
fun ProviderAvatar(provider: String, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
  val initial = provider.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
  Box(
    modifier = modifier.size(size).clip(CircleShape).background(tint),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      color = tint.onColor(),
      style = MaterialTheme.typography.titleMedium,
    )
  }
}

/** Picks a legible foreground colour for text drawn on top of [this]. */
private fun Color.onColor(): Color = if (luminance() > 0.5f) Color.Black else Color.White

/**
 * A soft, translucent variant of a provider tint for colouring related surfaces (such as account
 * cards) so they read as belonging to the same provider as the avatar.
 */
fun Color.containerTint(alpha: Float = 0.18f): Color = copy(alpha = alpha)
