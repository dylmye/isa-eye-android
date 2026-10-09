package me.dylmye.isa.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.entity.ProductEntity

@Dao
interface ProductDao {
  @Query(
    "SELECT p._id AS productId, " +
      "p.friendlyName AS friendlyName, " +
      "p.productTypeId AS productTypeId, " +
      "pr._id AS providerId, " +
      "pr.name AS providerName, " +
      "pr.colour AS providerColour " +
      "FROM products p " +
      "JOIN providers pr ON pr._id = p.providerId " +
      "ORDER BY p.friendlyName",
  )
  fun observeSummaries(): Flow<List<AccountSummary>>

  @Upsert
  suspend fun upsert(product: ProductEntity)

  @Upsert
  suspend fun upsertAll(products: List<ProductEntity>)
}
