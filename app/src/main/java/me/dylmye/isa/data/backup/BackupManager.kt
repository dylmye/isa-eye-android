// Backup I/O must never crash the app; every failure maps to a user-facing result.
@file:Suppress("TooGenericExceptionCaught")

package me.dylmye.isa.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.DatabaseKeyStore
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.SqlCipherSupport
import me.dylmye.isa.notifications.ReminderScheduler
import me.dylmye.isa.widgets.WidgetUpdater
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val BACKUP_APPLICATION_ID = 0x49534145 // "ISAE"
internal const val BACKUP_EXTENSION = ".isabackup"
internal const val BACKUP_FOLDER_NAME = "IsaEye"

data class BackupFile(val uri: Uri, val name: String, val lastModified: Long, val sizeBytes: Long)

enum class BackupError {
  NoFolder,
  FolderUnavailable,
  InvalidFile,
  WrongPassphrase,
  EncryptedBackup,
  UnsupportedVersion,
  ExportFailed,
  ImportFailed,
  Io,
}

internal class BackupException(
  val error: BackupError,
  cause: Throwable? = null,
  val reloadRequired: Boolean = false,
) : Exception(cause)

/**
 * Manual backup/restore: the only copy of the data that leaves the device. The live database is
 * always encrypted with the Keystore-wrapped device key; an export is re-encrypted with the optional
 * user passphrase (or left as plaintext when none is given), and an import is re-encrypted with the
 * device key before it replaces `isa-eye.db`. One small function per step keeps the transaction
 * readable.
 */
