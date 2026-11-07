package me.dylmye.isa

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.dylmye.isa.data.backup.BACKUP_MIME_TYPE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The manifest should offer ISA Eye only for ISA Eye's own backup files. */
@RunWith(AndroidJUnit4::class)
class FileHandlerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  private fun handledByIsaEye(uri: String, type: String): Boolean {
    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(uri), type)
        addCategory(Intent.CATEGORY_DEFAULT)
      }
    return context.packageManager
      .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
      .any { it.activityInfo.packageName == context.packageName }
  }

  @Test
  fun customMimeFile_opensWithIsaEye() {
    assertTrue(
      handledByIsaEye("file:///sdcard/Download/isa-eye-backup.isabackup", BACKUP_MIME_TYPE),
    )
  }

  @Test
  fun customMimeContentUri_opensWithIsaEye() {
    assertTrue(
      handledByIsaEye(
        "content://com.example.documents/document/msf%3A100",
        BACKUP_MIME_TYPE,
      ),
    )
  }

  @Test
  fun legacyOctetStreamContentBackup_opensWithIsaEye() {
    assertTrue(
      handledByIsaEye(
        "content://com.example.documents/document/primary%3ADownload%2Fbackup.isabackup",
        "application/octet-stream",
      ),
    )
  }

  @Test
  fun legacyOctetStreamFileBackup_opensWithIsaEye() {
    assertTrue(
      handledByIsaEye("file:///sdcard/Download/backup.isabackup", "application/octet-stream"),
    )
  }

  @Test
  fun legacyOctetStreamMediaStoreUri_opensWithIsaEye() {
    assertTrue(
      handledByIsaEye("content://media/external/file/5039", "application/octet-stream"),
    )
  }

  @Test
  fun otherBinaryFile_doesNotOpenWithIsaEye() {
    assertFalse(
      handledByIsaEye("file:///sdcard/Download/photo.jpg", "application/octet-stream"),
    )
  }

  @Test
  fun octetStreamWithoutBackupExtension_doesNotOpenWithIsaEye() {
    assertFalse(
      handledByIsaEye(
        "content://com.example.documents/document/msf%3A100",
        "application/octet-stream",
      ),
    )
  }

  @Test
  fun backupWithAnotherMimeType_doesNotOpenWithIsaEye() {
    assertFalse(
      handledByIsaEye("file:///sdcard/Download/isa-eye-backup.isabackup", "image/jpeg"),
    )
  }
}
