package me.dylmye.isa.notifications

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReminderPlannerTest {
  private val rulesets =
    listOf(
      ruleset("2025/2026", "2025-04-06", "2026-04-05"),
      ruleset("2026/2027", "2026-04-06", "2027-04-05"),
    )

  private val accounts = listOf(account("a1", "Cash ISA"), account("a2", "Stocks & Shares ISA"))

  @Test
  fun planReminders_namesTheContributionDueOnTheFirst() {
    val reminders =
      plan(now = "2026-11-01 09:00", schedules = listOf(schedule("a1", 20_000)))

    val reminder = reminders.single()
    assertEquals("Remember to save", reminder.title)
    assertEquals("Tracking £200.00 in your Cash ISA account", reminder.text)
    assertEquals(ReminderTarget.AccountDetails("a1"), reminder.target)
    assertTrue(reminder.lines.isEmpty())
    assertEquals("auto/2026-11", reminder.key)
  }

  @Test
  fun planReminders_gathersEveryContributionIntoOneNotification() {
    val reminders =
      plan(
        now = "2026-11-01 09:00",
        schedules = listOf(schedule("a1", 20_000), schedule("a2", 30_000)),
      )

    val reminder = reminders.single()
    assertEquals("Remember to save", reminder.title)
    assertEquals("Tracking £500.00 across 2 accounts", reminder.text)
    assertEquals(
      listOf("Cash ISA: +£200.00", "Stocks & Shares ISA: +£300.00"),
      reminder.lines,
    )
    assertEquals(ReminderTarget.Accounts, reminder.target)
  }

  @Test
  fun planReminders_suggestsTheGoalMonthlyAmountWithoutAutoContributions() {
    val reminders =
      plan(
        now = "2026-11-01 09:00",
        balances = listOf(balance("2026/2027", "a1", 100_000)),
        goals = listOf(goal("2026/2027", 1_000_000)),
      )

    val reminder = reminders.single()
    assertEquals("Hit your savings goal", reminder.title)
    assertEquals(
      "Put £1,500.00 aside this month to hit your goal. You've got this!",
      reminder.text,
    )
    assertEquals(ReminderTarget.Insights, reminder.target)
    assertEquals("goal/2026-11", reminder.key)
  }

  @Test
  fun planReminders_leavesOutAGoalThatIsAlreadyMet() {
    val reminders =
      plan(
        now = "2026-11-01 09:00",
        balances = listOf(balance("2026/2027", "a1", 2_000_000)),
        goals = listOf(goal("2026/2027", 1_000_000)),
      )

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_prefersAutoContributionsOverTheGoal() {
    val reminders =
      plan(
        now = "2026-11-01 09:00",
        balances = listOf(balance("2026/2027", "a1", 100_000)),
        goals = listOf(goal("2026/2027", 1_000_000)),
        schedules = listOf(schedule("a1", 20_000)),
      )

    assertEquals(listOf("auto/2026-11"), reminders.map { it.key })
  }

  @Test
  fun planReminders_doesNotRemindBeforeTheFirst() {
    val reminders =
      plan(now = "2026-10-31 09:00", schedules = listOf(schedule("a1", 20_000)))

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_stillRemindsEarlyInTheMonth() {
    val reminders =
      plan(now = "2026-11-03 09:00", schedules = listOf(schedule("a1", 20_000)))

    assertEquals(listOf("auto/2026-11"), reminders.map { it.key })
  }

  @Test
  fun planReminders_stopsRemindingAfterTheGraceDays() {
    val reminders =
      plan(now = "2026-11-04 09:00", schedules = listOf(schedule("a1", 20_000)))

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_warnsWhenSixtyDaysAreLeft() {
    val reminders =
      plan(
        now = "2027-02-04 09:00",
        balances = listOf(balance("2026/2027", "a1", 500_000)),
      )

    val reminder = reminders.single()
    assertEquals("60 days left to save!", reminder.title)
    assertEquals("Make the most of your 2026/2027 ISA allowance.", reminder.text)
    assertEquals(ReminderTarget.Insights, reminder.target)
    assertEquals("allowance/2026/2027", reminder.key)
  }

  @Test
  fun planReminders_namesTheGoalInTheAllowanceWarningWhenItIsUnmet() {
    val reminders =
      plan(
        now = "2027-02-04 09:00",
        balances = listOf(balance("2026/2027", "a1", 500_000)),
        goals = listOf(goal("2026/2027", 1_200_000)),
      )

    assertEquals(
      "£7,000.00 left to meet your goal. Save while you can!",
      reminders.single().text,
    )
  }

  @Test
  fun planReminders_fallsBackToTheAllowanceCopyWhenTheGoalIsMet() {
    val reminders =
      plan(
        now = "2027-02-04 09:00",
        balances = listOf(balance("2026/2027", "a1", 500_000)),
        goals = listOf(goal("2026/2027", 100_000)),
      )

    assertEquals(
      "Make the most of your 2026/2027 ISA allowance.",
      reminders.single().text,
    )
  }

  @Test
  fun planReminders_keepsJuniorContributionsOutOfTheSharedAllowance() {
    val reminders =
      plan(
        now = "2027-02-04 09:00",
        balances =
          listOf(
            balance("2026/2027", "a1", 1_900_000),
            balance("2026/2027", "j1", 900_000),
          ),
        accounts = accounts + account("j1", "Junior ISA", typeId = "JUNIOR"),
        exceptions = juniorException(),
      )

    // £1,000 is still left in the shared pot even though the Junior ISA is full.
    assertEquals("allowance/2026/2027", reminders.single().key)
  }

  @Test
  fun planReminders_skipsTheAllowanceWarningOnceNothingIsLeft() {
    val reminders =
      plan(
        now = "2027-02-04 09:00",
        balances = listOf(balance("2026/2027", "a1", 2_000_000)),
      )

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_sendsTheAllowanceWarningOnlyOnce() {
    val reminders =
      plan(
        now = "2027-02-05 09:00",
        balances = listOf(balance("2026/2027", "a1", 500_000)),
        sentKeys = setOf("allowance/2026/2027"),
      )

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_summarisesTheYearThatJustEnded() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances =
          listOf(
            balance("2025/2026", "a1", 100_000),
            balance("2025/2026", "a2", 200_000),
          ),
      )

    val reminder = reminders.single()
    assertEquals("2025/2026 is done", reminder.title)
    assertEquals("You put away £3,000.00 across 2 accounts. Nice one!", reminder.text)
    assertEquals(ReminderTarget.Insights, reminder.target)
    assertEquals("summary/2025/2026", reminder.key)
  }

  @Test
  fun planReminders_mentionsAGoalMetLastYear() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances = listOf(balance("2025/2026", "a1", 300_000)),
        goals = listOf(goal("2025/2026", 200_000)),
      )

    assertEquals(
      "You put away £3,000.00 and met your £2,000.00 goal. Way to go!",
      reminders.single().text,
    )
  }

  @Test
  fun planReminders_stillCheersAGoalMetWithASmallYear() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances = listOf(balance("2025/2026", "a1", 2_000)),
        goals = listOf(goal("2025/2026", 1_000)),
      )

    assertEquals(
      "You put away £20.00 and met your £10.00 goal. Way to go!",
      reminders.single().text,
    )
  }

  @Test
  fun planReminders_leavesTheCheerOffASmallYear() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances = listOf(balance("2025/2026", "a1", 4_999)),
      )

    assertEquals("You put away £49.99 across 1 account.", reminders.single().text)
  }

  @Test
  fun planReminders_cheersFromFiftyPounds() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances = listOf(balance("2025/2026", "a1", 5_000)),
      )

    assertEquals("You put away £50.00 across 1 account. Nice one!", reminders.single().text)
  }

  @Test
  fun planReminders_stillCheersAMissedGoalThatSavedSomething() {
    val reminders =
      plan(
        now = "2026-04-06 09:00",
        balances = listOf(balance("2025/2026", "a1", 100_000)),
        goals = listOf(goal("2025/2026", 200_000)),
      )

    assertEquals(
      "You put away £1,000.00 of your £2,000.00 goal. Nice one!",
      reminders.single().text,
    )
  }

  @Test
  fun planReminders_stopsSummarisingAfterAFortnight() {
    val reminders =
      plan(
        now = "2026-04-20 09:00",
        balances = listOf(balance("2025/2026", "a1", 100_000)),
      )

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_skipsTheSummaryWithNothingRecorded() {
    val reminders = plan(now = "2026-04-06 09:00")

    assertTrue(reminders.isEmpty())
  }

  @Test
  fun planReminders_skipsWhatWasAlreadySent() {
    val reminders =
      plan(
        now = "2026-11-01 09:00",
        schedules = listOf(schedule("a1", 20_000)),
        sentKeys = setOf("auto/2026-11"),
      )

    assertTrue(reminders.isEmpty())
  }

  private fun plan(
    now: String,
    balances: List<AnnualBalanceEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    schedules: List<AutoContributionEntity> = emptyList(),
    sentKeys: Set<String> = emptySet(),
    accounts: List<AccountSummary> = this.accounts,
    exceptions: List<RulesetExceptionEntity> = emptyList(),
  ): List<Reminder> = planReminders(
    snapshot =
      ReminderSnapshot(
        accounts = accounts,
        balances = balances,
        rulesets = rulesets,
        exceptions = exceptions,
        goals = goals,
        schedules = schedules,
      ),
    nowMillis = at(now),
    sentKeys = sentKeys,
  )
}

