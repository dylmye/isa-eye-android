package me.dylmye.isa.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
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
  val selectedIsExternal: Boolean = false,
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

  /** A selected backup that came from outside the backup folder, such as the Files app. */
  val externalBackup: BackupFile?
    get() = selectedBackup?.takeIf { selected -> backups.none { it.uri == selected.uri } }

  val canImport: Boolean
    get() = selectedBackup != null &&
      !isBusy &&
      !isCheckingEncryption &&
      (!selectedIsEncrypted || importPassphrase.isNotEmpty())
}

@Suppress("TooManyFunctions") // Small, explicit actions keep the dialog's state machine testable.
internal class BackupRestoreViewModel(
  application: Application,
  private val manager: BackupManager,
  private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
  private val _uiState = MutableStateFlow(savedStateHandle.backupRestoreState())
  val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

  /** The last file opened from outside the app, so a recomposition does not restart its import. */
  private var externalUri: String? = null

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
    updateState { it.copy(tab = tab, error = null) }
    if (tab == BackupTab.Import) refreshFolder()
  }

  fun onPassphraseChange(value: String) {
    updateState { it.copy(passphrase = value) }
  }

  fun onImportPassphraseChange(value: String) {
    updateState { it.copy(importPassphrase = value) }
  }

  fun onExportRequested() {
    if (_uiState.value.passphrase.isEmpty()) {
      updateState { it.copy(showExportWithoutPassphraseWarning = true) }
    } else {
      export()
    }
  }

  fun dismissExportWarning() {
    updateState { it.copy(showExportWithoutPassphraseWarning = false) }
  }

  fun confirmExportWithoutPassphrase() {
    updateState { it.copy(showExportWithoutPassphraseWarning = false) }
    export()
  }

  fun onSelectBackup(file: BackupFile) {
    select(file, external = false, confirmWhenReady = false)
  }

  /**
   * Opens a backup handed over by the system Files app. It is not in the chosen backup folder, so
   * it is tracked as an external selection, and an unencrypted one goes straight to the replace
   * confirmation the user already asked for by opening the file.
   */
  fun onExternalBackup(uri: Uri) {
    if (externalUri == uri.toString()) return
    externalUri = uri.toString()
    viewModelScope.launch {
      val file = withContext(Dispatchers.IO) { manager.describe(uri) }
      select(file, external = true, confirmWhenReady = true)
    }
  }

  private fun select(file: BackupFile, external: Boolean, confirmWhenReady: Boolean) {
    updateState {
      it.copy(
        tab = BackupTab.Import,
        selectedBackup = file,
        selectedIsEncrypted = false,
        selectedIsExternal = external,
        isCheckingEncryption = true,
        importPassphrase = "",
        error = null,
        showImportConfirmation = false,
      )
    }
    viewModelScope.launch {
      try {
        val encrypted = manager.isEncrypted(file.uri)
        updateState { state ->
          if (state.selectedBackup?.uri == file.uri) {
            state.copy(
              selectedIsEncrypted = encrypted,
              isCheckingEncryption = false,
              showImportConfirmation = confirmWhenReady && !encrypted,
            )
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
      updateState { it.copy(error = BackupError.EncryptedBackup) }
      return
    }
    updateState { it.copy(showImportConfirmation = true, error = null) }
  }

  fun dismissImportConfirmation() {
    updateState { it.copy(showImportConfirmation = false) }
  }

  fun confirmImport() {
    val state = _uiState.value
    val backup = state.selectedBackup ?: return
    updateState { it.copy(showImportConfirmation = false) }
    import(backup, state.importPassphrase.ifEmpty { null })
  }

  fun clearError() {
    updateState { it.copy(error = null) }
  }

  /**
   * The dialog's ViewModel can outlive a single presentation (and an Activity recreation after an
   * import). Clear the transient result state so the next open starts at the top.
   */
  fun resetForNextOpen() {
    externalUri = null
    updateState { BackupRestoreUiState(folderName = it.folderName) }
    refreshFolder()
  }

  private fun export() {
    val state = _uiState.value
    if (state.isBusy) return
    updateState { it.copy(isBusy = true, error = null, exportedFileName = null) }
    viewModelScope.launch {
      try {
        val file = manager.export(state.passphrase.ifEmpty { null })
        val backups = withContext(Dispatchers.IO) { manager.listBackups() }
        updateState {
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
        updateState {
          it.copy(isBusy = false, error = BackupError.ExportFailed)
        }
      }
    }
  }

  private fun import(backup: BackupFile, passphrase: String?) {
    if (_uiState.value.isBusy) return
    updateState { it.copy(isBusy = true, error = null, imported = false) }
    viewModelScope.launch {
      try {
        manager.import(backup.uri, passphrase)
        updateState {
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
          updateState { it.copy(needsReload = true) }
        }
      } catch (_: Exception) {
        updateState {
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
      updateState {
        it.copy(
          folderName = name.getOrNull(),
          backups = backups,
          // A file opened from the Files app is not in this folder, so a folder refresh must not
          // discard it.
          selectedBackup = if (it.selectedIsExternal) it.selectedBackup else null,
          selectedIsEncrypted = if (it.selectedIsExternal) it.selectedIsEncrypted else false,
          isCheckingEncryption = if (it.selectedIsExternal) it.isCheckingEncryption else false,
          importPassphrase = if (it.selectedIsExternal) it.importPassphrase else "",
        )
      }
    }
  }

  private fun fail(error: BackupException) {
    updateState {
      it.copy(
        isBusy = false,
        isCheckingEncryption = false,
        error = error.error,
        needsReload = it.needsReload || error.reloadRequired,
      )
    }
  }

  /** Applies a state change and keeps the fields a process death should restore in the handle. */
  private fun updateState(transform: (BackupRestoreUiState) -> BackupRestoreUiState) {
    _uiState.update { current ->
      transform(current).also { savedStateHandle.writeBackupRestoreState(it) }
    }
  }

  companion object {
    val Factory = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        BackupRestoreViewModel(
          application,
          BackupManager(application, application, application.userPreferences),
          createSavedStateHandle(),
        )
      }
    }
  }
}

private const val STATE_TAB = "backupRestore.tab"
private const val STATE_PASSPHRASE = "backupRestore.passphrase"
private const val STATE_IMPORT_PASSPHRASE = "backupRestore.importPassphrase"

/** The user's inputs, restored after a process death while the backup dialog was open. */
private fun SavedStateHandle.backupRestoreState(): BackupRestoreUiState {
  val tabName: String? = this[STATE_TAB]
  val tab = tabName?.let { name -> BackupTab.entries.firstOrNull { it.name == name } }
  return BackupRestoreUiState(
    tab = tab ?: BackupTab.Export,
    passphrase = this[STATE_PASSPHRASE] ?: "",
    importPassphrase = this[STATE_IMPORT_PASSPHRASE] ?: "",
  )
}

private fun SavedStateHandle.writeBackupRestoreState(state: BackupRestoreUiState) {
  this[STATE_TAB] = state.tab.name
  this[STATE_PASSPHRASE] = state.passphrase
  this[STATE_IMPORT_PASSPHRASE] = state.importPassphrase
}
