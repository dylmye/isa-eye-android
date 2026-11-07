package me.dylmye.isa.ui.components

/** The thumb's offset and length within the scrollbar's track, in pixels. */
internal data class ScrollbarThumb(val top: Float, val height: Float)

/**
 * Works out the thumb for a scrollbar track of [trackHeight].
 *
 * Returns null when there is nothing to scroll, or when the content has not been measured yet. The
 * thumb is never shorter than [minThumbHeight] (so it stays visible on a very long list) nor longer
 * than [maxThumbFraction] of the track (so it still reads as a thumb rather than a filled track).
 * The offset is clamped, so a transient scroll position outside the range cannot push it offscreen.
 */
internal fun calculateScrollbarThumb(
  contentSize: Int,
  viewportSize: Int,
  scrollOffset: Int,
  trackHeight: Float,
  minThumbHeight: Float,
  maxThumbFraction: Float,
): ScrollbarThumb? {
  if (contentSize <= viewportSize || viewportSize <= 0 || trackHeight <= 0f) return null

  val maxHeight = trackHeight * maxThumbFraction
  val minHeight = minThumbHeight.coerceAtMost(maxHeight)
  val height = (trackHeight * viewportSize / contentSize).coerceIn(minHeight, maxHeight)
  val scrollRange = contentSize - viewportSize
  val position = scrollOffset.coerceIn(0, scrollRange).toFloat() / scrollRange
  return ScrollbarThumb(top = (trackHeight - height) * position, height = height)
}
