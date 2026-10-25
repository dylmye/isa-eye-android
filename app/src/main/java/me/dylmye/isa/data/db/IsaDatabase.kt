package me.dylmye.isa.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.dylmye.isa.data.db.dao.AccountOrderDao
import me.dylmye.isa.data.db.dao.AnnualBalanceDao
import me.dylmye.isa.data.db.dao.AutoContributionDao
import me.dylmye.isa.data.db.dao.GoalDao
import me.dylmye.isa.data.db.dao.ProductDao
import me.dylmye.isa.data.db.dao.ProductTypeDao
import me.dylmye.isa.data.db.dao.ProviderAliasDao
import me.dylmye.isa.data.db.dao.ProviderDao
import me.dylmye.isa.data.db.dao.RulesetDao
import me.dylmye.isa.data.db.dao.RulesetExceptionDao
import me.dylmye.isa.data.db.entity.AccountOrderEntity
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.AutoContributionEntity
import me.dylmye.isa.data.db.entity.GoalEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderAliasEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import me.dylmye.isa.data.db.entity.SeedStateEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream

@Database(
  entities = [
    ProviderEntity::class,
    ProviderAliasEntity::class,
    RulesetEntity::class,
    ProductTypeEntity::class,
    ProductEntity::class,
    AnnualBalanceEntity::class,
    AutoContributionEntity::class,
    RulesetExceptionEntity::class,
    AccountOrderEntity::class,
    GoalEntity::class,
    SeedStateEntity::class,
  ],
  version = 6,
  exportSchema = true,
  autoMigrations = [
    AutoMigration(from = 2, to = 3),
    AutoMigration(from = 3, to = 4),
    AutoMigration(from = 4, to = 5),
    AutoMigration(from = 5, to = 6),
  ],
)
abstract class IsaDatabase : RoomDatabase() {
  abstract fun providerDao(): ProviderDao

  abstract fun providerAliasDao(): ProviderAliasDao

  abstract fun rulesetDao(): RulesetDao

  abstract fun productTypeDao(): ProductTypeDao

  abstract fun productDao(): ProductDao

  abstract fun annualBalanceDao(): AnnualBalanceDao

  abstract fun autoContributionDao(): AutoContributionDao

  abstract fun rulesetExceptionDao(): RulesetExceptionDao

  abstract fun goalDao(): GoalDao

  abstract fun accountOrderDao(): AccountOrderDao

  companion object {
    const val NAME = "isa-eye.db"

    private const val PLAINTEXT_HEADER = "SQLite format 3\u0000"
    private const val HEADER_BYTES = 16

    fun build(context: Context): IsaDatabase {
      SqlCipherSupport.load()
      val applicationContext = context.applicationContext
      recoverInterruptedImport(applicationContext)

      val databaseFile = applicationContext.getDatabasePath(NAME)
      // v1 intentionally replaces the pre-encryption database in place. Leaving a plaintext file
      // behind would defeat the point of encrypting it.
      if (databaseFile.exists() && isPlaintext(databaseFile)) {
        deleteDatabaseFiles(applicationContext)
      }

      val keys = DatabaseKeyStore(applicationContext)
      var keyHex = keys.keyHex(createIfMissing = !databaseFile.exists())
      if (keyHex == null) {
        // The database exists but its Keystore-wrapped key is gone or unreadable. There is no way
        // to decrypt it, so start over; a user backup can be imported from Help.
        deleteDatabaseFiles(applicationContext)
        keys.clear()
        keyHex =
          keys.keyHex(createIfMissing = true)
            ?: error("Could not create the database encryption key")
      }

      return Room.databaseBuilder(applicationContext, IsaDatabase::class.java, NAME)
        // Every version since v2 has an auto-migration above. The fallback remains only for v1
        // databases from before the first release: there is no upgrade path from them and none is
        // wanted, so they are dropped and recreated.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .openHelperFactory(
          SupportOpenHelperFactory(
            keyHex.toByteArray(Charsets.UTF_8),
            SqlCipherSupport.hook(),
            true,
          ),
        )
        .addCallback(SeedData(applicationContext).callback)
        .build()
    }

    /**
     * Repairs the small crash window in the import swap. The importer validates a replacement in
     * cache, renames the live database to `isa-eye.db.old`, then promotes the replacement. If the
     * process dies between those two renames, put the previous database back.
     */
    private fun recoverInterruptedImport(context: Context) {
      val databaseFile = context.getDatabasePath(NAME)
      val previous = File("${databaseFile.path}.old")
      if (!databaseFile.exists() && previous.exists()) {
        previous.renameTo(databaseFile)
      }
      // A leftover `.old` next to a valid database is harmless and is cleaned up after the next
      // successful import/reopen.
    }

    private fun isPlaintext(file: File): Boolean = try {
      FileInputStream(file).use { input ->
        val header = ByteArray(HEADER_BYTES)
        val read = input.read(header)
        read == HEADER_BYTES && String(header, Charsets.ISO_8859_1) == PLAINTEXT_HEADER
      }
    } catch (_: java.io.IOException) {
      false
    }

    private fun deleteDatabaseFiles(context: Context) {
      context.deleteDatabase(NAME)
      val databaseFile = context.getDatabasePath(NAME)
      File("${databaseFile.path}-wal").delete()
      File("${databaseFile.path}-shm").delete()
      File("${databaseFile.path}-journal").delete()
    }
  }
}
