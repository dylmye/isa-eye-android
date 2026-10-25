package me.dylmye.isa.ui.backup

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.window.core.layout.WindowSizeClass
import me.dylmye.isa.data.backup.BackupError
import me.dylmye.isa.data.backup.BackupFile
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.accounts.FormDialogScaffold
import java.text.DateFormat
import java.util.Date

// The dialog coordinates two flows and two confirmations; splitting hides the order.
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun BackupRestoreDialog(
  onClose: () -> Unit,
  onImported: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: BackupRestoreViewModel = viewModel(factory = BackupRestoreViewModel.Factory)
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val close = {
    val reload = state.imported || state.needsReload
    viewModel.resetForNextOpen()
    if (reload) onImported() else onClose()
  }
  val folderPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      uri?.let(viewModel::onFolderChosen)
    }

  val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
  NavigationBackHandler(
    state = navigationState,
    isBackEnabled = state.isBusy || state.imported || state.needsReload,
    onBackCompleted = {
      if (!state.isBusy) close()
    },
  )

  if (state.showExportWithoutPassphraseWarning) {
    AlertDialog(
      onDismissRequest = viewModel::dismissExportWarning,
      title = { Text("Export without a passphrase?") },
      text = {
        Text(
          "The backup file won't be encrypted. Anyone who gets the file can read your accounts " +
            "and balances.",
        )
      },
      confirmButton = {
        TextButton(onClick = viewModel::confirmExportWithoutPassphrase) {
          Text("Export unencrypted")
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissExportWarning) { Text("Cancel") }
      },
    )
  }

  if (state.showImportConfirmation) {
    AlertDialog(
      onDismissRequest = viewModel::dismissImportConfirmation,
      title = { Text("Replace all data?") },
      text = {
        Text(
          "This will replace all accounts and balances on this device with the backup " +
            "${state.selectedBackup?.name ?: ""}. This can't be undone.",
        )
      },
      confirmButton = {
        TextButton(onClick = viewModel::confirmImport) { Text("Import backup") }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissImportConfirmation) { Text("Cancel") }
      },
    )
  }

  val isCompact =
    !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )

  FormDialogScaffold(
    title = "Backup & restore",
    onClose = { if (!state.isBusy) close() },
    isCompact = isCompact,
    modifier = modifier,
  ) { innerPadding ->
    BackupRestoreContent(
      state = state,
      onTabChange = viewModel::onTabChange,
      onPassphraseChange = viewModel::onPassphraseChange,
      onImportPassphraseChange = viewModel::onImportPassphraseChange,
      onChooseFolder = { folderPicker.launch(null) },
      onExport = viewModel::onExportRequested,
      onSelectBackup = viewModel::onSelectBackup,
      onImport = viewModel::onImportRequested,
      onDone = close,
      onClearError = viewModel::clearError,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Suppress("LongMethod") // Keeping the dialog's state branches together makes the flow readable.
@Composable
fun BackupRestoreContent(
  state: BackupRestoreUiState,
  onTabChange: (BackupTab) -> Unit,
  onPassphraseChange: (String) -> Unit,
  onImportPassphraseChange: (String) -> Unit,
  onChooseFolder: () -> Unit,
  onExport: () -> Unit,
  onSelectBackup: (BackupFile) -> Unit,
  onImport: () -> Unit,
  onDone: () -> Unit,
  onClearError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    FolderRow(folderName = state.folderName, onChange = onChooseFolder)

    PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
      Tab(
        selected = state.tab == BackupTab.Export,
        onClick = { onTabChange(BackupTab.Export) },
        text = { Text("Export") },
      )
      Tab(
        selected = state.tab == BackupTab.Import,
        onClick = { onTabChange(BackupTab.Import) },
        text = { Text("Import") },
      )
    }

    state.error?.let { error ->
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = error.message(),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onClearError) { Text("Dismiss") }
        }
      }
    }

    if (state.isBusy) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    when (state.tab) {
      BackupTab.Export -> {
        ExportContent(state, onPassphraseChange, onExport, onDone)
      }

      BackupTab.Import -> {
        ImportContent(
          state = state,
          onChooseFolder = onChooseFolder,
          onSelectBackup = onSelectBackup,
          onImportPassphraseChange = onImportPassphraseChange,
          onImport = onImport,
          onDone = onDone,
        )
      }
    }
  }
}

@Composable
private fun FolderRow(folderName: String?, onChange: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = if (folderName == null) "No backup folder" else "Backup folder: $folderName",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          if (folderName == null) {
            "Choose a folder for ISA Eye backups. ISA Eye keeps its files in an IsaEye folder " +
              "inside it."
          } else {
            "ISA Eye keeps its backups in the IsaEye folder here."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextButton(onClick = onChange, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
        Text(if (folderName == null) "Choose folder" else "Change folder")
      }
    }
  }
}

