package me.dylmye.isa.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

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
    // A painter rather than SubcomposeAsyncImage: the latter defers composition to the measurement
    // pass, which every row in the accounts list would pay for while scrolling.
    val painter = rememberAsyncImagePainter(iconUrl)
    val state by painter.state.collectAsState()
    if (state is AsyncImagePainter.State.Success) {
      Image(
        painter = painter,
        contentDescription = provider,
        modifier = avatarModifier,
        contentScale = ContentScale.Crop,
      )
    } else {
      // While the icon loads, or if it fails, the initial stands in for it.
      LetterAvatar(provider = provider, tint = tint, modifier = avatarModifier)
    }
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

/**
 * [containerTint] with its transparency resolved against [background], so a surface painted with
 * it hides whatever lies underneath. Used while a card is lifted out of the list: a translucent
 * card would let the cards it passes over show through and mix with its colour.
 */
fun Color.opaqueContainerTint(background: Color): Color = containerTint().compositeOver(background)
