// The navigation tree is a set of small entry builders; splitting it would scatter one screen's
// wiring across files.
@file:Suppress("TooManyFunctions")

package me.dylmye.isa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import me.dylmye.isa.notifications.rememberNotificationPermissionRequest
import me.dylmye.isa.ui.accounts.AccountDetailPlaceholder
import me.dylmye.isa.ui.accounts.AccountDetailScreen
import me.dylmye.isa.ui.accounts.AccountsScreen
import me.dylmye.isa.ui.accounts.AddAccountGateEvent
import me.dylmye.isa.ui.accounts.AddAccountGateViewModel
import me.dylmye.isa.ui.accounts.AddAccountScreen
import me.dylmye.isa.ui.accounts.AutoContributionScreen
import me.dylmye.isa.ui.accounts.EditAccountScreen
import me.dylmye.isa.ui.accounts.UpdateContributionsScreen
import me.dylmye.isa.ui.backup.BackupRestoreDialog
import me.dylmye.isa.ui.components.Tooltip
import me.dylmye.isa.ui.components.TooltipIconButton
import me.dylmye.isa.ui.components.TooltipPosition
import me.dylmye.isa.ui.help.HelpScreen
import me.dylmye.isa.ui.help.HelpTopicPlaceholder
import me.dylmye.isa.ui.help.HelpTopicScreen
import me.dylmye.isa.ui.insights.InsightsScreen
import me.dylmye.isa.ui.insights.SetGoalScreen
import me.dylmye.isa.ui.icons.add as AddIcon
import me.dylmye.isa.ui.icons.chevronBackward as ChevronBackwardIcon
import me.dylmye.isa.ui.icons.chevronForward as ChevronForwardIcon
import me.dylmye.isa.ui.icons.financeMode as InsightsIcon
import me.dylmye.isa.ui.icons.help as HelpIcon
import me.dylmye.isa.ui.icons.savings as SavingsIcon

private data class AppTab(
  val key: NavKey,
  val label: String,
  val icon: ImageVector,
  val testTag: String,
)

private val AppTabs =
  listOf(
    AppTab(Accounts, "Accounts", SavingsIcon, "robo_tab_accounts"),
    AppTab(Insights, "Insights", InsightsIcon, "robo_tab_insights"),
    AppTab(Help, "Help", HelpIcon, "robo_tab_help"),
  )

private val NavigationSuiteType.usesWideRail: Boolean
  get() =
    this == NavigationSuiteType.WideNavigationRailCollapsed ||
      this == NavigationSuiteType.WideNavigationRailExpanded

/** Chooses the tab shown on launch: Help the first time the app opens, Insights after that. */
internal fun launchTab(hasLaunched: Boolean): NavKey = if (hasLaunched) Insights else Help

/**
 * Help runs its own list-detail scaffold, so it takes a scene key of its own. The accounts scaffold
 * uses the default key, which keeps the two from ever pairing an entry with the wrong pane.
 */
private const val HELP_SCENE_KEY = "help"

/** The navigation suite type for the current window, plus the wide rail's toggle state. */
private data class NavigationChrome(
  val suiteType: NavigationSuiteType,
  val canExpandRail: Boolean,
  val railExpanded: Boolean,
  val onToggleRail: () -> Unit,
)

/**
 * Chooses the navigation suite type for the current window. A large window can fit the full
 * navigation panel, but the suite only ever picks the collapsed rail, so pick the expanded type
 * here and let the header's toggle switch between the two.
 */
@Composable
private fun rememberNavigationChrome(windowAdaptiveInfo: WindowAdaptiveInfo): NavigationChrome {
  val canExpandRail =
    windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND,
    )
  var railExpanded by rememberSaveable { mutableStateOf(true) }
  val suiteType =
    remember(windowAdaptiveInfo, railExpanded) {
      if (canExpandRail && railExpanded) {
        NavigationSuiteType.WideNavigationRailExpanded
      } else {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
      }
    }
  return NavigationChrome(
    suiteType = suiteType,
    canExpandRail = canExpandRail,
    railExpanded = railExpanded,
    onToggleRail = { railExpanded = !railExpanded },
  )
}

