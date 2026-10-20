package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AccountDetailUiStateTest {
  private val rulesets =
    listOf(
      ruleset("2024/2025", "2024-04-06", "2025-04-05"),
      ruleset("2025/2026", "2025-04-06", "2026-04-05"),
      ruleset("2026/2027", "2026-04-06", "2027-04-05"),
    )
  private val now = at("2026-03-15 12:00")
  private val threeDaysAgo = at("2026-03-12 12:00")

  @Test
  fun resolveAccountDetail_readsTheSelectedYearsContributionsAndComparesToThePrevious() {
    val state =
      resolve(
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 125_000)),
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
      resolve(
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 80_000)),
        selectedId = "2026/2027",
      )

    assertEquals(ContributionTrend.Down, state.trend)
  }

  @Test
  fun resolveAccountDetail_reportsFlatWhenContributionsAreUnchanged() {
    val state =
      resolve(
        balances = listOf(contribution("2025/2026", 100_000), contribution("2026/2027", 100_000)),
        selectedId = "2026/2027",
      )

    assertEquals(ContributionTrend.Flat, state.trend)
  }

  @Test
  fun resolveAccountDetail_omitsTheComparisonWithoutAPreviousYearRecord() {
    val state =
      resolve(balances = listOf(contribution("2026/2027", 125_000)), selectedId = "2026/2027")

    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_marksAnEarlierRulesetAsNotLatest() {
    val state =
      resolve(balances = listOf(contribution("2024/2025", 50_000)), selectedId = "2024/2025")

    assertEquals("2024/2025", state.ruleset)
    assertFalse(state.isLatestRuleset)
    assertNull(state.previousRuleset)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_defaultsToTheLatestRulesetAndZeroContributions() {
    val state = resolve()

    assertEquals("2026/2027", state.ruleset)
    assertTrue(state.isLatestRuleset)
    assertEquals(0L, state.contributionsPence)
    assertNull(state.trend)
  }

  @Test
  fun resolveAccountDetail_carriesTheProductTypesNameAndLongDescription() {
    val state = resolve(productTypes = listOf(productType()))

    assertEquals("Cash ISA", state.typeName)
    assertEquals("A deposit account, where your cash is guaranteed.", state.longDescription)
  }

  @Test
  fun resolveAccountDetail_omitsTypeDetailsWhenTheTypeIsUnknown() {
    val state = resolve()

    assertNull(state.typeName)
    assertNull(state.longDescription)
  }

  @Test
  fun resolveAccountDetail_describesARecentFigureRelatively() {
    val state =
      resolve(
        balances =
          listOf(contribution("2026/2027", 125_000, updatedMillis = threeDaysAgo)),
        selectedId = "2026/2027",
      )

    assertEquals("3 days ago", state.lastUpdatedLabel)
  }

  @Test
  fun resolveAccountDetail_datesAFigureOlderThanAYear() {
    val state =
      resolve(
        balances =
          listOf(contribution("2026/2027", 125_000, updatedMillis = at("2025-01-04 12:00"))),
        selectedId = "2026/2027",
      )

    assertEquals("4 Jan 2025", state.lastUpdatedLabel)
  }

  @Test
  fun resolveAccountDetail_omitsTheLabelWhenTheYearHasNoRecordedFigure() {
    val state =
      resolve(
        balances = listOf(contribution("2024/2025", 50_000, updatedMillis = threeDaysAgo)),
        selectedId = "2026/2027",
      )

    assertNull(state.lastUpdatedLabel)
  }

  @Test
  fun resolveAccountDetail_marksTheYearTodayFallsIn() {
    val state = resolve(selectedId = "2026/2027", nowMillis = at("2026-10-18 12:00"))

    assertTrue(state.isCurrentTaxYear)
  }

  @Test
  fun resolveAccountDetail_doesNotMarkAnEarlierYearAsCurrent() {
    val state = resolve(selectedId = "2025/2026", nowMillis = at("2026-10-18 12:00"))

    assertFalse(state.isCurrentTaxYear)
    assertFalse(state.hasAutoContribution)
  }

  @Test
  fun resolveAccountDetail_reportsAScheduleForTheRunningYear() {
    val state =
      resolve(
        selectedId = "2026/2027",
        nowMillis = at("2026-10-18 12:00"),
        autoContributions = listOf(schedule("2026/2027")),
      )

    assertTrue(state.hasAutoContribution)
  }

  @Test
  fun resolveAccountDetail_reportsAScheduleForAnEarlierYearToo() {
    val state =
      resolve(
        selectedId = "2025/2026",
        nowMillis = at("2026-10-18 12:00"),
        autoContributions = listOf(schedule("2025/2026")),
      )

    assertFalse(state.isCurrentTaxYear)
    assertTrue(state.hasAutoContribution)
  }

  @Test
  fun resolveAccountDetail_omitsAScheduleBelongingToAnotherYear() {
    val state =
      resolve(
        selectedId = "2026/2027",
        nowMillis = at("2026-10-18 12:00"),
        autoContributions = listOf(schedule("2025/2026")),
      )

    assertFalse(state.hasAutoContribution)
  }

  private fun resolve(
    balances: List<AnnualBalanceEntity> = emptyList(),
    productTypes: List<ProductTypeEntity> = emptyList(),
    selectedId: String? = null,
    autoContributions: List<AutoContributionEntity> = emptyList(),
    nowMillis: Long = now,
  ) = resolveAccountDetail(
    product = product(),
    productTypes = productTypes,
    balances = balances,
    rulesets = rulesets,
    selectedId = selectedId,
    autoContributions = autoContributions,
    nowMillis = nowMillis,
  )

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

  private fun schedule(rulesetId: String) = AutoContributionEntity(
    productId = "p1",
    rulesetId = rulesetId,
    monthlyPence = 50_000,
    firstCreditDate = "2026-11-01",
    appliedThroughDate = null,
  )

  private fun ruleset(id: String, startDate: String, endDate: String) = RulesetEntity(
    id = id,
    sharedAllowancePence = 2_000_000,
    startDate = startDate,
    endDate = endDate,
    notes = null,
  )
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

/** Parses a UK wall-clock instant so the expectations above read as dates rather than millis. */
private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
