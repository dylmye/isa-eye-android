package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.AutoContributionEntity

@Dao
interface AutoContributionDao {
  @Query("SELECT * FROM autoContributions WHERE productId = :productId")
  fun observeForProduct(productId: String): Flow<List<AutoContributionEntity>>

  @Query("SELECT * FROM autoContributions")
  suspend fun getAll(): List<AutoContributionEntity>

  @Query("SELECT * FROM autoContributions WHERE productId = :productId AND rulesetId = :rulesetId")
  suspend fun find(productId: String, rulesetId: String): AutoContributionEntity?

  @Upsert suspend fun upsert(schedule: AutoContributionEntity)

  @Query("DELETE FROM autoContributions WHERE productId = :productId AND rulesetId = :rulesetId")
  suspend fun delete(productId: String, rulesetId: String)
}