/**
 * Chooses the pane layout for the current window. The accounts scaffold draws its own dividers, so
 * the stock partition spacer is removed.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun rememberPaneScaffoldDirective(windowAdaptiveInfo: WindowAdaptiveInfo) =
  remember(windowAdaptiveInfo) {
    calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
  }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavigation(
  startTab: NavKey = Accounts,
  addAccountShortcut: Flow<Unit> = emptyFlow(),
  screenRequests: Flow<NavKey> = emptyFlow(),
  backupImportRequests: Flow<Uri> = emptyFlow(),
) {
  val backStack =
    rememberSaveable(saver = TopLevelBackStackSaver) { TopLevelBackStack<NavKey>(startTab) }
  // The add button, the shortcut and the goal card share one guard, so none of them can open a
  // dialog on top of a dialog that is already showing.
  val openAddAccount = { backStack.addDialog(AddAccount) }
  val requestAddAccount = rememberAddAccountRequest(addAccountShortcut, openAddAccount)
  // A tapped reminder opens its screen at the top: a detail page is pushed over the accounts list,
  // and a tab drops any detail page that was left open on it.
  LaunchedEffect(Unit) {
    screenRequests.collect { key ->
      if (key is AccountDetail) {
        backStack.resetTopLevel(Accounts)
        backStack.add(key)
      } else {
        backStack.resetTopLevel(key)
      }
    }
  }
  // A backup opened from the Files app arrives as a dialog over whatever the app is showing.
  OpenBackupImports(backupImportRequests, backStack)

  val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
  val directive = rememberPaneScaffoldDirective(windowAdaptiveInfo)
  // A narrow window shows only the detail page, so re-tapping the tab is the way back to the list.
  // A wider one already shows the list beside the detail, where popping it would be a surprise.
  val singlePane = directive.maxHorizontalPartitions == 1
  val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
  val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
  val navigationChrome = rememberNavigationChrome(windowAdaptiveInfo)
  val wideRail = navigationChrome.suiteType.usesWideRail
  // Hoisted above the nav display: a dialog closes as soon as it saves, but the launcher must stay
  // registered until the system's permission prompt is answered.
  val requestNotifications = rememberNotificationPermissionRequest()

  NavigationSuiteScaffold(
    navigationItems = {
      AppNavigationItems(
        selectedKey = backStack.topLevelKey,
        navigationSuiteType = navigationChrome.suiteType,
        onSelect = { key ->
          if (singlePane) {
            backStack.selectTopLevel(key)
          } else {
            backStack.addTopLevel(key)
          }
        },
      )
    },
    navigationSuiteType = navigationChrome.suiteType,
    primaryActionContent = {
      NavigationHeader(
        chrome = navigationChrome,
        showLogo = wideRail,
        onClick = dropUnlessResumed { requestAddAccount() },
        modifier = if (wideRail) Modifier.padding(horizontal = 20.dp) else Modifier,
      )
    },
  ) {
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
      AppNavDisplay(
        backStack = backStack,
        listDetailStrategy = listDetailStrategy,
        dialogStrategy = dialogStrategy,
        requestNotifications = requestNotifications,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

/**
 * Opens a backup handed over by the system Files app as a dialog over whatever the app is showing.
 */
@Composable
private fun OpenBackupImports(requests: Flow<Uri>, backStack: TopLevelBackStack<NavKey>) {
  LaunchedEffect(Unit) {
    requests.collect { uri -> backStack.addDialog(BackupRestore(uri.toString())) }
  }
}

