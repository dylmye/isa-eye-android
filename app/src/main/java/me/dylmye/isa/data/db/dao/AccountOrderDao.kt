package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.AccountOrderEntity

@Dao
interface AccountOrderDao {
  @Query("SELECT * FROM accountOrder")
  fun observeAll(): Flow<List<AccountOrderEntity>>

  @Query("DELETE FROM accountOrder WHERE rulesetId = :rulesetId")
  suspend fun deleteForRuleset(rulesetId: String)

  @Upsert
  suspend fun upsertAll(rows: List<AccountOrderEntity>)
}
