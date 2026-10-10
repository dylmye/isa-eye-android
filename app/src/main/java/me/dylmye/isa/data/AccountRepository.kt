package me.dylmye.isa.data

import kotlinx.coroutines.flow.Flow
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderAliasEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity

/** Read/write access to ISA accounts and the reference data that supports them. */
class AccountRepository(private val database: IsaDatabase) {
  fun observeAccounts(): Flow<List<AccountSummary>> = database.productDao().observeSummaries()

  fun observeProduct(id: String): Flow<ProductEntity?> = database.productDao().observeById(id)

  fun observeProviders(): Flow<List<ProviderEntity>> = database.providerDao().observeAll()

  fun observeProviderAliases(): Flow<List<ProviderAliasEntity>> =
    database.providerAliasDao().observeAll()

  fun observeRulesets(): Flow<List<RulesetEntity>> = database.rulesetDao().observeAll()

  fun observeProductTypes(): Flow<List<ProductTypeEntity>> = database.productTypeDao().observeAll()

  /** The name of the product type [id], used to detect account names derived from the type. */
  suspend fun productTypeName(id: String): String? = database.productTypeDao().getName(id)

  suspend fun upsertProduct(product: ProductEntity) = database.productDao().upsert(product)
}
