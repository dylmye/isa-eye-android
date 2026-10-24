package me.dylmye.isa.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.AccountOrderEntity

/** Read/write access to the order the user has dragged each tax year's accounts into. */
class AccountOrderRepository(private val database: IsaDatabase) {
  /** Every saved place, one row per placed account. */
  fun observeAll(): Flow<List<AccountOrderEntity>> = database.accountOrderDao().observeAll()

  /**
   * Saves [productIds] as the top-to-bottom order of [rulesetId]'s accounts. The whole year is
   * replaced in one transaction, so a half-written order can never be read.
   */
  suspend fun save(rulesetId: String, productIds: List<String>) {
    database.withTransaction {
      database.accountOrderDao().deleteForRuleset(rulesetId)
      database.accountOrderDao().upsertAll(
        productIds.mapIndexed { position, productId ->
          AccountOrderEntity(rulesetId = rulesetId, productId = productId, position = position)
        },
      )
    }
  }
}
