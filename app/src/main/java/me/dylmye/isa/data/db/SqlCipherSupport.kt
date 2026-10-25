package me.dylmye.isa.data.db

import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook

/** Shared SQLCipher setup: the native library and the pragmas we always apply. */
internal object SqlCipherSupport {
  @Volatile private var loaded = false

  @Synchronized
  fun load() {
    if (!loaded) {
      System.loadLibrary("sqlcipher")
      loaded = true
    }
  }

  fun hook(): SQLiteDatabaseHook = object : SQLiteDatabaseHook {
    override fun preKey(connection: SQLiteConnection) = Unit

    override fun postKey(connection: SQLiteConnection) {
      connection.execute("PRAGMA cipher_memory_security = ON", null, null)
    }
  }
}
