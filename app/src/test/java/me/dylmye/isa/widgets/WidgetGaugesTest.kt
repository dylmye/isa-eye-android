package me.dylmye.isa.widgets

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetGaugesTest {
  @Test
  fun widgetCellWidth_splitsTheContentWidthAcrossTwoColumnsAndOneGap() {
    assertEquals(100.dp, widgetCellWidth(212.dp, columns = 2))
  }

  @Test
  fun widgetCellWidth_usesTheWholeContentWidthForOneColumn() {
    assertEquals(218.dp, widgetCellWidth(218.dp, columns = 1))
  }
}
