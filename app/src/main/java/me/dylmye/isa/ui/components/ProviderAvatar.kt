package me.dylmye.isa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Circular avatar for an account provider: the provider's icon when a URL is available, otherwise
 * its initial on a tinted circle.
 */
@Composable
fun ProviderAvatar(
  provider: String,
  tint: Color,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  iconUrl: String? = null,
) {
  val avatarModifier = modifier.size(size).clip(CircleShape)
  if (iconUrl.isNullOrBlank()) {
    LetterAvatar(provider = provider, tint = tint, modifier = avatarModifier)
  } else {
    SubcomposeAsyncImage(
      model = iconUrl,
      contentDescription = provider,
      modifier = avatarModifier,
      contentScale = ContentScale.Crop,
      loading = { LetterAvatar(provider = provider, tint = tint) },
      error = { LetterAvatar(provider = provider, tint = tint) },
    )
  }
}

/** Fallback avatar: the provider's initial on a circle filled with its brand colour. */
@Composable
private fun LetterAvatar(provider: String, tint: Color, modifier: Modifier = Modifier) {
  val initial = provider.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
  Box(
    modifier = modifier.fillMaxSize().clip(CircleShape).background(tint),
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
