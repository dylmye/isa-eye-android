package me.dylmye.isa.data

import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderAliasEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/** Read/write access to ISA accounts and the reference data that supports them. */
class AccountRepository(private val database: IsaDatabase) {
  fun observeAccounts(): Flow<List<AccountSummary>> = database.productDao().observeSummaries()

  fun observeProduct(id: String): Flow<ProductEntity?> = database.productDao().observeById(id)

  /**
   * Recorded contribution totals: for [productId] when one is given, otherwise for every account.
   */
  fun observeBalances(productId: String? = null): Flow<List<AnnualBalanceEntity>> =
    if (productId == null) {
      database.annualBalanceDao().observeAll()
    } else {
      database.annualBalanceDao().observeForProduct(productId)
    }

  fun observeProviders(): Flow<List<ProviderEntity>> = database.providerDao().observeAll()

  fun observeProviderAliases(): Flow<List<ProviderAliasEntity>> =
    database.providerAliasDao().observeAll()

  fun observeRulesets(): Flow<List<RulesetEntity>> = database.rulesetDao().observeAll()

  /** Per-type allowance exceptions, one row per product type and tax year that has its own limit. */
  fun observeRulesetExceptions(): Flow<List<RulesetExceptionEntity>> =
    database.rulesetExceptionDao().observeAll()

  fun observeProductTypes(): Flow<List<ProductTypeEntity>> = database.productTypeDao().observeAll()

  /** The name of the product type [id], used to detect account names derived from the type. */
  suspend fun productTypeName(id: String): String? = database.productTypeDao().getName(id)

  suspend fun upsertProduct(product: ProductEntity) = database.productDao().upsert(product)

  /** Records [balance] for its tax year, replacing any figure already stored for that year. */
  suspend fun upsertBalance(balance: AnnualBalanceEntity) {
    database.annualBalanceDao().upsert(balance)
  }
}
