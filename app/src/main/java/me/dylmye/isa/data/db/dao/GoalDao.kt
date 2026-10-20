package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.GoalEntity

@Dao
interface GoalDao {
  @Query("SELECT * FROM goals")
  fun observeAll(): Flow<List<GoalEntity>>

  @Upsert suspend fun upsert(goal: GoalEntity)

  @Query("DELETE FROM goals WHERE rulesetId = :rulesetId")
  suspend fun delete(rulesetId: String)
}
