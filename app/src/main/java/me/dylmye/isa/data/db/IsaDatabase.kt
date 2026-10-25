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

    fun build(context: Context): IsaDatabase {
      val applicationContext = context.applicationContext
      return Room.databaseBuilder(applicationContext, IsaDatabase::class.java, NAME)
        // Every version since v2 has an auto-migration above. The fallback remains only for v1
        // databases from before the first release: there is no upgrade path from them and none is
        // wanted, so they are dropped and recreated.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(SeedData(applicationContext).callback)
        .build()
    }
  }
}
