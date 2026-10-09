package me.dylmye.isa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities mirroring the TinyBase table schema. Primary keys map to TinyBase's row-id column
 * (`_id`); cell columns keep the same names. Money is stored as integer pence ([Long]) rather than
 * TinyBase's strings.
 *
 * Referential actions encode ownership: rows a table *owns* cascade on delete, while user data that
 * merely references seeded reference data is restricted so reference-data cleanup can never silently
 * destroy a user's accounts.
 */
@Entity(tableName = "providers")
data class ProviderEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val name: String,
  val iconRelativeUrl: String?,
  @ColumnInfo(defaultValue = "'#ffffff'")
  val colour: String = "#ffffff",
)

@Entity(
  tableName = "providerAliases",
  foreignKeys = [
    ForeignKey(
      entity = ProviderEntity::class,
      parentColumns = ["_id"],
      childColumns = ["providerId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("providerId"), Index("alias")],
)
data class ProviderAliasEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val alias: String,
  val providerId: String,
)

@Entity(tableName = "rulesets")
data class RulesetEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val sharedAllowancePence: Long,
  val startDate: String,
  val endDate: String,
  val notes: String?,
)

@Entity(
  tableName = "productTypes",
  // `introducedWithRuleset`/`removedWithRuleset` are soft references to historical rulesets that can
  // predate the seeded range, so they are intentionally not enforced as foreign keys.
  indices = [Index("introducedWithRuleset"), Index("removedWithRuleset")],
)
data class ProductTypeEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val name: String,
  val introducedWithRuleset: String,
  val removedWithRuleset: String?,
  val shortDescription: String,
  val longDescription: String,
)

@Entity(
  tableName = "products",
  foreignKeys = [
    ForeignKey(
      entity = ProviderEntity::class,
      parentColumns = ["_id"],
      childColumns = ["providerId"],
      onDelete = ForeignKey.RESTRICT,
    ),
    ForeignKey(
      entity = ProductTypeEntity::class,
      parentColumns = ["_id"],
      childColumns = ["productTypeId"],
      onDelete = ForeignKey.RESTRICT,
    ),
  ],
  indices = [
    Index("providerId"),
    Index("productTypeId"),
    Index("startTaxYear"),
    Index("endTaxYear"),
  ],
)
data class ProductEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val startTaxYear: String,
  val endTaxYear: String?,
  val providerId: String,
  val friendlyName: String,
  val productTypeId: String,
  @ColumnInfo(defaultValue = "0")
  val flexible: Boolean,
)

@Entity(
  tableName = "annualBalances",
  primaryKeys = ["productId", "rulesetId"],
  foreignKeys = [
    ForeignKey(
      entity = ProductEntity::class,
      parentColumns = ["_id"],
      childColumns = ["productId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = RulesetEntity::class,
      parentColumns = ["_id"],
      childColumns = ["rulesetId"],
      onDelete = ForeignKey.RESTRICT,
    ),
  ],
  // `byTaxYear` from the TinyBase schema.
  indices = [Index("rulesetId")],
)
data class AnnualBalanceEntity(
  val productId: String,
  val rulesetId: String,
  val lastUpdatedDateUnix: Long,
  val deductedFromAllowancePence: Long,
)

@Entity(
  tableName = "rulesetExceptions",
  foreignKeys = [
    ForeignKey(
      entity = ProductTypeEntity::class,
      parentColumns = ["_id"],
      childColumns = ["productTypeId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = RulesetEntity::class,
      parentColumns = ["_id"],
      childColumns = ["rulesetId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index("rulesetId"),
    Index(value = ["productTypeId", "rulesetId"], unique = true),
  ],
)
data class RulesetExceptionEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: String,
  val productTypeId: String,
  val rulesetId: String,
  val allowancePence: Long,
  val notes: String?,
  @ColumnInfo(defaultValue = "1")
  val includedInShared: Boolean = true,
)

/** Single-row bookkeeping for the reference-data seed version currently applied. */
@Entity(tableName = "seedState")
data class SeedStateEntity(
  @PrimaryKey @ColumnInfo(name = "_id") val id: Int = 1,
  val seedVersion: Int,
)
