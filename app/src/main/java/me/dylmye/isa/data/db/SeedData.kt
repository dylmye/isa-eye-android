package me.dylmye.isa.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds reference data (providers, product types, rulesets and their exceptions) from
 * `assets/seed.sql`, on first create and whenever [SEED_VERSION] advances.
 *
 * `seed.sql` is generated from the web app's seed data by `scripts/generate-seed.mjs` and every row
 * is an idempotent upsert, so re-applying it refreshes reference data in place without touching user
 * data.
 */
internal class SeedData(context: Context) {
  private val assets = context.assets

  val callback =
    object : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        applySeed(db)
      }

      override fun onOpen(db: SupportSQLiteDatabase) {
        if (appliedSeedVersion(db) != SEED_VERSION) {
          applySeed(db)
        }
      }
    }

  private fun applySeed(db: SupportSQLiteDatabase) {
    assets.open(SEED_ASSET).bufferedReader().useLines { lines ->
      lines.filter { it.isNotBlank() }.forEach(db::execSQL)
    }
    db.execSQL(
      "INSERT OR REPLACE INTO seedState (_id, seedVersion) VALUES ($SEED_ID, $SEED_VERSION)",
    )
  }

  private fun appliedSeedVersion(db: SupportSQLiteDatabase): Int =
    db.query("SELECT seedVersion FROM seedState WHERE _id = $SEED_ID").use { cursor ->
      if (cursor.moveToFirst()) cursor.getInt(0) else NO_SEED
    }

  internal companion object {
    /** Bump when `seed/` changes so existing installs re-apply the seed. */
    const val SEED_VERSION = 2

    private const val SEED_ASSET = "seed.sql"
    private const val SEED_ID = 1
    private const val NO_SEED = 0
  }
}
