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
      "p.startTaxYear AS startTaxYear, " +
      "p.endTaxYear AS endTaxYear, " +
      "pr._id AS providerId, " +
      "pr.name AS providerName, " +
      "pr.iconRelativeUrl AS providerIconUrl, " +
      "pr.colour AS providerColour " +
      "FROM products p " +
      "JOIN providers pr ON pr._id = p.providerId " +
      "ORDER BY p.friendlyName",
  )
  fun observeSummaries(): Flow<List<AccountSummary>>

  @Query("SELECT * FROM products WHERE _id = :id")
  fun observeById(id: String): Flow<ProductEntity?>

  @Upsert
  suspend fun upsert(product: ProductEntity)

  @Upsert
  suspend fun upsertAll(products: List<ProductEntity>)
}