/** The top-level destinations, rendered with whichever navigation component the suite chose. */
@Composable
private fun AppNavigationItems(
  selectedKey: NavKey,
  navigationSuiteType: NavigationSuiteType,
  onSelect: (NavKey) -> Unit,
) {
  AppTabs.forEach { tab ->
    NavigationSuiteItem(
      icon = { Icon(tab.icon, contentDescription = null) },
      label = { Text(tab.label) },
      selected = tab.key == selectedKey,
      onClick = { onSelect(tab.key) },
      navigationSuiteType = navigationSuiteType,
      modifier = Modifier.testTag(tab.testTag),
    )
  }
}

/** The app's primary action; the navigation suite places it in the rail header on large screens. */
@Composable
private fun AddAccountFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
  // The FAB is icon-only in the rail header, so it carries a description and a hover label.
  Tooltip(text = "Add account", position = TooltipPosition.End) {
    FloatingActionButton(
      onClick = onClick,
      modifier =
        modifier
          .semantics { contentDescription = "Add account" }
          .testTag("robo_add_account"),
    ) {
      Icon(AddIcon, contentDescription = null)
    }
  }
}

/**
 * The navigation header. The wide rail has room for the eye mark above the primary action; the
 * compact bar only shows the action. A large window also gets the rail's expand/collapse toggle,
 * which the stock header would otherwise provide.
 */
@Composable
private fun NavigationHeader(
  chrome: NavigationChrome,
  showLogo: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (showLogo) {
      Icon(
        painter = painterResource(R.drawable.ic_splash_logo),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(56.dp),
      )
    }
    if (chrome.canExpandRail) {
      val toggleLabel =
        if (chrome.railExpanded) "Collapse navigation rail" else "Expand navigation rail"
      TooltipIconButton(
        icon = if (chrome.railExpanded) ChevronBackwardIcon else ChevronForwardIcon,
        contentDescription = toggleLabel,
        onClick = chrome.onToggleRail,
        position = TooltipPosition.End,
      )
    }
    AddAccountFab(onClick = onClick)
  }
}

/**
 * Wires the add button and the keyboard shortcut to the gate ViewModel, and surfaces its one-off
 * events. Kept out of [MainNavigation] so the scaffold itself stays readable.
 */
