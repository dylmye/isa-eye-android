package me.dylmye.isa.ui.insights

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class InsightsUiStateTest {
  private val now = at("2026-10-18 12:00")

  private val rulesets =
    listOf(
      ruleset("2025/2026", "2025-04-06", "2026-04-05"),
      ruleset("2026/2027", "2026-04-06", "2027-04-05"),
    )

  private val productTypes =
    listOf(
      productType("CASH", "Cash ISA", "#2E7D32"),
      productType("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0"),
    )

  private val accounts =
    listOf(
      account("a1", "Cash ISA", "CASH", "#006A4D"),
      account("a2", "Stocks & Shares ISA", "STOCKS_AND_SHARES", "#8A1E2D"),
    )

  @Test
  fun resolveInsights_readsTheRulesetThatContainsToday() {
    val state =
      resolve(listOf(balance("2025/2026", "a1", 50_000), balance("2026/2027", "a1", 125_000)))

    assertEquals("2026/2027", state.ruleset)
    assertEquals(125_000L, state.currentYearTotalPence)
  }

  @Test
  fun resolveInsights_countsTheDaysUntilTheRulesetEnds() {
    val state = resolve(listOf(balance("2026/2027", "a1", 125_000)))

    assertEquals(169, state.daysRemaining)
  }

  @Test
  fun resolveInsights_isEarlyThroughTheFirstSixMonths() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 125_000)), nowMillis = at("2026-09-30 12:00"))

    assertEquals(TaxYearStage.Early, state.stage)
  }

  @Test
  fun resolveInsights_turnsMidOnceSixMonthsHavePassed() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 125_000)), nowMillis = at("2026-10-06 12:00"))

    assertEquals(TaxYearStage.Mid, state.stage)
  }

  @Test
  fun resolveInsights_turnsLateOnceNineMonthsHavePassed() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 125_000)), nowMillis = at("2027-01-06 12:00"))

    assertEquals(TaxYearStage.Late, state.stage)
  }

  @Test
  fun resolveInsights_reportsNoCountdownWithoutARulesetForToday() {
    val state = resolve(emptyList(), nowMillis = at("2030-01-01 12:00"))

    assertNull(state.ruleset)
    assertNull(state.daysRemaining)
    assertNull(state.stage)
  }

  @Test
  fun resolveInsights_sharesTheYearAcrossAccountsLargestFirst() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 40_000), balance("2026/2027", "a2", 125_000)))

    assertEquals(listOf("a2", "a1"), state.accountShares.map { it.productId })
    assertEquals("#8A1E2D", state.accountShares.first().colour)
    assertEquals(165_000L, state.currentYearTotalPence)
  }

  @Test
  fun resolveInsights_leavesOutAccountsWithNothingRecordedThisYear() {
    val state = resolve(listOf(balance("2026/2027", "a1", 0), balance("2026/2027", "a2", 125_000)))

    assertEquals(listOf("a2"), state.accountShares.map { it.productId })
  }

  @Test
  fun resolveInsights_groupsTheYearByProductType() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 40_000), balance("2026/2027", "a2", 125_000)))

    assertEquals(listOf("STOCKS_AND_SHARES", "CASH"), state.typeShares.map { it.productTypeId })
    assertEquals(listOf("Stocks & Shares ISA", "Cash ISA"), state.typeShares.map { it.name })
    assertEquals(listOf("#1565C0", "#2E7D32"), state.typeShares.map { it.colour })
  }

  @Test
  fun resolveInsights_listsOnlyYearsWithContributionsOldestFirst() {
    val state =
      resolve(listOf(balance("2026/2027", "a1", 125_000), balance("2025/2026", "a1", 50_000)))

    assertEquals(listOf("2025/2026", "2026/2027"), state.yearShares.map { it.rulesetId })
    assertEquals(listOf("25/26", "26/27"), state.yearShares.map { it.label })
  }

  @Test
  fun resolveInsights_isEmptyWithoutAnyContributions() {
    val state = resolve(emptyList())

    assertEquals(0L, state.currentYearTotalPence)
    assertEquals(emptyList<AccountShare>(), state.accountShares)
    assertEquals(emptyList<TypeShare>(), state.typeShares)
    assertEquals(emptyList<YearShare>(), state.yearShares)
  }

  @Test
  fun shortRulesetLabel_trimsEachYearToItsLastTwoDigits() {
    assertEquals("26/27", shortRulesetLabel("2026/2027"))
    assertEquals("17/18", shortRulesetLabel("2017/2018"))
  }

  @Test
  fun seriesValues_readsEachYearInPoundsSoTheAxisGetsRoundSteps() {
    val years =
      listOf(
        yearShare("2025/2026", 140_000, mapOf("CASH" to 50_000, "STOCKS_AND_SHARES" to 90_000)),
        yearShare("2026/2027", 165_000, mapOf("CASH" to 40_000, "STOCKS_AND_SHARES" to 125_000)),
      )

    assertEquals(listOf<Number>(500.0, 400.0), seriesValues(years, "CASH"))
    assertEquals(listOf<Number>(900.0, 1250.0), seriesValues(years, "STOCKS_AND_SHARES"))
  }

  @Test
  fun seriesValues_fillsZeroForAYearWithoutThatType() {
    val years = listOf(yearShare("2026/2027", 40_000, mapOf("CASH" to 40_000)))

    assertEquals(listOf<Number>(0.0), seriesValues(years, "STOCKS_AND_SHARES"))
  }

  @Test
  fun axisMoney_abbreviatesThousandsWithoutMisstatingThem() {
    assertEquals("£0", axisMoney(0.0))
    assertEquals("£400", axisMoney(400.0))
    assertEquals("£1.2k", axisMoney(1200.0))
    assertEquals("£1.5k", axisMoney(1500.0))
    assertEquals("£2k", axisMoney(2000.0))
  }

  private fun resolve(balances: List<AnnualBalanceEntity>, nowMillis: Long = now) =
    resolveInsights(accounts, productTypes, balances, rulesets, nowMillis)

  private fun yearShare(rulesetId: String, totalPence: Long, byType: Map<String, Long>) = YearShare(
    rulesetId = rulesetId,
    label = shortRulesetLabel(rulesetId),
    totalPence = totalPence,
    byType = byType.map { (typeId, pence) -> TypeShare(typeId, typeId, "#000000", pence) },
  )

  private fun account(id: String, name: String, typeId: String, colour: String) = AccountSummary(
    productId = id,
    friendlyName = name,
    productTypeId = typeId,
    startTaxYear = "2024/2025",
    endTaxYear = null,
    providerId = "provider-$id",
    providerName = "Bank",
    providerColour = colour,
    providerIconUrl = null,
  )

  private fun balance(rulesetId: String, productId: String, pence: Long) = AnnualBalanceEntity(
    productId = productId,
    rulesetId = rulesetId,
    lastUpdatedDateUnix = 0L,
    deductedFromAllowancePence = pence,
  )

  private fun ruleset(id: String, startDate: String, endDate: String) = RulesetEntity(
    id = id,
    sharedAllowancePence = 2_000_000,
    startDate = startDate,
    endDate = endDate,
    notes = null,
  )

  private fun productType(id: String, name: String, colour: String = "#2E7D32") = ProductTypeEntity(
    id = id,
    name = name,
    introducedWithRuleset = "2008/2009",
    removedWithRuleset = null,
    shortDescription = "",
    longDescription = "",
    colour = colour,
  )
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
