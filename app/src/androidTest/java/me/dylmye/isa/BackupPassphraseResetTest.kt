package me.dylmye.isa

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import me.dylmye.isa.data.backup.BackupFile
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.backup.BackupRestoreContent
import me.dylmye.isa.ui.backup.BackupRestoreUiState
import me.dylmye.isa.ui.backup.BackupTab
import org.junit.Rule
import org.junit.Test

/**
 * Guards the import passphrase against going stale: selecting another encrypted backup clears the
 * passphrase in the ViewModel while the field stays composed, so the field has to follow.
 */
class BackupPassphraseResetTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun importPassphrase_clearsWhenAnotherBackupResetsIt() {
    val first = BackupFile(Uri.EMPTY, "first.isabackup", lastModified = 1L, sizeBytes = 10L)
    val second = BackupFile(Uri.EMPTY, "second.isabackup", lastModified = 2L, sizeBytes = 20L)
    var state by mutableStateOf(importState(first, passphrase = ""))

    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        BackupRestoreContent(
          state = state,
          onTabChange = {},
          onPassphraseChange = {},
          onImportPassphraseChange = { state = state.copy(importPassphrase = it) },
          onChooseFolder = {},
          onExport = {},
          onSelectBackup = {},
          onImport = {},
          onDone = {},
          onClearError = {},
        )
      }
    }

    val field = composeTestRule.onNode(hasSetTextAction())
    field.requestFocus().performTextInput("hunter2")
    field.assert(hasEditableText("hunter2"))

    // What BackupRestoreViewModel.select does: a new encrypted backup clears the passphrase while
    // the same passphrase field stays on screen.
    composeTestRule.runOnIdle {
      state = importState(second, passphrase = "")
    }

    field.assert(hasEditableText(""))
  }

  private fun importState(selected: BackupFile, passphrase: String) = BackupRestoreUiState(
    folderName = "Backups",
    tab = BackupTab.Import,
    backups = listOf(selected),
    selectedBackup = selected,
    selectedIsEncrypted = true,
    importPassphrase = passphrase,
  )

  private fun hasEditableText(text: String) =
    SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
}
