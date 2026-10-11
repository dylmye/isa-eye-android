package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDetailUiStateTest {
  private val rulesetIds = listOf("2024/2025", "2025/2026", "2026/2027")

  @Test
  fun resolveAccountDetail_readsTheSelectedYearsContributionsAndComparesToThePrevious() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals("Cash ISA", state.name)
    assertEquals(125_000L, state.contributionsPence)
    assertEquals("2026/2027", state.ruleset)
    assertEquals("2025/2026", state.previousRuleset)
    assertEquals(ContributionTrend.Up, state.trend)
    assertTrue(state.isLatestRuleset)
  }

  @Test
  fun resolveAccountDetail_reportsDownWhenContributionsFall() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 80_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals(ContributionTrend.Down, state.trend)
  }

  @Test
  fun resolveAccountDetail_reportsFlatWhenContributionsAreUnchanged() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 100_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertEquals(ContributionTrend.Flat, state.trend)
  }

  @Test
  fun resolveAccountDetail_omitsTheComparisonWithoutAPreviousYearRecord() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(contribution("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
      )

    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_marksAnEarlierRulesetAsNotLatest() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = listOf(contribution("2024/2025", 50_000)),
        rulesetIds = rulesetIds,
        selectedId = "2024/2025",
      )

    assertEquals("2024/2025", state.ruleset)
    assertFalse(state.isLatestRuleset)
    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_defaultsToTheLatestRulesetAndZeroContributions() {
    val state =
      resolveAccountDetail(
        product = product(),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        selectedId = null,
      )

    assertEquals("2026/2027", state.ruleset)
    assertTrue(state.isLatestRuleset)
    assertEquals(0L, state.contributionsPence)
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

  private fun contribution(rulesetId: String, pence: Long) = AnnualBalanceEntity(
    productId = "p1",
    rulesetId = rulesetId,
    lastUpdatedDateUnix = 0L,
    deductedFromAllowancePence = pence,
  )
}
