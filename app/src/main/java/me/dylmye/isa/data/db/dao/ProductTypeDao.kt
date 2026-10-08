package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.ProductTypeEntity

@Dao
interface ProductTypeDao {
  @Query("SELECT * FROM productTypes ORDER BY name")
  fun observeAll(): Flow<List<ProductTypeEntity>>

  @Upsert
  suspend fun upsertAll(productTypes: List<ProductTypeEntity>)
}
