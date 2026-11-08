package me.dylmye.isa.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Draws an overlay scrollbar thumb over a lazy list or grid.
 *
 * A fine pointer is required: only mouse or stylus events count as pointer activity, so a touch-only
 * device never shows the thumb and its content stays unobstructed. Pointer users get the thumb on
 * hover, while wheel or trackpad scrolling keeps it awake. It fades out after [fadeDelayMillis]
 * without either, so it does not sit over the content permanently.
 *
 * [topInset] keeps the track below an app bar that the scrollable draws behind. The thumb's
 * position and length come from the scrollable's [ScrollIndicatorState], which reports the
 * estimated content size and the measured viewport for both lazy lists and lazy grids.
 *
 * Pointer and scroll activity is handled by a [VerticalScrollbarNode] rather than by Composition.
 * A mouse move or scroll tick only sends a conflated signal to the node's animation coroutine, and
 * the thumb's alpha and position are read in the draw phase, so scrolling invalidates the draw
 * layer instead of recomposing the list container.
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
  // Read in Composition, never on scroll: the theme, density and layout direction only change on
  // configuration or theme changes. The node keeps them as plain fields and draws with them, so
  // pointer moves and scroll ticks never touch Composition.
  return this.then(
    VerticalScrollbarElement(
      state = state,
      topInset = with(density) { topInset.toPx() },
      fadeDelayMillis = fadeDelayMillis,
      thumbColor = MaterialTheme.colorScheme.outline.copy(alpha = THUMB_OPACITY),
      thickness = with(density) { THUMB_THICKNESS.toPx() },
      minThumbLength = with(density) { THUMB_MIN_LENGTH.toPx() },
      isRtl = layoutDirection == LayoutDirection.Rtl,
    ),
  )
}

private data class VerticalScrollbarElement(
  val state: ScrollIndicatorState,
  val topInset: Float,
  val fadeDelayMillis: Int,
  val thumbColor: Color,
  val thickness: Float,
  val minThumbLength: Float,
  val isRtl: Boolean,
) : ModifierNodeElement<VerticalScrollbarNode>() {
  override fun create() = VerticalScrollbarNode(
    state = state,
    topInset = topInset,
    fadeDelayMillis = fadeDelayMillis,
    thumbColor = thumbColor,
    thickness = thickness,
    minThumbLength = minThumbLength,
    isRtl = isRtl,
  )

  override fun update(node: VerticalScrollbarNode) {
    node.update(this)
    node.invalidateDraw()
  }
}

private class VerticalScrollbarNode(
  var state: ScrollIndicatorState,
  var topInset: Float,
  var fadeDelayMillis: Int,
  var thumbColor: Color,
  var thickness: Float,
  var minThumbLength: Float,
  var isRtl: Boolean,
) : Modifier.Node(),
  DrawModifierNode,
  PointerInputModifierNode {
  // Conflated: a burst of pointer moves or scroll ticks needs one buffered wake-up, not one
  // animation per event. The single consumer below is the only thing that animates the thumb.
  private val activity = Channel<Unit>(Channel.CONFLATED)
  private val alpha = Animatable(0f)

  // A mouse or stylus has been used, so scrolling from now on may be a wheel or trackpad rather
  // than a finger. Touch-only devices never set this and so never show the thumb.
  private var hasFinePointer = false
  private var scrollObserver: Job? = null

  override fun onAttach() {
    coroutineScope.launch {
      while (true) {
        activity.receive()
        alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = FADE_IN_MILLIS))
        // Stay visible until a whole fade delay passes without another sign of life. Activity that
        // arrives while waiting restarts the delay without starting another animation.
        while (true) {
          val stillActive = withTimeoutOrNull(fadeDelayMillis.toLong()) { activity.receive() }
          if (stillActive == null) break
        }
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = FADE_OUT_MILLIS))
      }
    }
    observeScrollOffset()
  }

  override fun onDetach() {
    scrollObserver?.cancel()
    scrollObserver = null
  }

  private fun observeScrollOffset() {
    scrollObserver?.cancel()
    val observedState = state
    scrollObserver =
      coroutineScope.launch {
        // Scrolling after a fine pointer has been seen keeps the thumb in, including wheel and
        // trackpad scrolling, and any accessibility or animated scroll that changes the offset.
        var previousOffset = observedState.scrollOffset
        snapshotFlow { observedState.scrollOffset }.collect { offset ->
          if (offset != previousOffset) {
            previousOffset = offset
            if (hasFinePointer) activity.trySend(Unit)
          }
        }
      }
  }

  override fun onPointerEvent(
    pointerEvent: PointerEvent,
    pass: PointerEventPass,
    bounds: IntSize,
  ) {
    // The Initial pass sees the event before the scrollable consumes it.
    if (pass != PointerEventPass.Initial) return
    val finePointer =
      pointerEvent.changes.any {
        it.type == PointerType.Mouse || it.type == PointerType.Stylus
      }
    if (!finePointer) return
    hasFinePointer = true
    activity.trySend(Unit)
  }

  override fun onCancelPointerInput() = Unit

  override fun ContentDrawScope.draw() {
    drawContent()
    drawScrollbarThumb(
      state = state,
      alpha = alpha.value,
      topInset = topInset,
      thickness = thickness,
      thumbColor = thumbColor,
      minThumbHeight = minThumbLength,
      isRtl = isRtl,
    )
  }

  fun update(element: VerticalScrollbarElement) {
    topInset = element.topInset
    fadeDelayMillis = element.fadeDelayMillis
    thumbColor = element.thumbColor
    thickness = element.thickness
    minThumbLength = element.minThumbLength
    isRtl = element.isRtl
    if (state !== element.state) {
      state = element.state
      if (isAttached) observeScrollOffset()
    }
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
private const val FADE_IN_MILLIS = 100
private const val FADE_OUT_MILLIS = 250
