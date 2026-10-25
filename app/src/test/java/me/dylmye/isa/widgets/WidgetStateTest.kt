package me.dylmye.isa.widgets

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import me.dylmye.isa.ui.insights.TaxYearStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WidgetStateTest {
  private val now = at("2026-10-18 12:00")

  private val rulesets =
    listOf(
      ruleset("2025/2026", "2025-04-06", "2026-04-05"),
      ruleset("2026/2027", "2026-04-06", "2027-04-05"),
    )

  private val productTypes =
    listOf(
      productType("CASH", "Cash ISA"),
      productType("JUNIOR", "Junior ISA"),
    )

  private val accounts =
    listOf(
      account("a1", "Cash ISA", "CASH"),
      account("j1", "Ada's Junior ISA", "JUNIOR"),
    )

  @Test
  fun resolveWidgetState_readsTodaysYearAndTheSharedAllowance() {
    val state = resolve(balances = listOf(balance("2026/2027", "a1", 165_000)))

    assertEquals("2026/2027", state.rulesetId)
    assertEquals("2026/2027", state.yearLabel)
    assertEquals(2_000_000L, state.sharedAllowancePence)
    assertEquals(1_835_000L, state.sharedRemainingPence)
    assertEquals(165_000L, state.sharedContributedPence)
    assertEquals(0.9175f, state.sharedRemainingFraction, 0.0001f)
    assertFalse(state.sharedIsMaxedOut)
    assertTrue(state.hasSharedAccounts)
    assertEquals("5 April 2027", state.endDateLabel)
    assertEquals("6 April 2027", state.renewalLabel)
  }

  @Test
  fun resolveWidgetState_countsTheDaysAndTheWholeTaxYear() {
    val state = resolve(balances = listOf(balance("2026/2027", "a1", 165_000)))

    assertEquals(169, state.daysRemaining)
    assertEquals(365, state.totalDays)
    assertEquals(196, state.daysElapsed)
    assertEquals(365, state.daysElapsed!! + state.daysRemaining!!)
    assertEquals(0.537f, state.elapsedFraction, 0.001f)
    assertEquals(TaxYearStage.Mid, state.stage)
  }

  @Test
  fun resolveWidgetState_countsAwholeLeapYear() {
    val state =
      resolve(
        rulesets =
          listOf(ruleset("2027/2028", "2027-04-06", "2028-04-05")),
        nowMillis = at("2027-06-01 12:00"),
      )

    assertEquals(366, state.totalDays)
    assertEquals(366, state.daysElapsed!! + state.daysRemaining!!)
  }

  @Test
  fun resolveWidgetState_listsEachJuniorAccountAgainstItsOwnAllowance() {
    val state =
      resolve(
        accounts = listOf(account("j1", "Ada's Junior ISA", "JUNIOR")),
        balances = listOf(balance("2026/2027", "j1", 200_000)),
        exceptions = listOf(juniorException("2026/2027")),
      )

    val junior = state.juniors.single()
    assertEquals("j1", junior.productId)
    assertEquals("Ada's Junior ISA", junior.accountName)
    assertEquals(900_000L, junior.allowancePence)
    assertEquals(200_000L, junior.usedPence)
    assertEquals(700_000L, junior.remainingPence)
    assertEquals(0.7778f, junior.remainingFraction, 0.0001f)
    assertFalse(state.hasSharedAccounts)
  }

  @Test
  fun resolveWidgetState_leavesTheSharedFigureAloneWhenOnlyJuniorIsasExist() {
    val state =
      resolve(
        accounts = listOf(account("j1", "Ada's Junior ISA", "JUNIOR")),
        balances = listOf(balance("2026/2027", "j1", 200_000)),
        exceptions = listOf(juniorException("2026/2027")),
      )

    assertFalse(state.hasSharedAccounts)
    // The shared allowance widget shows its empty state rather than an unusable £20,000.
    assertEquals(2_000_000L, state.sharedRemainingPence)
  }

  @Test
  fun resolveWidgetState_includesJuniorContributionsInTheGoal() {
    val state =
      resolve(
        balances =
          listOf(balance("2026/2027", "a1", 165_000), balance("2026/2027", "j1", 200_000)),
        exceptions = listOf(juniorException("2026/2027")),
        goals = listOf(GoalEntity("2026/2027", 2_000_000)),
      )

    val goal = requireNotNull(state.goal)
    assertEquals(365_000L, goal.contributedPence)
    assertEquals(1_635_000L, goal.remainingPence)
    assertEquals(6, goal.monthsRemaining)
    assertEquals(272_500L, goal.monthlySuggestionPence)
    assertEquals(365_000L, state.contributedPence)
  }

  @Test
  fun resolveWidgetState_marksTheAllowanceMaxedOutAtTheLimit() {
    val state = resolve(balances = listOf(balance("2026/2027", "a1", 2_000_000)))

    assertEquals(0L, state.sharedRemainingPence)
    assertTrue(state.sharedIsMaxedOut)
    assertEquals(0f, state.sharedRemainingFraction, 0f)
  }

  @Test
  fun resolveWidgetState_hasNoYearOutsideTheSeededRange() {
    val state =
      resolve(
        balances = listOf(balance("2026/2027", "a1", 165_000)),
        nowMillis = at("2030-01-01 12:00"),
      )

    assertNull(state.rulesetId)
    assertNull(state.daysRemaining)
    assertNull(state.totalDays)
    assertNull(state.stage)
    assertNull(state.goal)
    assertNull(state.endDateLabel)
    assertTrue(state.hasAccounts)
  }

  @Test
  fun formatWidgetGbp_dropsPenceThatWouldNotBeRead() {
    assertEquals("£12,500", formatWidgetGbp(1_250_000))
    assertEquals("£12,500.50", formatWidgetGbp(1_250_050))
  }

  @Test
  fun days_isSingularForOneDay() {
    assertEquals("1 day", days(1))
    assertEquals("2 days", days(2))
  }

  private fun resolve(
    balances: List<AnnualBalanceEntity> = emptyList(),
    accounts: List<AccountSummary> = this.accounts,
    rulesets: List<RulesetEntity> = this.rulesets,
    exceptions: List<RulesetExceptionEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    nowMillis: Long = now,
  ) = resolveWidgetState(
    accounts = accounts,
    productTypes = productTypes,
    balances = balances,
    rulesets = rulesets,
    exceptions = exceptions,
    goals = goals,
    nowMillis = nowMillis,
  )

  private fun juniorException(rulesetId: String) = RulesetExceptionEntity(
    id = "$rulesetId-JUNIOR",
    productTypeId = "JUNIOR",
    rulesetId = rulesetId,
    allowancePence = 900_000,
    notes = null,
    includedInShared = false,
  )

  private fun account(id: String, name: String, typeId: String) = AccountSummary(
    productId = id,
    friendlyName = name,
    productTypeId = typeId,
    startTaxYear = "2024/2025",
    endTaxYear = null,
    providerId = "provider-$id",
    providerName = "Bank",
    providerColour = "#006A4D",
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

  private fun productType(id: String, name: String) = ProductTypeEntity(
    id = id,
    name = name,
    introducedWithRuleset = "2008/2009",
    removedWithRuleset = null,
    shortDescription = "",
    longDescription = "",
    colour = "#2E7D32",
  )
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