private fun account(id: String, name: String, typeId: String = "CASH") = AccountSummary(
  productId = id,
  friendlyName = name,
  productTypeId = typeId,
  startTaxYear = "2026/2027",
  endTaxYear = null,
  providerId = "provider",
  providerName = "Bank",
  providerColour = "#006A4D",
  providerIconUrl = null,
)

private fun juniorException() = listOf(
  RulesetExceptionEntity(
    id = "2026/2027-JUNIOR",
    productTypeId = "JUNIOR",
    rulesetId = "2026/2027",
    allowancePence = 900_000,
    notes = null,
    includedInShared = false,
  ),
)

private fun balance(rulesetId: String, productId: String, pence: Long) = AnnualBalanceEntity(
  productId = productId,
  rulesetId = rulesetId,
  lastUpdatedDateUnix = 0L,
  deductedFromAllowancePence = pence,
)

private fun goal(rulesetId: String, targetPence: Long) =
  GoalEntity(rulesetId = rulesetId, targetPence = targetPence)

private fun ruleset(id: String, startDate: String, endDate: String) = RulesetEntity(
  id = id,
  sharedAllowancePence = 2_000_000,
  startDate = startDate,
  endDate = endDate,
  notes = null,
)

private fun schedule(productId: String, monthlyPence: Long) = AutoContributionEntity(
  productId = productId,
  rulesetId = "2026/2027",
  monthlyPence = monthlyPence,
  firstCreditDate = "2026-04-06",
  appliedThroughDate = null,
)

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