@Suppress("TooManyFunctions")
internal class BackupManager(
  context: Context,
  private val application: IsaEyeApplication,
  private val preferences: UserPreferences,
) {
  private val appContext = context.applicationContext
  private val resolver = appContext.contentResolver
  private val keyStore = DatabaseKeyStore(appContext)

  fun selectedFolder(): DocumentFile? {
    val uri = preferences.backupFolderUri?.let(Uri::parse)
    val folder = uri?.let { DocumentFile.fromTreeUri(appContext, it) }
    val usable = folder?.let { it.exists() && it.isDirectory && it.canRead() } == true
    if (!usable) preferences.backupFolderUri = null
    return folder?.takeIf { usable }
  }

  /**
   * The folder ISA Eye owns inside the picked folder. Files never go directly into the folder the
   * user picks: they live in `Backups/IsaEye`. If the user picks `IsaEye` itself, use it directly.
   */
  fun backupFolder(create: Boolean = false): DocumentFile? {
    val root = selectedFolder() ?: return null
    val existing =
      if (root.name == BACKUP_FOLDER_NAME) {
        root
      } else {
        root.findFile(BACKUP_FOLDER_NAME)?.takeIf { it.isDirectory }
      }
    return existing
      ?: if (create) root.createDirectory(BACKUP_FOLDER_NAME)?.takeIf { it.isDirectory } else null
  }

  fun selectedFolderName(): String? {
    val root = selectedFolder() ?: return null
    return if (root.name == BACKUP_FOLDER_NAME) {
      BACKUP_FOLDER_NAME
    } else {
      "${root.name ?: "Backups"}/$BACKUP_FOLDER_NAME"
    }
  }

  fun saveFolder(uri: Uri) {
    try {
      resolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (error: SecurityException) {
      throw BackupException(BackupError.FolderUnavailable, error)
    }
    preferences.backupFolderUri = uri.toString()
  }

  fun listBackups(): List<BackupFile> = backupFolder(create = false)
    ?.listFiles()
    ?.asSequence()
    ?.filter { it.isFile && it.name?.endsWith(BACKUP_EXTENSION, ignoreCase = true) == true }
    ?.mapNotNull { file ->
      val name = file.name ?: return@mapNotNull null
      BackupFile(file.uri, name, file.lastModified(), file.length())
    }
    ?.sortedByDescending { it.lastModified }
    ?.toList()
    .orEmpty()

  suspend fun isEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
    resolver.openInputStream(uri)?.use { input ->
      val header = ByteArray(SQLITE_HEADER.size)
      val read = input.read(header)
      if (read < SQLITE_HEADER.size) throw BackupException(BackupError.InvalidFile)
      !header.contentEquals(SQLITE_HEADER)
    } ?: throw BackupException(BackupError.InvalidFile)
  }

  suspend fun export(
    passphrase: String?,
    folder: DocumentFile? = backupFolder(create = true),
  ): BackupFile = withContext(Dispatchers.IO) {
    val targetFolder = folder ?: throw BackupException(BackupError.FolderUnavailable)
    if (!targetFolder.exists() || !targetFolder.canWrite()) {
      throw BackupException(BackupError.FolderUnavailable)
    }

    val temporary = File.createTempFile("isa-eye-export-", ".db", appContext.cacheDir)
    try {
      val database = application.database.openHelper.writableDatabase
      val version = queryInt(database, "PRAGMA user_version")
      database.execSQL(
        "ATTACH DATABASE ${sqlString(temporary.absolutePath)} AS backup KEY " +
          keyClause(passphrase),
      )
      try {
        database.query("SELECT sqlcipher_export('backup')").use { it.moveToFirst() }
        database.execSQL("PRAGMA backup.application_id = $BACKUP_APPLICATION_ID")
        database.execSQL("PRAGMA backup.user_version = $version")
      } finally {
        runCatching { database.execSQL("DETACH DATABASE backup") }
      }

      verifyExport(temporary, passphrase, version)
      writeToFolder(targetFolder, temporary)
    } catch (error: BackupException) {
      throw error
    } catch (error: Exception) {
      throw BackupException(BackupError.ExportFailed, error)
    } finally {
      temporary.delete()
    }
  }

  suspend fun import(source: Uri, passphrase: String?): Unit = withContext(Dispatchers.IO) {
    SqlCipherSupport.load()
    val temporary = copyToCache(source)
    try {
      val plaintext = temporary.isPlaintext()
      if (!plaintext && passphrase.isNullOrEmpty()) {
        throw BackupException(BackupError.EncryptedBackup)
      }

      val databaseFile = appContext.getDatabasePath(IsaDatabase.NAME)
      val liveVersion =
        queryInt(application.database.openHelper.writableDatabase, "PRAGMA user_version")
      val liveIdentity = identityHash(application.database.openHelper.writableDatabase)

      val sourceDatabase = open(temporary, if (plaintext) null else passphrase)
      try {
        validateSource(sourceDatabase, liveVersion, liveIdentity, encrypted = !plaintext)
        // Build the replacement in cache first: attaching a second database inside the live
        // `databases/` directory can fail with SQLITE_CANTOPEN while the live DB is open.
        val replacement = File.createTempFile("isa-eye-import-", ".db", appContext.cacheDir)
        replacement.delete()
        replacement.deleteSidecars()

        val deviceKey =
          keyStore.existingKeyHex() ?: throw BackupException(BackupError.ImportFailed)
        sourceDatabase.execSQL(
          "ATTACH DATABASE ${sqlString(replacement.absolutePath)} AS target KEY " +
            sqlString(deviceKey),
        )
        try {
          sourceDatabase.query("SELECT sqlcipher_export('target')").use { it.moveToFirst() }
          sourceDatabase.execSQL("PRAGMA target.application_id = $BACKUP_APPLICATION_ID")
          sourceDatabase.execSQL(
            "PRAGMA target.user_version = ${queryInt(sourceDatabase, "PRAGMA user_version")}",
          )
        } finally {
          runCatching { sourceDatabase.execSQL("DETACH DATABASE target") }
        }
        replacement.deleteSidecars()
        try {
          swapIn(replacement)
        } finally {
          // A successful swap renames the file, so this is a no-op; a failed one leaves it in cache.
          replacement.delete()
        }
      } finally {
        sourceDatabase.close()
      }
    } catch (error: BackupException) {
      Log.e(TAG, "Import failed: ${error.error}", error)
      throw error
    } catch (error: Exception) {
      Log.e(TAG, "Import failed", error)
      throw BackupException(BackupError.ImportFailed, error)
    } finally {
      temporary.delete()
    }
  }

  private fun check(condition: Boolean, error: BackupError) {
    if (!condition) throw BackupException(error)
  }

  private fun fail(
    error: BackupError,
    cause: Throwable? = null,
    reloadRequired: Boolean = false,
  ): Nothing = throw BackupException(error, cause, reloadRequired)

  private fun verifyExport(file: File, passphrase: String?, version: Int) {
    val database = open(file, passphrase)
    try {
      check(
        queryInt(database, "PRAGMA application_id") == BACKUP_APPLICATION_ID,
        BackupError.ExportFailed,
      )
      check(queryInt(database, "PRAGMA user_version") == version, BackupError.ExportFailed)
      check(queryString(database, "PRAGMA integrity_check") == "ok", BackupError.ExportFailed)
      if (!passphrase.isNullOrEmpty()) {
        checkCipherIntegrity(database, BackupError.ExportFailed)
      }
      queryString(database, "SELECT identity_hash FROM room_master_table")
    } finally {
      database.close()
    }
  }

  private fun validateSource(
    database: SQLiteDatabase,
    liveVersion: Int,
    liveIdentity: String,
    encrypted: Boolean,
  ) {
    check(
      queryInt(database, "PRAGMA application_id") == BACKUP_APPLICATION_ID,
      BackupError.InvalidFile,
    )
    check(queryInt(database, "PRAGMA user_version") == liveVersion, BackupError.UnsupportedVersion)
    check(
      queryString(database, "SELECT identity_hash FROM room_master_table") == liveIdentity,
      BackupError.UnsupportedVersion,
    )
    check(queryString(database, "PRAGMA integrity_check") == "ok", BackupError.InvalidFile)
    if (encrypted) {
      checkCipherIntegrity(database, BackupError.InvalidFile)
    }
  }

  /** SQLCipher's HMAC-based page check; it returns rows only when it finds damage. */
  private fun checkCipherIntegrity(database: SupportSQLiteDatabase, error: BackupError) {
    database.query("PRAGMA cipher_integrity_check").use { cursor ->
      if (cursor.moveToFirst()) fail(error)
    }
  }

  private suspend fun swapIn(replacement: File) {
    val databaseFile = appContext.getDatabasePath(IsaDatabase.NAME)
    val previous = File("${databaseFile.path}.old")
    previous.delete()
    previous.deleteSidecars()

    application.closeDatabaseForImport()
    var movedPrevious = false
    try {
      if (databaseFile.exists()) {
        if (!databaseFile.renameTo(previous)) {
          fail(BackupError.ImportFailed, reloadRequired = true)
        }
        movedPrevious = true
      }
      databaseFile.deleteSidecars()
      if (!replacement.renameTo(databaseFile)) {
        fail(BackupError.ImportFailed, reloadRequired = true)
      }

      // Force Room to open the replacement and validate its schema before dropping the old copy.
      application.database.openHelper.writableDatabase.query(
        "SELECT count(*) FROM sqlite_master",
      ).use {
        it.moveToFirst()
      }
      previous.delete()
      previous.deleteSidecars()
    } catch (error: Exception) {
      application.closeDatabaseForImport()
      if (movedPrevious) {
        databaseFile.delete()
        databaseFile.deleteSidecars()
        if (previous.exists()) {
          previous.renameTo(databaseFile)
          runCatching { application.database.openHelper.writableDatabase }
        }
      }
      if (error is BackupException) throw error
      throw BackupException(BackupError.ImportFailed, error, reloadRequired = true)
    }

    runCatching { WidgetUpdater.updateAll(appContext) }
    runCatching { ReminderScheduler.ensureScheduled(appContext) }
  }

  private fun writeToFolder(folder: DocumentFile, temporary: File): BackupFile {
    val name = uniqueName(folder)
    val document =
      folder.createFile("application/octet-stream", name) ?: fail(BackupError.ExportFailed)
    if (document.name != name) {
      runCatching { document.renameTo(name) }
    }
    try {
      resolver.openOutputStream(document.uri, "wt")?.use { output ->
        temporary.inputStream().use { input -> input.copyTo(output) }
      } ?: fail(BackupError.ExportFailed)
    } catch (error: Exception) {
      runCatching { document.delete() }
      if (error is BackupException) throw error
      throw BackupException(BackupError.ExportFailed, error)
    }
    return BackupFile(
      uri = document.uri,
      name = document.name ?: name,
      lastModified = document.lastModified(),
      sizeBytes = document.length(),
    )
  }

  private fun uniqueName(folder: DocumentFile): String {
    val stamp =
      SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
    var name = "isa-eye-backup-$stamp$BACKUP_EXTENSION"
    var suffix = 2
    while (folder.findFile(name) != null) {
      name = "isa-eye-backup-$stamp-$suffix$BACKUP_EXTENSION"
      suffix++
    }
    return name
  }

  private fun copyToCache(source: Uri): File {
    val temporary = File.createTempFile("isa-eye-import-", ".db", appContext.cacheDir)
    try {
      resolver.openInputStream(source)?.use { input ->
        temporary.outputStream().use { output -> input.copyTo(output) }
      } ?: fail(BackupError.InvalidFile)
      if (temporary.length() > MAX_BACKUP_BYTES) {
        fail(BackupError.InvalidFile)
      }
      return temporary
    } catch (error: Exception) {
      temporary.delete()
      if (error is BackupException) throw error
      throw BackupException(BackupError.Io, error)
    }
  }

  private fun open(file: File, passphrase: String?): SQLiteDatabase = try {
    SQLiteDatabase.openDatabase(
      file.absolutePath,
      passphrase?.toByteArray(Charsets.UTF_8),
      null,
      SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
      null,
    )
  } catch (error: SQLiteException) {
    throw BackupException(BackupError.WrongPassphrase, error)
  }

  private fun File.isPlaintext(): Boolean {
    if (!exists() || length() < SQLITE_HEADER.size) return false
    return try {
      FileInputStream(this).use { input ->
        val header = ByteArray(SQLITE_HEADER.size)
        input.read(header) == SQLITE_HEADER.size && header.contentEquals(SQLITE_HEADER)
      }
    } catch (_: java.io.IOException) {
      false
    }
  }

  private fun queryInt(database: SupportSQLiteDatabase, sql: String): Int =
    database.query(sql).use { cursor ->
      if (!cursor.moveToFirst()) throw BackupException(BackupError.InvalidFile)
      cursor.getInt(0)
    }

  private fun queryString(database: SupportSQLiteDatabase, sql: String): String =
    database.query(sql).use { cursor ->
      if (!cursor.moveToFirst()) throw BackupException(BackupError.InvalidFile)
      cursor.getString(0) ?: throw BackupException(BackupError.InvalidFile)
    }

  private fun identityHash(database: SupportSQLiteDatabase): String =
    queryString(database, "SELECT identity_hash FROM room_master_table")

  private fun File.deleteSidecars() {
    File("$path-wal").delete()
    File("$path-shm").delete()
    File("$path-journal").delete()
  }

  private fun sqlString(value: String): String = "'${value.replace("'", "''")}'"

  private fun keyClause(passphrase: String?): String =
    if (passphrase.isNullOrEmpty()) "''" else sqlString(passphrase)

  private companion object {
    const val TAG = "BackupManager"
    val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)
    const val MAX_BACKUP_BYTES = 100L * 1024L * 1024L
  }
}
