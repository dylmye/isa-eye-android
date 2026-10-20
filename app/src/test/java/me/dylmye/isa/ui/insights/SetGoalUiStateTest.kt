package me.dylmye.isa.ui.insights

import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SetGoalUiStateTest {
  private val now = at("2026-10-18 12:00")

  private val rulesets =
    listOf(
      ruleset("2025/2026", "2025-04-06", "2026-04-05"),
      ruleset("2026/2027", "2026-04-06", "2027-04-05"),
    )

  @Test
  fun resolveSetGoal_fillsTheFieldWithTheSavedGoal() {
    val state = resolve(goals = listOf(goal("2026/2027", 1_200_000)))

    assertEquals("2026/2027", state.ruleset)
    assertEquals(2_000_000L, state.allowancePence)
    assertEquals("12000", state.input)
    assertEquals(1_200_000L, state.savedPence)
    assertFalse(state.isLoading)
  }

  @Test
  fun resolveSetGoal_leavesTheFieldEmptyWithoutAGoalForThisYear() {
    val state = resolve(goals = listOf(goal("2025/2026", 1_200_000)))

    assertEquals("", state.input)
    assertNull(state.savedPence)
  }

  @Test
  fun resolveSetGoal_letsAFreshEntryOverrideTheSavedGoal() {
    val state =
      resolve(
        goals = listOf(goal("2026/2027", 1_200_000)),
        draft = SetGoalDraft(input = "500"),
      )

    assertEquals("500", state.input)
    assertEquals(1_200_000L, state.savedPence)
  }

  @Test
  fun resolveSetGoal_reportsNoYearOnceTheRulesetRangeHasPassed() {
    val state = resolve(nowMillis = at("2030-01-01 12:00"))

    assertNull(state.ruleset)
    assertNull(state.allowancePence)
  }

  @Test
  fun enteredPence_isNullWhileTheFieldIsBlank() {
    assertNull(resolve().enteredPence)
  }

  @Test
  fun error_rejectsAmountsThatAreNotAboveZero() {
    assertEquals("Enter an amount above £0.", resolve(draft = SetGoalDraft(input = "0")).error)
    assertEquals("Enter an amount above £0.", resolve(draft = SetGoalDraft(input = "-5")).error)
  }

  @Test
  fun error_rejectsAGoalAboveTheYearsAllowance() {
    val state = resolve(draft = SetGoalDraft(input = "20000.01"))

    assertEquals("That's more than this year's £20,000.00 allowance.", state.error)
  }

  @Test
  fun error_isNullAtTheAllowanceItself() {
    val state = resolve(draft = SetGoalDraft(input = "20000"))

    assertNull(state.error)
    assertTrue(state.canSave)
  }

  @Test
  fun canSave_needsAValidEntryAndARunningYear() {
    assertFalse(resolve().canSave)
    assertTrue(resolve(draft = SetGoalDraft(input = "12000")).canSave)
    assertFalse(resolve(draft = SetGoalDraft(input = "12000", isSaving = true)).canSave)
    assertFalse(
      resolve(nowMillis = at("2030-01-01 12:00"), draft = SetGoalDraft(input = "12000")).canSave,
    )
  }

  @Test
  fun canRemove_isOnlyOfferedOnceAGoalIsSaved() {
    assertFalse(resolve().canRemove)
    assertTrue(resolve(goals = listOf(goal("2026/2027", 1_200_000))).canRemove)
  }

  private fun resolve(
    goals: List<GoalEntity> = emptyList(),
    nowMillis: Long = now,
    draft: SetGoalDraft = SetGoalDraft(),
  ) = resolveSetGoal(rulesets, goals, nowMillis, draft)

  private fun goal(rulesetId: String, targetPence: Long) =
    GoalEntity(rulesetId = rulesetId, targetPence = targetPence)

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
