package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountDetailUiStateTest {
  private val rulesetIds = listOf("2024/2025", "2025/2026", "2026/2027")

  @Test
  fun resolveAccountDetail_readsTheSelectedYearsBalanceAndComparesToThePrevious() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(balance("2025/2026", 100_000), balance("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals("Cash ISA", state.name)
    assertEquals(125_000L, state.balancePence)
    assertEquals("2026/2027", state.ruleset)
    assertEquals("2025/2026", state.previousRuleset)
    assertEquals(BalanceTrend.Up, state.trend)
  }

  @Test
  fun resolveAccountDetail_reportsDownWhenTheBalanceFalls() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(balance("2025/2026", 100_000), balance("2026/2027", 80_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals(BalanceTrend.Down, state.trend)
  }

  @Test
  fun resolveAccountDetail_reportsFlatWhenTheBalanceIsUnchanged() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(balance("2025/2026", 100_000), balance("2026/2027", 100_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals(BalanceTrend.Flat, state.trend)
  }

  @Test
  fun resolveAccountDetail_omitsTheComparisonWithoutAPreviousYearRecord() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(balance("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_omitsTheComparisonWhenThereIsNoEarlierRuleset() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(balance("2024/2025", 50_000)),
        rulesetIds = rulesetIds,
        selectedId = "2024/2025",
      )

    assertEquals("2024/2025", state.ruleset)
    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_defaultsToTheLatestRulesetAndZeroBalance() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        selectedId = null,
      )

    assertEquals("2026/2027", state.ruleset)
    assertEquals(0L, state.balancePence)
    assertNull(state.trend)
  }

  private fun product() = ProductEntity(
    id = "p1",
    startTaxYear = "2024/2025",
    endTaxYear = null,
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