@Composable
private fun ExportContent(
  state: BackupRestoreUiState,
  onPassphraseChange: (String) -> Unit,
  onExport: () -> Unit,
  onDone: () -> Unit,
) {
  if (state.exportedFileName != null) {
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Backup saved", style = MaterialTheme.typography.titleMedium)
        Text(state.exportedFileName, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onDone, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
          Text("Done")
        }
      }
    }
    return
  }

  Text(
    "Create a file you can copy to another device or keep somewhere safe.",
    style = MaterialTheme.typography.bodyMedium,
  )
  PassphraseField(
    value = state.passphrase,
    onValueChange = onPassphraseChange,
    label = "Passphrase (optional)",
  )
  Text(
    "Leave empty for an unencrypted file. Anyone who has the file can read your accounts and " +
      "balances.",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Button(
    onClick = onExport,
    enabled = state.folderAvailable && !state.isBusy,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Export backup")
  }
}

@Suppress("LongMethod", "ReturnCount") // Each early return is a distinct dialog state.
@Composable
private fun ImportContent(
  state: BackupRestoreUiState,
  onChooseFolder: () -> Unit,
  onSelectBackup: (BackupFile) -> Unit,
  onImportPassphraseChange: (String) -> Unit,
  onImport: () -> Unit,
  onDone: () -> Unit,
) {
  if (state.imported) {
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Data restored", style = MaterialTheme.typography.titleMedium)
        Text(
          "Your accounts and balances have been replaced with the backup.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onDone, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
          Text("Done")
        }
      }
    }
    return
  }

  if (!state.folderAvailable) {
    Text(
      "Choose a backup folder before importing.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
      Text("Choose folder")
    }
    return
  }

  if (state.backups.isEmpty()) {
    Text(
      "No backups in ${state.folderName ?: "this folder"}.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    state.backups.forEach { backup ->
      BackupRow(
        backup = backup,
        selected = state.selectedBackup?.uri == backup.uri,
        onClick = { onSelectBackup(backup) },
      )
    }
  }

  if (state.isCheckingEncryption) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }

  if (state.selectedBackup != null && state.selectedIsEncrypted) {
    PassphraseField(
      value = state.importPassphrase,
      onValueChange = onImportPassphraseChange,
      label = "Passphrase",
    )
    Text(
      "This backup is encrypted. Enter the passphrase used when it was exported.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  Button(
    onClick = onImport,
    enabled =
      state.selectedBackup != null &&
        !state.isBusy &&
        !state.isCheckingEncryption &&
        (!state.selectedIsEncrypted || state.importPassphrase.isNotEmpty()),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Import backup")
  }
}

@Composable
private fun BackupRow(backup: BackupFile, selected: Boolean, onClick: () -> Unit) {
  val context = LocalContext.current
  val date =
    remember(backup.lastModified) {
      if (backup.lastModified > 0) {
        DateFormat.getDateTimeInstance(
          DateFormat.MEDIUM,
          DateFormat.SHORT,
        ).format(Date(backup.lastModified))
      } else {
        "Unknown date"
      }
    }
  Card(
    onClick = onClick,
    colors =
      if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
      } else {
        CardDefaults.cardColors()
      },
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(backup.name, style = MaterialTheme.typography.titleSmall)
      Text(
        "$date · ${Formatter.formatShortFileSize(context, backup.sizeBytes)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PassphraseField(value: String, onValueChange: (String) -> Unit, label: String) {
  var visible by remember { mutableStateOf(false) }
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    visualTransformation =
      if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    trailingIcon = {
      TextButton(onClick = { visible = !visible }) {
        Text(if (visible) "Hide" else "Show")
      }
    },
    modifier = Modifier.fillMaxWidth(),
  )
}

private fun BackupError.message(): String = when (this) {
  BackupError.NoFolder -> {
    "Choose a backup folder first."
  }

  BackupError.FolderUnavailable -> {
    "That folder isn't available. Choose it again."
  }

  BackupError.InvalidFile -> {
    "That isn't a valid ISA Eye backup."
  }

  BackupError.WrongPassphrase -> {
    "That passphrase didn't work, or the file is damaged."
  }

  BackupError.EncryptedBackup -> {
    "This backup is encrypted. Enter its passphrase."
  }

  BackupError.UnsupportedVersion -> {
    "This backup was made by a different version of ISA Eye."
  }

  BackupError.ExportFailed -> {
    "The backup couldn't be saved."
  }

  BackupError.ImportFailed -> {
    "The backup couldn't be restored. Your current data is unchanged."
  }

  BackupError.Io -> {
    "The file couldn't be read."
  }
}

@FormFactorPreviews
@Composable
private fun BackupRestoreExportPreview() {
  ISAEyeTheme {
    BackupRestoreContent(
      state =
        BackupRestoreUiState(
          folderName = "Backups",
          passphrase = "",
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

@FormFactorPreviews
@Composable
private fun BackupRestoreImportPreview() {
  ISAEyeTheme {
    BackupRestoreContent(
      state =
        BackupRestoreUiState(
          folderName = "Backups",
          tab = BackupTab.Import,
          backups =
            listOf(
              BackupFile(
                uri = android.net.Uri.EMPTY,
                name = "isa-eye-backup-2026-10-25-1930.isabackup",
                lastModified = 0L,
                sizeBytes = 45_000L,
              ),
            ),
          selectedBackup =
            BackupFile(
              uri = android.net.Uri.EMPTY,
              name = "isa-eye-backup-2026-10-25-1930.isabackup",
              lastModified = 0L,
              sizeBytes = 45_000L,
            ),
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
