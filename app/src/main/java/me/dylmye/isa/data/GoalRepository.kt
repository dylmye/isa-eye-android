package me.dylmye.isa.data

import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.GoalEntity

/**
 * Read/write access to the savings goals the app keeps for itself, one per tax year. Kept apart from
 * [AccountRepository] because goals have no counterpart in the shared TinyBase schema.
 */
class GoalRepository(private val database: IsaDatabase) {
  fun observeGoals(): Flow<List<GoalEntity>> = database.goalDao().observeAll()

  /** Replaces the savings goal for [rulesetId]. */
  suspend fun setGoal(rulesetId: String, targetPence: Long) {
    database.goalDao().upsert(GoalEntity(rulesetId = rulesetId, targetPence = targetPence))
  }

  /** Removes the savings goal for [rulesetId], if there is one. */
  suspend fun clearGoal(rulesetId: String) {
    database.goalDao().delete(rulesetId)
  }
}
