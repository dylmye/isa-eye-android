package me.dylmye.isa.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds reference data (providers, product types, rulesets and their exceptions) from
 * `assets/seed.sql` the first time the database is created.
 *
 * `seed.sql` is generated from the web app's seed data by `scripts/generate-seed.mjs`.
 */
internal class SeedData(context: Context) {
  private val assets = context.assets

  val callback =
    object : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        assets.open(SEED_ASSET).bufferedReader().useLines { lines ->
          lines.filter { it.isNotBlank() }.forEach(db::execSQL)
        }
      }
    }

  private companion object {
    const val SEED_ASSET = "seed.sql"
  }
}
