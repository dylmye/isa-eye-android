package me.dylmye.isa.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds SQL-level column defaults for `providers.colour`, `products.flexible` and
 * `rulesetExceptions.includedInShared`. SQLite cannot change a column default in place, so each
 * affected table is recreated and its rows copied across.
 */
internal val MIGRATION_1_2 =
  object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      recreateProviders(db)
      recreateProducts(db)
      recreateRulesetExceptions(db)
    }
  }

private fun recreateProviders(db: SupportSQLiteDatabase) {
  db.execSQL(
    "CREATE TABLE IF NOT EXISTS `providers_new` (`_id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
      "`iconRelativeUrl` TEXT, `colour` TEXT NOT NULL DEFAULT '#ffffff', PRIMARY KEY(`_id`))",
  )
  db.execSQL(
    "INSERT INTO `providers_new` (`_id`, `name`, `iconRelativeUrl`, `colour`) " +
      "SELECT `_id`, `name`, `iconRelativeUrl`, `colour` FROM `providers`",
  )
  db.execSQL("DROP TABLE `providers`")
  db.execSQL("ALTER TABLE `providers_new` RENAME TO `providers`")
}

private fun recreateProducts(db: SupportSQLiteDatabase) {
  db.execSQL(
    "CREATE TABLE IF NOT EXISTS `products_new` (`_id` TEXT NOT NULL, " +
      "`startTaxYear` TEXT NOT NULL, `endTaxYear` TEXT, `providerId` TEXT NOT NULL, " +
      "`friendlyName` TEXT NOT NULL, `productTypeCode` TEXT NOT NULL, " +
      "`flexible` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`_id`), " +
      "FOREIGN KEY(`providerId`) REFERENCES `providers`(`_id`) " +
      "ON UPDATE NO ACTION ON DELETE NO ACTION, " +
      "FOREIGN KEY(`productTypeCode`) REFERENCES `productTypes`(`_id`) " +
      "ON UPDATE NO ACTION ON DELETE NO ACTION)",
  )
  db.execSQL(
    "INSERT INTO `products_new` (`_id`, `startTaxYear`, `endTaxYear`, `providerId`, " +
      "`friendlyName`, `productTypeCode`, `flexible`) " +
      "SELECT `_id`, `startTaxYear`, `endTaxYear`, `providerId`, " +
      "`friendlyName`, `productTypeCode`, `flexible` FROM `products`",
  )
  db.execSQL("DROP TABLE `products`")
  db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
  db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_providerId` ON `products` (`providerId`)")
  db.execSQL(
    "CREATE INDEX IF NOT EXISTS `index_products_productTypeCode` " +
      "ON `products` (`productTypeCode`)",
  )
}

private fun recreateRulesetExceptions(db: SupportSQLiteDatabase) {
  db.execSQL(
    "CREATE TABLE IF NOT EXISTS `rulesetExceptions_new` (`_id` TEXT NOT NULL, " +
      "`productTypeId` TEXT NOT NULL, `rulesetId` TEXT NOT NULL, " +
      "`allowancePence` INTEGER NOT NULL, `notes` TEXT, " +
      "`includedInShared` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`_id`), " +
      "FOREIGN KEY(`productTypeId`) REFERENCES `productTypes`(`_id`) " +
      "ON UPDATE NO ACTION ON DELETE NO ACTION, " +
      "FOREIGN KEY(`rulesetId`) REFERENCES `rulesets`(`_id`) " +
      "ON UPDATE NO ACTION ON DELETE NO ACTION)",
  )
  db.execSQL(
    "INSERT INTO `rulesetExceptions_new` (`_id`, `productTypeId`, `rulesetId`, `allowancePence`, " +
      "`notes`, `includedInShared`) " +
      "SELECT `_id`, `productTypeId`, `rulesetId`, `allowancePence`, " +
      "`notes`, `includedInShared` FROM `rulesetExceptions`",
  )
  db.execSQL("DROP TABLE `rulesetExceptions`")
  db.execSQL("ALTER TABLE `rulesetExceptions_new` RENAME TO `rulesetExceptions`")
  db.execSQL(
    "CREATE INDEX IF NOT EXISTS `index_rulesetExceptions_rulesetId` " +
      "ON `rulesetExceptions` (`rulesetId`)",
  )
  db.execSQL(
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_rulesetExceptions_productTypeId_rulesetId` " +
      "ON `rulesetExceptions` (`productTypeId`, `rulesetId`)",
  )
}
