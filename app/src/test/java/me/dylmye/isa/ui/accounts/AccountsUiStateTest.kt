package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsUiStateTest {
  @Test
  fun toListItemUiState_mapsRowAndCarriesTheContribution() {
    val item = summary("p1").toListItemUiState(125_000)

    assertEquals("p1", item.productId)
    assertEquals("Cash ISA", item.name)
    assertEquals("Lloyds Bank", item.providerName)
    assertEquals("#006A4D", item.providerColour)
    assertEquals("https://example.com/lloyds.svg", item.providerIconUrl)
    assertEquals(125_000L, item.contributionsPence)
  }

  @Test
  fun resolveAccountsUiState_showsTheContributionRecordedForTheSelectedYear() {
    val state =
      resolveAccountsUiState(
        accounts = listOf(summary("p1"), summary("p2")),
        balances = listOf(balance("p1", "2026/2027", 125_000), balance("p2", "2025/2026", 80_000)),
        rulesets = rulesets(),
        selectedId = "2026/2027",
      )

    assertEquals(listOf("p1", "p2"), state.accounts.map { it.productId })
    assertEquals(125_000L, state.accounts.first { it.productId == "p1" }.contributionsPence)
    // p2 has no figure for the selected year, so it falls back to zero.
    assertEquals(0L, state.accounts.first { it.productId == "p2" }.contributionsPence)
  }

  @Test
  fun resolveAccountsUiState_reportsTheSharedAllowanceMinusRecordedContributions() {
    val state =
      resolveAccountsUiState(
        accounts = listOf(summary("p1"), summary("p2")),
        balances =
          listOf(balance("p1", "2026/2027", 1_500_000), balance("p2", "2026/2027", 250_000)),
        rulesets = rulesets(),
        selectedId = "2026/2027",
      )

    // £20,000 shared allowance less the £17,500 contributed this year.
    assertEquals(250_000L, state.remainingPence)
  }

  @Test
  fun resolveAccountsUiState_dropsAccountsClosedBeforeTheSelectedYear() {
    val state =
      resolveAccountsUiState(
        accounts = listOf(summary("p1", endTaxYear = "2024/2025"), summary("p2")),
        balances = emptyList(),
        rulesets = rulesets(),
        selectedId = "2026/2027",
      )

    assertEquals(listOf("p2"), state.accounts.map { it.productId })
    assertTrue(state.hasAccounts)
  }

  @Test
  fun allowanceHeadline_showsTheRemainingUntilItIsUsedUp() {
    assertEquals("£12,500.00 remaining", allowanceHeadline(1_250_000))
    assertEquals("Allowance maxed out!", allowanceHeadline(0))
    assertEquals("Allowance maxed out!", allowanceHeadline(-500))
    assertNull(allowanceHeadline(null))
  }

  @Test
  fun remainingAllowancePence_returnsNullForAnUnknownYear() {
    assertNull(remainingAllowancePence("1999/2000", emptyList(), rulesets()))
    assertNull(remainingAllowancePence(null, emptyList(), rulesets()))
  }

  @Test
  fun resolveRuleset_defaultsToTheLatest() {
    val ids = listOf("2024/2025", "2025/2026", "2026/2027")

    val latest = resolveRuleset(ids, selectedId = null)

    assertEquals("2026/2027", latest.current)
    assertTrue(latest.canGoBack)
    assertFalse(latest.canGoForward)
    assertFalse(latest.canReset)
  }

  @Test
  fun resolveRuleset_reportsStepBoundsForTheSelection() {
    val ids = listOf("2024/2025", "2025/2026", "2026/2027")

    val middle = resolveRuleset(ids, selectedId = "2025/2026")
    assertEquals("2025/2026", middle.current)
    assertTrue(middle.canGoBack)
    assertTrue(middle.canGoForward)
    assertTrue(middle.canReset)

    val oldest = resolveRuleset(ids, selectedId = "2024/2025")
    assertFalse(oldest.canGoBack)
    assertTrue(oldest.canGoForward)
  }

  @Test
  fun resolveRuleset_fallsBackToTheLatest_whenTheSelectionIsUnknown() {
    val ids = listOf("2024/2025", "2025/2026")

    val unknown = resolveRuleset(ids, selectedId = "1999/2000")

    assertEquals("2025/2026", unknown.current)
  }

  @Test
  fun resolveRuleset_handlesNoRulesets() {
    val empty = resolveRuleset(emptyList(), selectedId = null)

    assertNull(empty.current)
    assertFalse(empty.canGoBack)
    assertFalse(empty.canGoForward)
    assertFalse(empty.canReset)
  }

  @Test
  fun isOpenDuringRuleset_includesAccountsOpenInTheTargetYear() {
    // Still open, opened before the target.
    assertTrue(isOpenDuringRuleset("2025/2026", "2020/2021", null))
    // Opened in the target year.
    assertTrue(isOpenDuringRuleset("2025/2026", "2025/2026", null))
    // Closed in the target year.
    assertTrue(isOpenDuringRuleset("2025/2026", "2020/2021", "2025/2026"))
    // Closed after the target year.
    assertTrue(isOpenDuringRuleset("2025/2026", "2020/2021", "2026/2027"))
  }

  @Test
  fun isOpenDuringRuleset_excludesAccountsOpenedAfterOrClosedBefore() {
    // Opened after the target.
    assertFalse(isOpenDuringRuleset("2024/2025", "2025/2026", null))
    // Closed before the target.
    assertFalse(isOpenDuringRuleset("2025/2026", "2020/2021", "2024/2025"))
  }

  @Test
  fun isOpenDuringRuleset_withBlankTarget_includesEverything() {
    assertTrue(isOpenDuringRuleset("", "2025/2026", "2025/2026"))
  }

  private fun rulesets() = listOf(
    RulesetEntity("2025/2026", 2_000_000, "2025-04-06", "2026-04-05", null),
    RulesetEntity("2026/2027", 2_000_000, "2026-04-06", "2027-04-05", null),
  )

  private fun summary(id: String, endTaxYear: String? = null) = AccountSummary(
    productId = id,
    friendlyName = "Cash ISA",
    productTypeId = "CASH",
    startTaxYear = "2020/2021",
    endTaxYear = endTaxYear,
    providerId = "lloyds",
    providerName = "Lloyds Bank",
    providerColour = "#006A4D",
    providerIconUrl = "https://example.com/lloyds.svg",
  )

  private fun balance(productId: String, rulesetId: String, pence: Long) = AnnualBalanceEntity(
    productId = productId,
    rulesetId = rulesetId,
    lastUpdatedDateUnix = 0L,
    deductedFromAllowancePence = pence,
  )
}
