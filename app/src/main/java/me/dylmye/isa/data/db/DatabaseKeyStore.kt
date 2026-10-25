package me.dylmye.isa.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and stores the SQLCipher database key.
 *
 * The random 256-bit key is wrapped by an AES-256-GCM key in AndroidKeyStore. Only the wrapped
 * bytes live on disk, in `noBackupFilesDir`, so Auto Backup and device transfer can never carry the
 * key with the database. The key is never shown to the user and is never logged.
 *
 * The key is returned as hex because SQLCipher accepts raw keys as `x'...'` literals, which also
 * lets the export importer re-encrypt a backup with the same key without treating random bytes as a
 * text passphrase.
 */
internal class DatabaseKeyStore(context: Context) {
  private val appContext = context.applicationContext
  private val keyFile = File(appContext.noBackupFilesDir, KEY_FILE_NAME)

  /**
   * Returns the existing key, or creates and persists one when [createIfMissing] is true.
   *
   * A null result with [createIfMissing] false means there is no usable key. Callers must not
   * create a new key while an encrypted database exists: that would make the database unreadable.
   */
  @Synchronized
  fun keyHex(createIfMissing: Boolean): String? {
    val existing = readKey()
    if (existing == null && createIfMissing) {
      // A key file that cannot be unwrapped is useless. Drop it and its alias so a fresh key can
      // be generated; the caller is responsible for removing an orphaned database first.
      if (keyFile.exists()) {
        keyFile.delete()
        deleteKeystoreKey()
      }
      return generateAndStore()?.toHexAndClear()
    }
    return existing?.toHexAndClear()
  }

  /** The key used to re-encrypt an imported database; an import cannot proceed without it. */
  @Synchronized
  fun existingKeyHex(): String? = readKey()?.toHexAndClear()

  /** Removes the wrapped key and its Keystore alias. Only used when starting over. */
  @Synchronized
  fun clear() {
    keyFile.delete()
    deleteKeystoreKey()
  }

  private fun generateAndStore(): ByteArray? {
    val key = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
    return try {
      writeKey(key)
      key
    } catch (_: GeneralSecurityException) {
      key.fill(0)
      null
    } catch (_: java.io.IOException) {
      key.fill(0)
      null
    }
  }

  @Suppress("ReturnCount") // Early exits validate the binary header before decrypting.
  private fun readKey(): ByteArray? {
    if (!keyFile.exists()) return null
    return try {
      DataInputStream(FileInputStream(keyFile).buffered()).use { input ->
        if (input.readInt() != MAGIC || input.readInt() != VERSION) return null
        val ivLength = input.readInt()
        if (ivLength !in 1..MAX_IV_BYTES) return null
        val iv = ByteArray(ivLength)
        input.readFully(iv)
        val cipherLength = input.readInt()
        if (cipherLength !in 1..MAX_CIPHERTEXT_BYTES) return null
        val ciphertext = ByteArray(cipherLength)
        input.readFully(ciphertext)

        Cipher.getInstance(TRANSFORMATION).run {
          init(Cipher.DECRYPT_MODE, keystoreKey() ?: return null, GCMParameterSpec(TAG_BITS, iv))
          updateAAD(AAD)
          doFinal(ciphertext)
        }
      }
    } catch (_: GeneralSecurityException) {
      null
    } catch (_: java.io.IOException) {
      null
    } catch (_: RuntimeException) {
      null
    }
  }

  private fun writeKey(key: ByteArray) {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
    cipher.updateAAD(AAD)
    val ciphertext = cipher.doFinal(key)
    val iv = cipher.iv

    keyFile.parentFile?.mkdirs()
    val temporary = File(keyFile.parentFile, "${keyFile.name}.tmp")
    FileOutputStream(temporary).use { file ->
      val output = DataOutputStream(file)
      output.writeInt(MAGIC)
      output.writeInt(VERSION)
      output.writeInt(iv.size)
      output.write(iv)
      output.writeInt(ciphertext.size)
      output.write(ciphertext)
      output.flush()
      // Sync before the stream is closed; syncing the descriptor after close throws.
      file.fd.sync()
    }
    if (!temporary.renameTo(keyFile)) {
      keyFile.delete()
      if (!temporary.renameTo(keyFile)) {
        temporary.delete()
        throw java.io.IOException("Could not store the database key")
      }
    }
  }

  private fun keystoreKey(): SecretKey? {
    val keystore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    return keystore.getKey(KEY_ALIAS, null) as? SecretKey
  }

  private fun getOrCreateKeystoreKey(): SecretKey {
    keystoreKey()?.let {
      return it
    }
    // A stale alias without a wrapped key cannot be unwrapped; recreate it.
    deleteKeystoreKey()
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
      .apply {
        init(
          KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build(),
        )
      }
      .generateKey()
  }

  private fun deleteKeystoreKey() {
    runCatching {
      KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
    }
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

  private fun ByteArray.toHexAndClear(): String = try {
    toHex()
  } finally {
    fill(0)
  }

  private companion object {
    const val KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "isa_eye_db_key_v1"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val TAG_BITS = 128
    const val KEY_BYTES = 32
    const val KEY_FILE_NAME = "db_key.bin"
    const val MAGIC = 0x49454B31 // "IEK1"
    const val VERSION = 1
    const val MAX_IV_BYTES = 32
    const val MAX_CIPHERTEXT_BYTES = 128
    val AAD = "isa-eye-db-key-v1".toByteArray(Charsets.UTF_8)
  }
}
