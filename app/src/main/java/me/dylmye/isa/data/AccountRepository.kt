package me.dylmye.isa.data

import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.dao.AccountSummary

/** Read/write access to ISA accounts and the reference data that supports them. */
class AccountRepository(private val database: IsaDatabase) {
  fun observeAccounts(): Flow<List<AccountSummary>> = database.productDao().observeSummaries()
}
