package me.dylmye.isa.screenshot

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.data.backup.BackupFile
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.backup.BackupRestoreContent
import me.dylmye.isa.ui.backup.BackupRestoreUiState
import me.dylmye.isa.ui.backup.BackupTab

@PreviewTest
@CompactFormPreviews
@Composable
fun BackupExportScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    Surface(modifier = Modifier.fillMaxSize()) {
      BackupRestoreContent(
        state = BackupRestoreUiState(folderName = "MyBackups/IsaEye"),
        onTabChange = {},
        onPassphraseChange = {},
        onImportPassphraseChange = {},
        onChooseFolder = {},
        onExport = {},
        onSelectBackup = {},
        onImport = {},
        onDone = {},
        onClearError = {},
      )
    }
  }
}

@PreviewTest
@CompactFormPreviews
@Composable
fun BackupImportScreenshot() {
  val backup =
    BackupFile(
      uri = Uri.EMPTY,
      name = "isa-eye-backup-2026-10-25-1930.isabackup",
      lastModified = 0L,
      sizeBytes = 45_000L,
    )
  ISAEyeTheme(dynamicColor = false) {
    Surface(modifier = Modifier.fillMaxSize()) {
      BackupRestoreContent(
        state =
          BackupRestoreUiState(
            folderName = "MyBackups/IsaEye",
            tab = BackupTab.Import,
            backups = listOf(backup),
            selectedBackup = backup,
            selectedIsEncrypted = true,
          ),
        onTabChange = {},
        onPassphraseChange = {},
        onImportPassphraseChange = {},
        onChooseFolder = {},
        onExport = {},
        onSelectBackup = {},
        onImport = {},
        onDone = {},
        onClearError = {},
      )
    }
  }
}
