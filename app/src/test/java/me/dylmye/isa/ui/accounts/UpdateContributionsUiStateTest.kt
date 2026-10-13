package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateContributionsUiStateTest {
  private val rulesetIds = listOf("2024/2025", "2025/2026", "2026/2027")

  @Test
  fun resolveUpdateContributions_opensOnTheCurrentYearWithItsRecordedFigure() {
    val state =
      resolveUpdateContributions(
        product = product(),
        balances = listOf(balance("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        currentRulesetId = "2026/2027",
        draft = UpdateContributionsDraft(),
      )

    assertEquals("Cash ISA", state.accountName)
    assertEquals("2026/2027", state.selectedYearId)
    assertEquals("1250", state.amountInput)
    assertEquals(125_000L, state.recordedPence)
    assertFalse(state.isLoading)
  }

  @Test
  fun resolveUpdateContributions_defaultsToTheLatestYearWhenNothingIsSelected() {
    val state =
      resolveUpdateContributions(
        product = product(),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        currentRulesetId = null,
        draft = UpdateContributionsDraft(),
      )

    assertEquals("2026/2027", state.selectedYearId)
    assertEquals("0", state.amountInput)
  }

  @Test
  fun resolveUpdateContributions_offersOnlyTheYearsTheAccountWasOpen() {
    val state =
      resolveUpdateContributions(
        product = product(endTaxYear = "2025/2026"),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        currentRulesetId = null,
        draft = UpdateContributionsDraft(),
      )

    assertEquals(listOf("2024/2025", "2025/2026"), state.yearOptions.map { it.id })
    assertEquals("2025/2026", state.selectedYearId)
  }

  @Test
  fun resolveUpdateContributions_pickingAYearSwapsInThatYearsFigure() {
    val state =
      resolveUpdateContributions(
        product = product(),
        balances = listOf(balance("2025/2026", 80_000), balance("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        currentRulesetId = "2026/2027",
        draft = UpdateContributionsDraft(yearId = "2025/2026"),
      )

    assertEquals("2025/2026", state.selectedYearId)
    assertEquals("800", state.amountInput)
    assertEquals(80_000L, state.recordedPence)
  }

  @Test
  fun resolveUpdateContributions_keepsAnAmountTheUserHasEdited() {
    val state =
      resolveUpdateContributions(
        product = product(),
        balances = listOf(balance("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        currentRulesetId = "2026/2027",
        draft = UpdateContributionsDraft(amountInput = "999.99"),
      )

    assertEquals("999.99", state.amountInput)
    assertEquals(99_999L, state.amountPence)
  }

  @Test
  fun canSave_needsAValidNonNegativeAmount() {
    assertFalse(state(amountInput = "").canSave)
    assertFalse(state(amountInput = ".").canSave)
    assertTrue(state(amountInput = "0").canSave)
    assertTrue(state(amountInput = "1250.50").canSave)
  }

  @Test
  fun showReductionWarning_flagsANonFlexibleIsaGoingDown() {
    val down = state(amountInput = "100", recordedPence = 125_000)
    val flexible = state(isFlexible = true, amountInput = "100", recordedPence = 125_000)
    val equal = state(amountInput = "1250", recordedPence = 125_000)
    val up = state(amountInput = "2000", recordedPence = 125_000)
    val blank = state(amountInput = "", recordedPence = 125_000)

    assertTrue(down.showReductionWarning)
    assertFalse(flexible.showReductionWarning)
    assertFalse(equal.showReductionWarning)
    assertFalse(up.showReductionWarning)
    assertFalse(blank.showReductionWarning)
  }

  private fun state(
    isFlexible: Boolean = false,
    amountInput: String = "",
    recordedPence: Long = 0L,
  ) = UpdateContributionsUiState(
    selectedYearId = "2026/2027",
    isFlexible = isFlexible,
    amountInput = amountInput,
    recordedPence = recordedPence,
  )

  private fun product(endTaxYear: String? = null) = ProductEntity(
    id = "p1",
    startTaxYear = "2024/2025",
    endTaxYear = endTaxYear,
    providerId = "lloyds",
    friendlyName = "Cash ISA",
    productTypeId = "CASH",
    flexible = false,
  )

  private fun balance(rulesetId: String, pence: Long) = AnnualBalanceEntity(
    productId = "p1",
    rulesetId = rulesetId,
    lastUpdatedDateUnix = 0L,
    deductedFromAllowancePence = pence,
  )
}