@Composable
private fun rememberAddAccountRequest(
  addAccountShortcut: Flow<Unit>,
  openAddAccount: () -> Unit,
): () -> Unit {
  val gateViewModel: AddAccountGateViewModel = viewModel(factory = AddAccountGateViewModel.Factory)
  val activity = LocalActivity.current
  val context = LocalContext.current
  LaunchedEffect(gateViewModel, activity) {
    gateViewModel.events.collect { event ->
      when (event) {
        AddAccountGateEvent.OpenAddAccount -> {
          openAddAccount()
        }

        is AddAccountGateEvent.ShowMessage -> {
          Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  val requestAddAccount =
    remember(gateViewModel, activity) {
      { if (activity != null) gateViewModel.onAddClicked(activity) else openAddAccount() }
    }
  LaunchedEffect(addAccountShortcut, requestAddAccount) {
    addAccountShortcut.collect { requestAddAccount() }
  }
  return requestAddAccount
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
  backStack: TopLevelBackStack<NavKey>,
  listDetailStrategy: ListDetailSceneStrategy<NavKey>,
  dialogStrategy: DialogSceneStrategy<NavKey>,
  requestNotifications: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavDisplay(
    backStack = backStack.backStack,
    onBack = { backStack.removeLast() },
    sceneStrategies = listOf(dialogStrategy, listDetailStrategy),
    // Scope ViewModels to their NavEntry so the add-account draft resets each time the flow opens.
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    modifier = modifier,
    entryProvider = entryProvider {
      entry<Accounts>(
        metadata =
          ListDetailSceneStrategy.listPane(detailPlaceholder = { AccountDetailPlaceholder() }),
      ) {
        AccountsScreen(onAccountClick = { productId -> backStack.add(AccountDetail(productId)) })
      }
      entry<AccountDetail>(metadata = ListDetailSceneStrategy.detailPane()) { detail ->
        AccountDetailScreen(
          productId = detail.productId,
          onBack = { backStack.removeLast() },
          onEdit = { backStack.add(EditAccount(detail.productId)) },
          onUpdateContributions = { backStack.add(UpdateContributions(detail.productId)) },
          onSetUpAutoContribution = {
            backStack.addDialog(SetAutoContribution(detail.productId))
          },
        )
      }
      entry<Insights> {
        InsightsScreen(onEditGoal = { backStack.addDialog(SetGoal) })
      }
      entry<Help>(
        metadata =
          ListDetailSceneStrategy.listPane(
            sceneKey = HELP_SCENE_KEY,
            detailPlaceholder = { HelpTopicPlaceholder() },
          ),
      ) {
        HelpScreen(
          onTopicClick = { id -> backStack.add(HelpTopicPage(id)) },
          onBackupClick = { backStack.addDialog(BackupRestore()) },
        )
      }
      entry<HelpTopicPage>(
        metadata = ListDetailSceneStrategy.detailPane(sceneKey = HELP_SCENE_KEY),
      ) { page ->
        HelpTopicScreen(topicId = page.id, onBack = { backStack.removeLast() })
      }
      dialogEntries(backStack, requestNotifications)
    },
  )
}

/** The account flows, each shown as a dialog over whatever is behind it rather than as a page. */
private fun EntryProviderScope<NavKey>.dialogEntries(
  backStack: TopLevelBackStack<NavKey>,
  requestNotifications: () -> Unit,
) {
  entry<AddAccount>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) {
    AddAccountScreen(
      onClose = { backStack.removeLast() },
      onSaved = { backStack.popAndResetTopLevel(Accounts) },
    )
  }
  entry<EditAccount>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) { edit ->
    EditAccountScreen(productId = edit.productId, onClose = { backStack.removeLast() })
  }
  entry<UpdateContributions>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) { update ->
    UpdateContributionsScreen(productId = update.productId, onClose = { backStack.removeLast() })
  }
  entry<SetAutoContribution>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) { auto ->
    AutoContributionScreen(
      productId = auto.productId,
      onClose = { backStack.removeLast() },
      requestNotifications = requestNotifications,
    )
  }
  entry<SetGoal>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) {
    SetGoalScreen(
      onClose = { backStack.removeLast() },
      requestNotifications = requestNotifications,
    )
  }
  entry<BackupRestore>(
    metadata = DialogSceneStrategy.dialog(FormDialogProperties),
  ) { destination ->
    val activity = LocalActivity.current
    BackupRestoreDialog(
      importUri = destination.importUri?.let(Uri::parse),
      onClose = { backStack.removeLast() },
      onImported = {
        if (activity != null) activity.restartForNewDatabase() else backStack.removeLast()
      },
    )
  }
}

/**
 * Opens [key] as a dialog unless one is already showing, so a button tapped twice cannot stack two
 * of them.
 */
private fun TopLevelBackStack<NavKey>.addDialog(key: NavKey) {
  if (backStack.lastOrNull()?.isDialog() != true) add(key)
}

/** Every account flow uses the same full-screen dialog, so the properties live in one place. */
private val FormDialogProperties =
  DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

/**
 * Restarts the app on a fresh activity after an import. The import replaces the database file and
 * the application builds a new container the next time the database is touched, but recreate()
 * keeps the activity's ViewModelStore, so ViewModels still holding collectors on the closed
 * connection would never re-subscribe. A new activity starts with fresh ViewModels, which bind to
 * the reopened database.
 */
private fun Activity.restartForNewDatabase() {
  startActivity(Intent(this, MainActivity::class.java))
  finish()
}

/**
 * True for the keys that [dialogEntries] presents as a dialog. Kept next to it so that adding a
 * dialog there is a reminder to extend this.
 */
private fun NavKey.isDialog(): Boolean = this is AddAccount ||
  this is EditAccount ||
  this is UpdateContributions ||
  this is SetAutoContribution ||
  this is SetGoal ||
  this is BackupRestore
