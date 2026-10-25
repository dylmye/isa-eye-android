package me.dylmye.isa.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.time.isoDate

/**
 * Read/write access to the monthly contributions the app records for itself, including the catch-up
 * that credits every month which has come round since it last ran, and taking a credit back out
 * when the user says they did not actually save that month.
 */
class AutoContributionRepository(private val database: IsaDatabase) {
  fun observeForProduct(productId: String): Flow<List<AutoContributionEntity>> =
    database.autoContributionDao().observeForProduct(productId)

  /**
   * Saves the monthly amount for [productId] in [rulesetId]. An edit keeps the schedule's first
   * credit date and everything already applied, so only future credits take the new amount.
   */
  suspend fun setSchedule(
    productId: String,
    rulesetId: String,
    monthlyPence: Long,
    firstCreditDate: String,
  ) {
    val existing = database.autoContributionDao().find(productId, rulesetId)
    database.autoContributionDao().upsert(
      AutoContributionEntity(
        productId = productId,
        rulesetId = rulesetId,
        monthlyPence = monthlyPence,
        firstCreditDate = existing?.firstCreditDate ?: firstCreditDate,
        appliedThroughDate = existing?.appliedThroughDate,
        lastCreditPence = existing?.lastCreditPence,
      ),
    )
  }

  /** Stops the schedule. Credits already recorded stay in the year's total. */
  suspend fun clear(productId: String, rulesetId: String) {
    database.autoContributionDao().delete(productId, rulesetId)
  }

  /**
   * Adds every credit that has come due since the last run and advances each schedule past them.
   * One transaction, so a schedule and the total it feeds can never disagree.
   */
  suspend fun applyDue(nowMillis: Long) {
    database.withTransaction { applyDueLocked(nowMillis) }
  }

  /**
   * Takes the last credit [productId]'s schedule added back out of the year's total, subtracting
   * exactly what it contributed so a later amount change cannot skew it. The schedule keeps its
   * place, so that month is not credited again. Returns whether there was a credit to undo.
   */
  suspend fun undoLastCredit(productId: String, rulesetId: String): Boolean =
    database.withTransaction {
      val schedule = database.autoContributionDao().find(productId, rulesetId)
      val credit = schedule?.lastCreditPence
      if (schedule == null || credit == null) {
        false
      } else {
        removeCredit(schedule, credit, System.currentTimeMillis())
        true
      }
    }

  /**
   * Takes the month starting [monthDate] back out for [productIds], for the reminder's Undo action.
   * Only a schedule whose last credit is that month is touched, so a stale notification cannot
   * remove a later month. Returns how many schedules were undone.
   */
  suspend fun undoMonth(
    productIds: List<String>,
    rulesetId: String,
    monthDate: String,
    nowMillis: Long,
  ): Int = database.withTransaction {
    var undone = 0
    productIds.forEach { productId ->
      val schedule = database.autoContributionDao().find(productId, rulesetId)
      val credit = schedule?.lastCreditPence
      if (schedule != null && credit != null && schedule.appliedThroughDate == monthDate) {
        removeCredit(schedule, credit, nowMillis)
        undone++
      }
    }
    undone
  }

  /** The catch-up itself, for callers that already hold a transaction. */
  private suspend fun applyDueLocked(nowMillis: Long) {
    val today = isoDate(nowMillis)
    val schedules = database.autoContributionDao().getAll()
    val endDates = database.rulesetDao().getAll().associate { it.id to it.endDate }
    val balances =
      database.annualBalanceDao().getAll().associateBy { it.productId to it.rulesetId }
    schedules.forEach { schedule ->
      val end = endDates[schedule.rulesetId]
      val due = end?.let { dueCreditDates(schedule, it, today) }.orEmpty()
      if (due.isNotEmpty()) {
        val recorded = balances[schedule.productId to schedule.rulesetId]
        database.annualBalanceDao().upsert(
          AnnualBalanceEntity(
            productId = schedule.productId,
            rulesetId = schedule.rulesetId,
            lastUpdatedDateUnix = nowMillis,
            deductedFromAllowancePence =
              (recorded?.deductedFromAllowancePence ?: 0L) + due.size * schedule.monthlyPence,
          ),
        )
        database.autoContributionDao().upsert(
          schedule.copy(
            appliedThroughDate = due.last(),
            lastCreditPence = schedule.monthlyPence,
          ),
        )
      }
    }
  }

  /** Subtracts [credit] from the schedule's year and forgets it, leaving the schedule in place. */
  private suspend fun removeCredit(
    schedule: AutoContributionEntity,
    credit: Long,
    nowMillis: Long,
  ) {
    val balance = database.annualBalanceDao().find(schedule.productId, schedule.rulesetId)
    database.annualBalanceDao().upsert(
      AnnualBalanceEntity(
        productId = schedule.productId,
        rulesetId = schedule.rulesetId,
        lastUpdatedDateUnix = nowMillis,
        deductedFromAllowancePence =
          ((balance?.deductedFromAllowancePence ?: 0L) - credit).coerceAtLeast(0L),
      ),
    )
    database.autoContributionDao().upsert(schedule.copy(lastCreditPence = null))
  }
}
