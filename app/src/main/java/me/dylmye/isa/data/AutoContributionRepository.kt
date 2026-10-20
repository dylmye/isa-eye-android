package me.dylmye.isa.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.time.isoDate

/**
 * Read/write access to the monthly contributions the app records for itself, including the catch-up
 * that credits every month which has come round since it last ran.
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
    val today = isoDate(nowMillis)
    database.withTransaction {
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
          database.autoContributionDao().upsert(schedule.copy(appliedThroughDate = due.last()))
        }
      }
    }
  }
}
