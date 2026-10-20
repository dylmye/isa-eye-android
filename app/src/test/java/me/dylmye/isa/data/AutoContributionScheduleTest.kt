package me.dylmye.isa.data

import me.dylmye.isa.data.db.entity.AutoContributionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoContributionScheduleTest {
  @Test
  fun creditDatesBetween_listsTheFirstOfEachMonthInclusive() {
    assertEquals(
      listOf(
        "2026-11-01",
        "2026-12-01",
        "2027-01-01",
        "2027-02-01",
        "2027-03-01",
        "2027-04-01",
      ),
      creditDatesBetween("2026-11-01", "2027-04-05"),
    )
  }

  @Test
  fun creditDatesBetween_isEmptyWhenTheFirstIsPastTheEnd() {
    assertEquals(emptyList<String>(), creditDatesBetween("2027-05-01", "2027-04-05"))
  }

  @Test
  fun creditDatesBetween_includesACreditThatFallsOnTheEndDateItself() {
    assertEquals(listOf("2027-04-01"), creditDatesBetween("2027-04-01", "2027-04-01"))
  }

  @Test
  fun dueCreditDates_startsAtTheFirstCredit() {
    assertEquals(listOf("2026-11-01"), dueCreditDates(schedule(), "2027-04-05", "2026-11-15"))
  }

  @Test
  fun dueCreditDates_isEmptyBeforeTheFirstCredit() {
    assertEquals(emptyList<String>(), dueCreditDates(schedule(), "2027-04-05", "2026-10-18"))
  }

  @Test
  fun dueCreditDates_resumesAfterTheLastAppliedCredit() {
    assertEquals(
      listOf("2026-12-01", "2027-01-01"),
      dueCreditDates(schedule(applied = "2026-11-01"), "2027-04-05", "2027-01-20"),
    )
  }

  @Test
  fun dueCreditDates_neverCreditsPastTheTaxYearEnd() {
    assertEquals(
      listOf("2027-04-01"),
      dueCreditDates(schedule(applied = "2027-03-01"), "2027-04-05", "2027-06-01"),
    )
  }

  @Test
  fun dueCreditDates_isEmptyWhenEverythingHasBeenCredited() {
    assertEquals(
      emptyList<String>(),
      dueCreditDates(schedule(applied = "2027-04-01"), "2027-04-05", "2027-06-01"),
    )
  }

  @Test
  fun nextCreditOn_isTodayWhenTodayIsTheFirst() {
    assertEquals("2026-11-01", nextCreditOn("2026-11-01"))
  }

  @Test
  fun nextCreditOn_isTheNextFirstOtherwise() {
    assertEquals("2026-11-01", nextCreditOn("2026-10-18"))
  }

  @Test
  fun nextCreditOn_rollsOverTheYear() {
    assertEquals("2027-01-01", nextCreditOn("2026-12-31"))
  }

  @Test
  fun nextCreditOn_isNullForAnUnreadableDate() {
    assertNull(nextCreditOn("not a date"))
  }

  private fun schedule(applied: String? = null) = AutoContributionEntity(
    productId = "p1",
    rulesetId = "2026/2027",
    monthlyPence = 50_000,
    firstCreditDate = "2026-11-01",
    appliedThroughDate = applied,
  )
}
