package me.dylmye.isa.notifications

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/**
 * The app's own records, read in one pass for the reminder planner. Everything is already on the
 * device: reminders are worked out locally and never involve a server.
 */
internal data class ReminderSnapshot(
  val accounts: List<AccountSummary> = emptyList(),
  val balances: List<AnnualBalanceEntity> = emptyList(),
  val rulesets: List<RulesetEntity> = emptyList(),
  val exceptions: List<RulesetExceptionEntity> = emptyList(),
  val goals: List<GoalEntity> = emptyList(),
  val schedules: List<AutoContributionEntity> = emptyList(),
)
