package me.dylmye.isa.data

import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase

/**
 * The small slice of account data the free-limit policy needs. Kept separate from
 * [AccountRepository] so the count query is not mixed into the general account API.
 */
class AccountLimitRepository(private val database: IsaDatabase) {
  /** The total number of accounts, regardless of tax year or whether an account is closed. */
  fun observeAccountCount(): Flow<Int> = database.productDao().observeCount()

  /** One-shot form of [observeAccountCount], used by the add-account gate. */
  suspend fun countAccounts(): Int = database.productDao().count()
}
