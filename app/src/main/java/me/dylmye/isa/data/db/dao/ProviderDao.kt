package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.ProviderEntity

@Dao
interface ProviderDao {
  @Query("SELECT * FROM providers ORDER BY name")
  fun observeAll(): Flow<List<ProviderEntity>>

  @Query("SELECT * FROM providers WHERE _id = :id")
  fun observeById(id: String): Flow<ProviderEntity?>

  @Upsert
  suspend fun upsertAll(providers: List<ProviderEntity>)
}
