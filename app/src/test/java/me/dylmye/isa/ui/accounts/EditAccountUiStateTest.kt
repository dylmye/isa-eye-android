package me.dylmye.isa.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditAccountUiStateTest {
  @Test
  fun closingYearOptions_startWithStillOpen_andExcludeYearsBeforeOpening() {
    val options =
      closingYearOptions(
        rulesetIds = listOf("2024/2025", "2025/2026", "2026/2027"),
        startTaxYear = "2025/2026",
      )

    assertEquals(listOf(STILL_OPEN_ID, "2025/2026", "2026/2027"), options.map { it.id })
    assertEquals("Still open", options.first().label)
  }

  @Test
  fun canSave_requiresProductType() {
    assertFalse(EditAccountUiState().canSave)
    assertTrue(EditAccountUiState(selectedProductTypeId = "CASH").canSave)
  }
}
