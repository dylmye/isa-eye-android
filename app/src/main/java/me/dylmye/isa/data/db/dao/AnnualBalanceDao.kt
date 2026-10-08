package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity

@Dao
interface AnnualBalanceDao {
  @Query("SELECT * FROM annualBalances WHERE productId = :productId ORDER BY rulesetId")
  fun observeForProduct(productId: String): Flow<List<AnnualBalanceEntity>>

  @Upsert
  suspend fun upsertAll(balances: List<AnnualBalanceEntity>)
}
