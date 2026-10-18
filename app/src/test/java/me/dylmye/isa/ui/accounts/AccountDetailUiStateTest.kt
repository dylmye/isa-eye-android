package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AccountDetailUiStateTest {
  private val rulesetIds = listOf("2024/2025", "2025/2026", "2026/2027")
  private val now = at("2026-03-15 12:00")
  private val threeDaysAgo = at("2026-03-12 12:00")

  @Test
  fun resolveAccountDetail_readsTheSelectedYearsContributionsAndComparesToThePrevious() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
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
        productTypes = emptyList(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 80_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertEquals(ContributionTrend.Down, state.trend)
  }

  @Test
  fun resolveAccountDetail_reportsFlatWhenContributionsAreUnchanged() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 100_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertEquals(ContributionTrend.Flat, state.trend)
  }

  @Test
  fun resolveAccountDetail_omitsTheComparisonWithoutAPreviousYearRecord() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2026/2027", 125_000)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_marksAnEarlierRulesetAsNotLatest() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2024/2025", 50_000)),
        rulesetIds = rulesetIds,
        selectedId = "2024/2025",
        nowMillis = now,
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
        productTypes = emptyList(),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        selectedId = null,
        nowMillis = now,
      )

    assertEquals("2026/2027", state.ruleset)
    assertTrue(state.isLatestRuleset)
    assertEquals(0L, state.contributionsPence)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_carriesTheProductTypesNameAndLongDescription() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = listOf(productType()),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        selectedId = null,
        nowMillis = now,
      )

    assertEquals("Cash ISA", state.typeName)
    assertEquals("A deposit account, where your cash is guaranteed.", state.longDescription)
  }

  @Test
  fun resolveAccountDetail_omitsTypeDetailsWhenTheTypeIsUnknown() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = emptyList(),
        rulesetIds = rulesetIds,
        selectedId = null,
        nowMillis = now,
      )

    assertNull(state.typeName)
    assertNull(state.longDescription)
  }

  @Test
  fun resolveAccountDetail_describesARecentFigureRelatively() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2026/2027", 125_000, updatedMillis = threeDaysAgo)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertEquals("3 days ago", state.lastUpdatedLabel)
  }

  @Test
  fun resolveAccountDetail_datesAFigureOlderThanAYear() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances =
          listOf(contribution("2026/2027", 125_000, updatedMillis = at("2025-01-04 12:00"))),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertEquals("4 Jan 2025", state.lastUpdatedLabel)
  }

  @Test
  fun resolveAccountDetail_omitsTheLabelWhenTheYearHasNoRecordedFigure() {
    val state =
      resolveAccountDetail(
        product = product(),
        productTypes = emptyList(),
        balances = listOf(contribution("2024/2025", 50_000, updatedMillis = threeDaysAgo)),
        rulesetIds = rulesetIds,
        selectedId = "2026/2027",
        nowMillis = now,
      )

    assertNull(state.lastUpdatedLabel)
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

  private fun productType() = ProductTypeEntity(
    id = "CASH",
    name = "Cash ISA",
    introducedWithRuleset = "2008/2009",
    removedWithRuleset = null,
    shortDescription = "Earn interest on cash tax-free",
    longDescription = "A deposit account, where your cash is guaranteed.",
  )

  private fun contribution(rulesetId: String, pence: Long, updatedMillis: Long = 0L) =
    AnnualBalanceEntity(
      productId = "p1",
      rulesetId = rulesetId,
      lastUpdatedDateUnix = updatedMillis,
      deductedFromAllowancePence = pence,
    )
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

/** Parses a UK wall-clock instant so the expectations above read as dates rather than millis. */
private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
