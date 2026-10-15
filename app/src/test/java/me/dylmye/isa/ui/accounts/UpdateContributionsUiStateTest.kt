package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateContributionsUiStateTest {
  @Test
  fun resolveUpdateContributions_opensOnTheCurrentYearWithItsRecordedFigure() {
    val state =
      resolve(currentRulesetId = "2026/2027", balances = listOf(balance("2026/2027", 125_000)))

    assertEquals("Cash ISA", state.accountName)
    assertEquals("2026/2027", state.selectedYearId)
    assertEquals("1250", state.amountInput)
    assertEquals(125_000L, state.recordedPence)
    assertFalse(state.isLoading)
  }

  @Test
  fun resolveUpdateContributions_defaultsToTheLatestYearWhenNothingIsSelected() {
    val state = resolve()

    assertEquals("2026/2027", state.selectedYearId)
    assertEquals("0", state.amountInput)
  }

  @Test
  fun resolveUpdateContributions_offersOnlyTheYearsTheAccountWasOpen() {
    val state = resolve(product = product(endTaxYear = "2025/2026"))

    assertEquals(listOf("2024/2025", "2025/2026"), state.yearOptions.map { it.id })
    assertEquals("2025/2026", state.selectedYearId)
  }

  @Test
  fun resolveUpdateContributions_pickingAYearSwapsInThatYearsFigure() {
    val state =
      resolve(
        balances = listOf(balance("2025/2026", 80_000), balance("2026/2027", 125_000)),
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
      resolve(
        balances = listOf(balance("2026/2027", 125_000)),
        currentRulesetId = "2026/2027",
        draft = UpdateContributionsDraft(amountInput = "999.99"),
      )

    assertEquals("999.99", state.amountInput)
    assertEquals(99_999L, state.amountPence)
  }

  @Test
  fun resolveUpdateContributions_usesTheSharedAllowanceWhenTheTypeHasNoException() {
    val state = resolve(currentRulesetId = "2026/2027")

    assertEquals(2_000_000L, state.allowancePence)
  }

  @Test
  fun resolveUpdateContributions_prefersTheProductTypesOwnAllowance() {
    val state =
      resolve(
        product = product(productTypeId = "LIFETIME"),
        exceptions = listOf(exception("LIFETIME", "2026/2027", 400_000)),
        currentRulesetId = "2026/2027",
      )

    assertEquals(400_000L, state.allowancePence)
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

  @Test
  fun showAllowanceWarning_flagsAnAmountOverTheYearsAllowance() {
    val over = state(amountInput = "25000", allowancePence = 2_000_000)
    val exact = state(amountInput = "20000", allowancePence = 2_000_000)
    val under = state(amountInput = "5000", allowancePence = 2_000_000)
    val unknown = state(amountInput = "25000", allowancePence = null)

    assertTrue(over.showAllowanceWarning)
    assertFalse(exact.showAllowanceWarning)
    assertFalse(under.showAllowanceWarning)
    assertFalse(unknown.showAllowanceWarning)
  }

  @Test
  fun showAllowanceWarning_respectsATypesOwnLowerAllowance() {
    // £5,000 is fine for a Cash ISA but over the £4,000 Lifetime ISA limit.
    val lifetime = state(amountInput = "5000", allowancePence = 400_000)

    assertTrue(lifetime.showAllowanceWarning)
  }

  private fun resolve(
    product: ProductEntity = product(),
    balances: List<AnnualBalanceEntity> = emptyList(),
    rulesets: List<RulesetEntity> = rulesets(),
    exceptions: List<RulesetExceptionEntity> = emptyList(),
    currentRulesetId: String? = null,
    draft: UpdateContributionsDraft = UpdateContributionsDraft(),
  ) = resolveUpdateContributions(product, balances, rulesets, exceptions, currentRulesetId, draft)

  private fun state(
    isFlexible: Boolean = false,
    amountInput: String = "",
    recordedPence: Long = 0L,
    allowancePence: Long? = null,
  ) = UpdateContributionsUiState(
    selectedYearId = "2026/2027",
    isFlexible = isFlexible,
    amountInput = amountInput,
    recordedPence = recordedPence,
    allowancePence = allowancePence,
  )

  private fun rulesets() = listOf(
    RulesetEntity("2024/2025", 2_000_000, "2024-04-06", "2025-04-05", null),
    RulesetEntity("2025/2026", 2_000_000, "2025-04-06", "2026-04-05", null),
    RulesetEntity("2026/2027", 2_000_000, "2026-04-06", "2027-04-05", null),
  )

  private fun exception(typeId: String, yearId: String, pence: Long) = RulesetExceptionEntity(
    id = "$yearId-$typeId",
    productTypeId = typeId,
    rulesetId = yearId,
    allowancePence = pence,
    notes = null,
  )

  private fun product(productTypeId: String = "CASH", endTaxYear: String? = null) = ProductEntity(
    id = "p1",
    startTaxYear = "2024/2025",
    endTaxYear = endTaxYear,
    providerId = "lloyds",
    friendlyName = "Cash ISA",
    productTypeId = productTypeId,
    flexible = false,
  )

  private fun balance(rulesetId: String, pence: Long) = AnnualBalanceEntity(
    productId = "p1",
    rulesetId = rulesetId,
    lastUpdatedDateUnix = 0L,
    deductedFromAllowancePence = pence,
  )
}
