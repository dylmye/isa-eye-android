package me.dylmye.isa.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.backup.BackupError
import me.dylmye.isa.data.backup.BackupException
import me.dylmye.isa.data.backup.BackupFile
import me.dylmye.isa.data.backup.BackupManager

enum class BackupTab {
  Export,
  Import,
}

data class BackupRestoreUiState(
  val folderName: String? = null,
  val tab: BackupTab = BackupTab.Export,
  val passphrase: String = "",
  val backups: List<BackupFile> = emptyList(),
  val selectedBackup: BackupFile? = null,
  val selectedIsEncrypted: Boolean = false,
  val isCheckingEncryption: Boolean = false,
  val importPassphrase: String = "",
  val isBusy: Boolean = false,
  val error: BackupError? = null,
  val exportedFileName: String? = null,
  val imported: Boolean = false,
  val needsReload: Boolean = false,
  val showExportWithoutPassphraseWarning: Boolean = false,
  val showImportConfirmation: Boolean = false,
) {
  val folderAvailable: Boolean
    get() = folderName != null
}

@Suppress("TooManyFunctions") // Small, explicit actions keep the dialog's state machine testable.
internal class BackupRestoreViewModel(
  application: Application,
  private val manager: BackupManager,
) : AndroidViewModel(application) {
  private val _uiState = MutableStateFlow(BackupRestoreUiState())
  val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

  init {
    refreshFolder()
  }

  fun onFolderChosen(uri: Uri) {
    viewModelScope.launch {
      try {
        manager.saveFolder(uri)
        refreshFolder()
      } catch (error: BackupException) {
        fail(error)
      }
    }
  }

  fun onTabChange(tab: BackupTab) {
    _uiState.update { it.copy(tab = tab, error = null) }
    if (tab == BackupTab.Import) refreshFolder()
  }

  fun onPassphraseChange(value: String) {
    _uiState.update { it.copy(passphrase = value) }
  }

  fun onImportPassphraseChange(value: String) {
    _uiState.update { it.copy(importPassphrase = value) }
  }

  fun onExportRequested() {
    if (_uiState.value.passphrase.isEmpty()) {
      _uiState.update { it.copy(showExportWithoutPassphraseWarning = true) }
    } else {
      export()
    }
  }

  fun dismissExportWarning() {
    _uiState.update { it.copy(showExportWithoutPassphraseWarning = false) }
  }

  fun confirmExportWithoutPassphrase() {
    _uiState.update { it.copy(showExportWithoutPassphraseWarning = false) }
    export()
  }

  fun onSelectBackup(file: BackupFile) {
    _uiState.update {
      it.copy(
        selectedBackup = file,
        selectedIsEncrypted = false,
        isCheckingEncryption = true,
        importPassphrase = "",
        error = null,
      )
    }
    viewModelScope.launch {
      try {
        val encrypted = manager.isEncrypted(file.uri)
        _uiState.update { state ->
          if (state.selectedBackup?.uri == file.uri) {
            state.copy(selectedIsEncrypted = encrypted, isCheckingEncryption = false)
          } else {
            state
          }
        }
      } catch (error: BackupException) {
        fail(error)
      }
    }
  }

  fun onImportRequested() {
    val state = _uiState.value
    if (state.selectedBackup == null) return
    if (state.selectedIsEncrypted && state.importPassphrase.isEmpty()) {
      _uiState.update { it.copy(error = BackupError.EncryptedBackup) }
      return
    }
    _uiState.update { it.copy(showImportConfirmation = true, error = null) }
  }

  fun dismissImportConfirmation() {
    _uiState.update { it.copy(showImportConfirmation = false) }
  }

  fun confirmImport() {
    val state = _uiState.value
    val backup = state.selectedBackup ?: return
    _uiState.update { it.copy(showImportConfirmation = false) }
    import(backup, state.importPassphrase.ifEmpty { null })
  }

  fun clearError() {
    _uiState.update { it.copy(error = null) }
  }

  /**
   * The dialog's ViewModel can outlive a single presentation (and an Activity recreation after an
   * import). Clear the transient result state so the next open starts at the top.
   */
  fun resetForNextOpen() {
    _uiState.update { BackupRestoreUiState(folderName = it.folderName) }
    refreshFolder()
  }

  private fun export() {
    val state = _uiState.value
    if (state.isBusy) return
    _uiState.update { it.copy(isBusy = true, error = null, exportedFileName = null) }
    viewModelScope.launch {
      try {
        val file = manager.export(state.passphrase.ifEmpty { null })
        val backups = withContext(Dispatchers.IO) { manager.listBackups() }
        _uiState.update {
          it.copy(
            isBusy = false,
            passphrase = "",
            exportedFileName = file.name,
            backups = backups,
          )
        }
      } catch (error: BackupException) {
        fail(error)
      } catch (_: Exception) {
        _uiState.update {
          it.copy(isBusy = false, error = BackupError.ExportFailed)
        }
      }
    }
  }

  private fun import(backup: BackupFile, passphrase: String?) {
    if (_uiState.value.isBusy) return
    _uiState.update { it.copy(isBusy = true, error = null, imported = false) }
    viewModelScope.launch {
      try {
        manager.import(backup.uri, passphrase)
        _uiState.update {
          it.copy(
            isBusy = false,
            imported = true,
            needsReload = true,
            importPassphrase = "",
          )
        }
      } catch (error: BackupException) {
        fail(error)
        if (error.reloadRequired) {
          _uiState.update { it.copy(needsReload = true) }
        }
      } catch (_: Exception) {
        _uiState.update {
          it.copy(isBusy = false, error = BackupError.ImportFailed)
        }
      }
    }
  }

  private fun refreshFolder() {
    viewModelScope.launch {
      val name = withContext(Dispatchers.IO) { runCatching { manager.selectedFolderName() } }
      val backups =
        if (name.getOrNull() != null) {
          withContext(Dispatchers.IO) { runCatching { manager.listBackups() } }
            .getOrDefault(emptyList())
        } else {
          emptyList()
        }
      _uiState.update {
        it.copy(
          folderName = name.getOrNull(),
          backups = backups,
          selectedBackup = null,
          selectedIsEncrypted = false,
          isCheckingEncryption = false,
          importPassphrase = "",
        )
      }
    }
  }

  private fun fail(error: BackupException) {
    _uiState.update {
      it.copy(
        isBusy = false,
        isCheckingEncryption = false,
        error = error.error,
        needsReload = it.needsReload || error.reloadRequired,
      )
    }
  }

  companion object {
    val Factory = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        BackupRestoreViewModel(
          application,
          BackupManager(application, application, application.userPreferences),
        )
      }
    }
  }
}
