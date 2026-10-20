package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AutoContributionUiStateTest {
  private val now = at("2026-10-18 12:00")
  private val rulesets = listOf(ruleset("2026/2027", "2026-04-06", "2027-04-05"))

  @Test
  fun resolveAutoContribution_startsOnTheNextFirst() {
    val state = resolve()

    assertEquals("2026/2027", state.ruleset)
    assertEquals("2026-11-01", state.firstCreditDate)
    assertEquals("2027-04-01", state.lastCreditDate)
    assertEquals(6, state.credits)
    assertEquals("", state.input)
    assertNull(state.savedMonthlyPence)
    assertFalse(state.isLoading)
  }

  @Test
  fun resolveAutoContribution_startsTodayWhenTodayIsTheFirst() {
    val state = resolve(nowMillis = at("2026-11-01 09:00"))

    assertEquals("2026-11-01", state.firstCreditDate)
    assertEquals(6, state.credits)
  }

  @Test
  fun resolveAutoContribution_fillsTheFieldWithTheSavedAmount() {
    val state = resolve(schedules = listOf(schedule(monthlyPence = 50_000)))

    assertEquals("500", state.input)
    assertEquals(50_000L, state.savedMonthlyPence)
    assertEquals("2026-11-01", state.firstCreditDate)
  }

  @Test
  fun resolveAutoContribution_keepsTheSavedFirstCreditDate() {
    val state = resolve(schedules = listOf(schedule(first = "2026-12-01")))

    assertEquals("2026-12-01", state.firstCreditDate)
    assertEquals(5, state.credits)
  }

  @Test
  fun resolveAutoContribution_letsAFreshEntryOverrideTheSavedAmount() {
    val state =
      resolve(
        schedules = listOf(schedule(monthlyPence = 50_000)),
        draft = AutoContributionDraft(input = "750"),
      )

    assertEquals("750", state.input)
    assertEquals(50_000L, state.savedMonthlyPence)
  }

  @Test
  fun resolveAutoContribution_reportsNoYearOnceTheRangeHasPassed() {
    val state = resolve(nowMillis = at("2030-01-01 12:00"))

    assertNull(state.ruleset)
    assertEquals(0, state.credits)
    assertFalse(state.canSave)
  }

  @Test
  fun projectedTotal_multipliesTheAmountByTheCredits() {
    val state = resolve(draft = AutoContributionDraft(input = "500"))

    assertEquals(300_000L, state.projectedTotalPence)
  }

  @Test
  fun error_rejectsAmountsThatAreNotAboveZero() {
    val state = resolve(draft = AutoContributionDraft(input = "0"))

    assertEquals("Enter an amount above £0.", state.error)
  }

  @Test
  fun canSave_needsAnAmountAndCreditsLeft() {
    assertFalse(resolve().canSave)
    assertTrue(resolve(draft = AutoContributionDraft(input = "500")).canSave)
    assertFalse(resolve(draft = AutoContributionDraft(input = "500", isSaving = true)).canSave)
  }

  @Test
  fun canSave_isFalseWhenTheYearHasNoMonthsLeft() {
    val state =
      resolve(
        nowMillis = at("2027-04-02 12:00"),
        draft = AutoContributionDraft(input = "500"),
      )

    assertEquals(0, state.credits)
    assertFalse(state.canSave)
  }

  @Test
  fun canRemove_isOnlyOfferedOnceAScheduleIsSaved() {
    assertFalse(resolve().canRemove)
    assertTrue(resolve(schedules = listOf(schedule())).canRemove)
  }

  private fun resolve(
    schedules: List<AutoContributionEntity> = emptyList(),
    nowMillis: Long = now,
    draft: AutoContributionDraft = AutoContributionDraft(),
  ) = resolveAutoContribution(rulesets, schedules, nowMillis, draft)

  private fun schedule(monthlyPence: Long = 50_000, first: String = "2026-11-01") =
    AutoContributionEntity(
      productId = "p1",
      rulesetId = "2026/2027",
      monthlyPence = monthlyPence,
      firstCreditDate = first,
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

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
