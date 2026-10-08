package me.dylmye.isa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.dylmye.isa.data.db.dao.AnnualBalanceDao
import me.dylmye.isa.data.db.dao.ProductDao
import me.dylmye.isa.data.db.dao.ProductTypeDao
import me.dylmye.isa.data.db.dao.ProviderAliasDao
import me.dylmye.isa.data.db.dao.ProviderDao
import me.dylmye.isa.data.db.dao.RulesetDao
import me.dylmye.isa.data.db.dao.RulesetExceptionDao
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderAliasEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

@Database(
  entities = [
    ProviderEntity::class,
    ProviderAliasEntity::class,
    RulesetEntity::class,
    ProductTypeEntity::class,
    ProductEntity::class,
    AnnualBalanceEntity::class,
    RulesetExceptionEntity::class,
  ],
  version = 3,
  exportSchema = true,
)
abstract class IsaDatabase : RoomDatabase() {
  abstract fun providerDao(): ProviderDao

  abstract fun providerAliasDao(): ProviderAliasDao

  abstract fun rulesetDao(): RulesetDao

  abstract fun productTypeDao(): ProductTypeDao

  abstract fun productDao(): ProductDao

  abstract fun annualBalanceDao(): AnnualBalanceDao

  abstract fun rulesetExceptionDao(): RulesetExceptionDao

  companion object {
    const val NAME = "isa-eye.db"

    fun build(context: Context): IsaDatabase {
      val applicationContext = context.applicationContext
      return Room.databaseBuilder(applicationContext, IsaDatabase::class.java, NAME)
        .addCallback(SeedData(applicationContext).callback)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }
  }
}
