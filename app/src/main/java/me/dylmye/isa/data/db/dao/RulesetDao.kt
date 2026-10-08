package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.RulesetEntity

@Dao
interface RulesetDao {
  @Query("SELECT * FROM rulesets ORDER BY startDate")
  fun observeAll(): Flow<List<RulesetEntity>>

  @Upsert
  suspend fun upsertAll(rulesets: List<RulesetEntity>)
}
