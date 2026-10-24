package me.dylmye.isa.ui.insights

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsChartsTest {
  private val size = Size(200f, 200f)
  private val centre = 100f

  // The middle of the ring: the canvas is 200 wide, so the ring runs from radius 44 to 100.
  private val ring = 72f

  @Test
  fun sliceIndexAt_findsTheSectorUnderTheTouch() {
    val slices =
      listOf(DonutSlice(Color.Red, "Cash ISA", 50), DonutSlice(Color.Blue, "Stocks", 50))

    // The ring starts at twelve o'clock and runs clockwise, so three o'clock is in the first half
    // and nine o'clock in the second.
    assertEquals(0, sliceIndexAt(slices, 100, size, Offset(centre + ring, centre)))
    assertEquals(1, sliceIndexAt(slices, 100, size, Offset(centre - ring, centre)))
    assertEquals(0, sliceIndexAt(slices, 100, size, Offset(centre, centre - ring)))
  }

  @Test
  fun sliceIndexAt_honoursUnevenShares() {
    val slices = listOf(DonutSlice(Color.Red, "Big", 75), DonutSlice(Color.Blue, "Small", 25))

    // The 75% slice covers twelve o'clock round to nine; the remaining quarter is above it.
    assertEquals(0, sliceIndexAt(slices, 100, size, Offset(centre, centre + ring)))
    assertEquals(1, sliceIndexAt(slices, 100, size, Offset(centre - ring, centre)))
  }

  @Test
  fun sliceIndexAt_missesTheHole() {
    val slices = listOf(DonutSlice(Color.Red, "Cash ISA", 100))

    assertNull(sliceIndexAt(slices, 100, size, Offset(centre, centre)))
  }

  @Test
  fun sliceIndexAt_missesTheSpaceAroundTheRing() {
    val slices = listOf(DonutSlice(Color.Red, "Cash ISA", 100))

    assertNull(sliceIndexAt(slices, 100, size, Offset(0f, 0f)))
  }

  @Test
  fun sliceIndexAt_hasNothingToFindWithoutMoney() {
    val slices = listOf(DonutSlice(Color.Red, "Cash ISA", 0))

    assertNull(sliceIndexAt(slices, 0, size, Offset(centre + ring, centre)))
  }

  @Test
  fun accountDonutDescription_namesEachShareAndItsAmount() {
    val state =
      InsightsUiState(
        currentYearTotalPence = 100_000,
        accountShares = listOf(AccountShare("a1", "Cash ISA", "#2E7D32", 100_000)),
      )

    assertEquals(
      "£1,000.00 this year across 1 account: Cash ISA £1,000.00 (100%)",
      accountDonutDescription(state),
    )
  }
}
