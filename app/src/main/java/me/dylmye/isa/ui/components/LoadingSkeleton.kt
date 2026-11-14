package me.dylmye.isa.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The pulse shared by every block in a skeleton. It is a single animation for the whole screen
 * rather than one per block, and each block reads it from the draw phase, so the pulse invalidates
 * drawing without recomposing or re-laying out the placeholders.
 */
private val LocalSkeletonPulse = staticCompositionLocalOf<State<Float>> { mutableStateOf(1f) }

private const val PULSE_MIN = 0.45f
private const val PULSE_DURATION_MILLIS = 900

/**
 * Wraps a loading region so its placeholders share one pulse and a screen reader hears [label]
 * rather than a tree of empty boxes. The placeholder blocks are decorative and cleared from the
 * semantics tree.
 */
@Composable
fun SkeletonGroup(
  label: String,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val pulse = rememberSkeletonPulse()
  CompositionLocalProvider(LocalSkeletonPulse provides pulse) {
    Box(
      modifier = modifier.clearAndSetSemantics { contentDescription = label },
      content = content,
    )
  }
}

/** A single placeholder block, drawn with the current theme's variant surface. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
  val colour = MaterialTheme.colorScheme.outlineVariant
  val pulse = LocalSkeletonPulse.current
  Box(
    modifier =
      modifier.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, colour.copy(alpha = pulse.value)) }
      },
  )
}

/**
 * A line of placeholder text. The height is the line's own, not a minimum: a caller that wants a
 * paragraph gives it the height of the paragraph's line box.
 */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, height: Dp = 16.dp) {
  SkeletonBox(modifier = modifier.height(height), shape = CircleShape)
}

/** A card-shaped placeholder with the same padding and gaps as the real cards it stands in for. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
    )
  }
}

/**
 * The pulse is disabled while a preview or screenshot is rendered, so a golden image is the same
 * on every run instead of catching the animation at an arbitrary point.
 */
@Composable
private fun rememberSkeletonPulse(): State<Float> = if (LocalInspectionMode.current) {
  remember { mutableStateOf(1f) }
} else {
  rememberInfiniteTransition(label = "skeletonPulse").animateFloat(
    initialValue = PULSE_MIN,
    targetValue = 1f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(PULSE_DURATION_MILLIS, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "skeletonAlpha",
  )
}
