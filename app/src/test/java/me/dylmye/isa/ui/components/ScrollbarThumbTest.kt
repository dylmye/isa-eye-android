package me.dylmye.isa.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollbarThumbTest {
  private val trackHeight = 400f
  private val minThumbHeight = 24f
  private val maxThumbFraction = 0.9f

  @Test
  fun calculateScrollbarThumb_hasNoThumbWhenTheContentFits() {
    assertNull(thumb(contentSize = 1_000, viewportSize = 1_000, scrollOffset = 0))
    assertNull(thumb(contentSize = 500, viewportSize = 1_000, scrollOffset = 0))
  }

  @Test
  fun calculateScrollbarThumb_hasNoThumbBeforeTheViewportIsMeasured() {
    assertNull(thumb(contentSize = 4_000, viewportSize = 0, scrollOffset = 0))
    assertNull(thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 0, track = 0f))
  }

  @Test
  fun calculateScrollbarThumb_sizesTheThumbToTheVisibleFraction() {
    val thumb = thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 0)

    assertEquals(100f, thumb?.height)
  }

  @Test
  fun calculateScrollbarThumb_putsTheThumbAtTheTopAtTheStart() {
    val thumb = thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 0)

    assertEquals(0f, thumb?.top)
  }

  @Test
  fun calculateScrollbarThumb_putsTheThumbAtTheBottomAtTheEnd() {
    val thumb = thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 3_000)

    assertEquals(300f, thumb?.top)
  }

  @Test
  fun calculateScrollbarThumb_tracksTheScrollPositionBetweenTheEnds() {
    val thumb = thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 1_500)

    assertEquals(150f, thumb?.top)
  }

  @Test
  fun calculateScrollbarThumb_keepsShortThumbsVisible() {
    val thumb = thumb(contentSize = 100_000, viewportSize = 1_000, scrollOffset = 0)

    assertEquals(minThumbHeight, thumb?.height)
  }

  @Test
  fun calculateScrollbarThumb_doesNotLetTheThumbFillTheTrack() {
    val thumb = thumb(contentSize = 1_050, viewportSize = 1_000, scrollOffset = 0)

    assertEquals(trackHeight * maxThumbFraction, thumb?.height)
  }

  @Test
  fun calculateScrollbarThumb_clampsAnOffsetOutsideTheScrollRange() {
    assertEquals(
      0f,
      thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = -500)?.top,
    )
    assertEquals(
      300f,
      thumb(contentSize = 4_000, viewportSize = 1_000, scrollOffset = 99_999)?.top,
    )
  }

  private fun thumb(
    contentSize: Int,
    viewportSize: Int,
    scrollOffset: Int,
    track: Float = trackHeight,
  ): ScrollbarThumb? = calculateScrollbarThumb(
    contentSize = contentSize,
    viewportSize = viewportSize,
    scrollOffset = scrollOffset,
    trackHeight = track,
    minThumbHeight = minThumbHeight,
    maxThumbFraction = maxThumbFraction,
  )
}
