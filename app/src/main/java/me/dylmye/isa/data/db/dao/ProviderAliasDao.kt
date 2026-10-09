package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.ProviderAliasEntity

@Dao
interface ProviderAliasDao {
  @Query("SELECT * FROM providerAliases WHERE providerId = :providerId ORDER BY alias")
  fun observeForProvider(providerId: String): Flow<List<ProviderAliasEntity>>

  @Query("SELECT * FROM providerAliases ORDER BY alias")
  fun observeAll(): Flow<List<ProviderAliasEntity>>

  @Upsert
  suspend fun upsertAll(aliases: List<ProviderAliasEntity>)
}
