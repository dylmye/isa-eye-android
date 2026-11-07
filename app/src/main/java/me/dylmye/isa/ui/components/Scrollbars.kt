package me.dylmye.isa.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Draws an overlay scrollbar thumb over a lazy list or grid.
 *
 * A fine pointer is required: [hoverable] only reports a mouse or stylus, so a touch-only device
 * never shows the thumb and its content stays unobstructed. Pointer users get the thumb on hover,
 * while wheel or trackpad scrolling keeps it awake. It fades out after [fadeDelayMillis] without
 * either, so it does not sit over the content permanently.
 *
 * [topInset] keeps the track below an app bar that the scrollable draws behind. The thumb's
 * position and length come from the scrollable's [ScrollIndicatorState], which reports the
 * estimated content size and the measured viewport for both lazy lists and lazy grids.
 */
@Composable
fun Modifier.verticalScrollbar(
  state: ScrollIndicatorState?,
  topInset: Dp = 0.dp,
  fadeDelayMillis: Int = 1_000,
): Modifier {
  if (state == null) return this

  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()
  val alpha = remember { Animatable(0f) }
  var activity by remember { mutableIntStateOf(0) }
  // A mouse or stylus has been used, so scrolling from now on may be a wheel or trackpad rather
  // than a finger. Touch-only devices never set this and so never show the thumb.
  var hasFinePointer by remember { mutableStateOf(false) }

  val thumbColor = MaterialTheme.colorScheme.outline.copy(alpha = THUMB_OPACITY)
  val thickness = with(density) { THUMB_THICKNESS.toPx() }
  val minThumbLength = with(density) { THUMB_MIN_LENGTH.toPx() }
  val topInsetPx = with(density) { topInset.toPx() }

  // Entering or moving a fine pointer over the content brings the thumb in.
  LaunchedEffect(hovered) {
    if (hovered) {
      hasFinePointer = true
      activity++
    }
  }
  // Scrolling after a fine pointer has been seen keeps it in, including wheel and trackpad
  // scrolling, and any accessibility or animated scroll that changes the offset.
  LaunchedEffect(state) {
    var previousOffset = state.scrollOffset
    snapshotFlow { state.scrollOffset }.collect { offset ->
      if (offset != previousOffset) {
        previousOffset = offset
        if (hasFinePointer) activity++
      }
    }
  }
  LaunchedEffect(activity) {
    if (activity == 0) return@LaunchedEffect
    alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 100))
    delay(fadeDelayMillis.toLong())
    alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 250))
  }

  return this
    .hoverable(interactionSource)
    .pointerInput(Unit) {
      awaitPointerEventScope {
        trackFinePointerActivity {
          hasFinePointer = true
          activity++
        }
      }
    }
    .drawWithContent {
      drawContent()
      drawScrollbarThumb(
        state = state,
        alpha = alpha.value,
        topInset = topInsetPx,
        thickness = thickness,
        thumbColor = thumbColor,
        minThumbHeight = minThumbLength,
        isRtl = layoutDirection == LayoutDirection.Rtl,
      )
    }
}

/** Keeps the thumb awake while a mouse or stylus is used over the content. */
private suspend fun AwaitPointerEventScope.trackFinePointerActivity(onActivity: () -> Unit) {
  while (true) {
    val event = awaitPointerEvent(PointerEventPass.Initial)
    val finePointer =
      event.changes.any {
        it.type == PointerType.Mouse || it.type == PointerType.Stylus
      }
    if (finePointer) onActivity()
  }
}

/** Draws the thumb over already-drawn content, within the part of the track below [topInset]. */
private fun DrawScope.drawScrollbarThumb(
  state: ScrollIndicatorState,
  alpha: Float,
  topInset: Float,
  thickness: Float,
  thumbColor: Color,
  minThumbHeight: Float,
  isRtl: Boolean,
) {
  if (alpha <= 0f) return

  val trackTop = topInset + TRACK_INSET.toPx()
  val trackHeight = size.height - trackTop - TRACK_INSET.toPx()
  val thumb =
    calculateScrollbarThumb(
      contentSize = state.contentSize,
      viewportSize = state.viewportSize,
      scrollOffset = state.scrollOffset,
      trackHeight = trackHeight,
      minThumbHeight = minThumbHeight,
      maxThumbFraction = MAX_THUMB_FRACTION,
    ) ?: return

  drawRoundRect(
    color = thumbColor.copy(alpha = thumbColor.alpha * alpha),
    topLeft = Offset(x = if (isRtl) 0f else size.width - thickness, y = trackTop + thumb.top),
    size = Size(width = thickness, height = thumb.height),
    cornerRadius = CornerRadius(thickness / 2f),
  )
}

/** Matches Material's overlay scrollbar: a slim outline-coloured thumb with an inset track. */
private const val THUMB_OPACITY = 0.7f
private val THUMB_THICKNESS = 4.dp
private val THUMB_MIN_LENGTH = 24.dp
private const val MAX_THUMB_FRACTION = 0.9f
private val TRACK_INSET = 2.dp
