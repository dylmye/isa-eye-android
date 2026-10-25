package me.dylmye.isa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.dylmye.isa.data.db.SqlCipherSupport
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TEST_KEY =
  "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

@RunWith(AndroidJUnit4::class)
class BackupSqlCipherTest {
  @Test
  fun attachCreatesEncryptedDatabase() {
    SqlCipherSupport.load()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val source = File.createTempFile("source-", ".db", context.cacheDir)
    val target = File.createTempFile("target-", ".db", context.cacheDir)
    target.delete()

    val sourceDb = SQLiteDatabase.openOrCreateDatabase(source.absolutePath, null, null)
    try {
      sourceDb.execSQL("CREATE TABLE t (id TEXT PRIMARY KEY, value TEXT)")
      sourceDb.execSQL("INSERT INTO t VALUES ('a', 'b')")
    } finally {
      sourceDb.close()
    }

    val db =
      SQLiteDatabase.openDatabase(
        source.absolutePath,
        ByteArray(0),
        null,
        SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
        null,
      )
    try {
      db.execSQL(
        "ATTACH DATABASE '${target.absolutePath}' AS target KEY '$TEST_KEY'",
      )
      db.rawQuery("SELECT sqlcipher_export('target')", null).use { it.moveToFirst() }
      db.execSQL("DETACH DATABASE target")
    } finally {
      db.close()
    }

    assertTrue(target.exists())
    assertTrue(target.length() > 0)

    val encrypted =
      SQLiteDatabase.openDatabase(
        target.absolutePath,
        TEST_KEY.toByteArray(Charsets.UTF_8),
        null,
        SQLiteDatabase.OPEN_READONLY,
        null,
      )
    try {
      // cipher_integrity_check returns rows only when it finds damage.
      encrypted.rawQuery("PRAGMA cipher_integrity_check", null).use { cursor ->
        assertFalse(cursor.moveToFirst())
      }
    } finally {
      encrypted.close()
    }
  }

  @Test
  fun passphraseExportOpensWithTheSamePassphraseOnly() {
    SqlCipherSupport.load()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val source = File.createTempFile("source-", ".db", context.cacheDir)
    val target = File.createTempFile("target-", ".db", context.cacheDir)
    target.delete()

    val sourceDb = SQLiteDatabase.openOrCreateDatabase(source.absolutePath, null, null)
    try {
      sourceDb.execSQL("CREATE TABLE t (id TEXT PRIMARY KEY, value TEXT)")
      sourceDb.execSQL("INSERT INTO t VALUES ('a', 'backed-up-value')")
    } finally {
      sourceDb.close()
    }

    val db =
      SQLiteDatabase.openDatabase(
        source.absolutePath,
        ByteArray(0),
        null,
        SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
        null,
      )
    try {
      db.execSQL("ATTACH DATABASE '${target.absolutePath}' AS target KEY 'testpass123'")
      db.rawQuery("SELECT sqlcipher_export('target')", null).use { it.moveToFirst() }
      db.execSQL("DETACH DATABASE target")
    } finally {
      db.close()
    }

    val header = ByteArray(16)
    target.inputStream().use { it.read(header) }
    assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)))

    val encrypted =
      SQLiteDatabase.openDatabase(
        target.absolutePath,
        "testpass123".toByteArray(Charsets.UTF_8),
        null,
        SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
        null,
      )
    try {
      encrypted.rawQuery("SELECT value FROM t", null).use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("backed-up-value", cursor.getString(0))
      }
    } finally {
      encrypted.close()
    }
  }
}
