package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

@Dao
interface RulesetExceptionDao {
  @Query("SELECT * FROM rulesetExceptions WHERE rulesetId = :rulesetId")
  fun observeForRuleset(rulesetId: String): Flow<List<RulesetExceptionEntity>>

  @Upsert
  suspend fun upsertAll(exceptions: List<RulesetExceptionEntity>)
}
