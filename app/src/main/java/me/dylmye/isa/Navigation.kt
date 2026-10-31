package me.dylmye.isa

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import me.dylmye.isa.ui.accounts.AccountDetailPlaceholder
import me.dylmye.isa.ui.accounts.AccountDetailScreen
import me.dylmye.isa.ui.accounts.AccountsScreen
import me.dylmye.isa.ui.accounts.AddAccountScreen
import me.dylmye.isa.ui.accounts.AutoContributionScreen
import me.dylmye.isa.ui.accounts.EditAccountScreen
import me.dylmye.isa.ui.accounts.UpdateContributionsScreen
import me.dylmye.isa.ui.backup.BackupRestoreDialog
import me.dylmye.isa.ui.help.HelpScreen
import me.dylmye.isa.ui.help.HelpTopicPlaceholder
import me.dylmye.isa.ui.help.HelpTopicScreen
import me.dylmye.isa.ui.insights.InsightsScreen
import me.dylmye.isa.ui.insights.SetGoalScreen
import me.dylmye.isa.ui.icons.add as AddIcon
import me.dylmye.isa.ui.icons.financeMode as InsightsIcon
import me.dylmye.isa.ui.icons.help as HelpIcon
import me.dylmye.isa.ui.icons.savings as SavingsIcon

private data class AppTab(val key: NavKey, val label: String, val icon: ImageVector)

private val AppTabs =
  listOf(
    AppTab(Accounts, "Accounts", SavingsIcon),
    AppTab(Insights, "Insights", InsightsIcon),
    AppTab(Help, "Help", HelpIcon),
  )

/** Chooses the tab shown on launch: Help the first time the app opens, Insights after that. */
internal fun launchTab(hasLaunched: Boolean): NavKey = if (hasLaunched) Insights else Help

/**
 * Help runs its own list-detail scaffold, so it takes a scene key of its own. The accounts scaffold
 * uses the default key, which keeps the two from ever pairing an entry with the wrong pane.
 */
private const val HELP_SCENE_KEY = "help"

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavigation(
  startTab: NavKey = Accounts,
  addAccountShortcut: Flow<Unit> = emptyFlow(),
  screenRequests: Flow<NavKey> = emptyFlow(),
) {
  val backStack =
    rememberSaveable(saver = TopLevelBackStackSaver) { TopLevelBackStack<NavKey>(startTab) }
  // The add button, the shortcut and the goal card share one guard, so none of them can open a
  // dialog on top of a dialog that is already showing.
  val openAddAccount = { backStack.addDialog(AddAccount) }
  LaunchedEffect(Unit) { addAccountShortcut.collect { openAddAccount() } }
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

  val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
  val directive = remember(windowAdaptiveInfo) {
    calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
  }
  // A narrow window shows only the detail page, so re-tapping the tab is the way back to the list.
  // A wider one already shows the list beside the detail, where popping it would be a surprise.
  val singlePane = directive.maxHorizontalPartitions == 1
  val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
  val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
  // Material 3's preferred navigation components: a short navigation bar on compact windows and a
  // wide navigation rail on larger ones. The primary action rides in the rail's header.
  val navigationSuiteType = remember(windowAdaptiveInfo) {
    NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
  }
  // A wide rail is 96dp, so a 56dp FAB needs 20dp either side to sit under the nav icons.
  val wideRail =
    navigationSuiteType == NavigationSuiteType.WideNavigationRailCollapsed ||
      navigationSuiteType == NavigationSuiteType.WideNavigationRailExpanded

  NavigationSuiteScaffold(
    navigationItems = {
      AppNavigationItems(
        selectedKey = backStack.topLevelKey,
        navigationSuiteType = navigationSuiteType,
        onSelect = { key ->
          if (singlePane) {
            backStack.selectTopLevel(key)
          } else {
            backStack.addTopLevel(key)
          }
        },
      )
    },
    navigationSuiteType = navigationSuiteType,
    primaryActionContent = {
      AddAccountFab(
        onClick = dropUnlessResumed { openAddAccount() },
        modifier = if (wideRail) Modifier.padding(horizontal = 20.dp) else Modifier,
      )
    },
  ) {
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
      AppNavDisplay(
        backStack = backStack,
        listDetailStrategy = listDetailStrategy,
        dialogStrategy = dialogStrategy,
        modifier = Modifier.padding(innerPadding),
      )
    }
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
    )
  }
}

/** The app's primary action; the navigation suite places it in the rail header on large screens. */
@Composable
private fun AddAccountFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
  FloatingActionButton(
    onClick = onClick,
    // The FAB is icon-only in the rail header, so it carries its own description.
    modifier = modifier.semantics { contentDescription = "Add account" },
  ) {
    Icon(AddIcon, contentDescription = null)
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
  backStack: TopLevelBackStack<NavKey>,
  listDetailStrategy: ListDetailSceneStrategy<NavKey>,
  dialogStrategy: DialogSceneStrategy<NavKey>,
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
          onBackupClick = { backStack.addDialog(BackupRestore) },
        )
      }
      entry<HelpTopicPage>(
        metadata = ListDetailSceneStrategy.detailPane(sceneKey = HELP_SCENE_KEY),
      ) { page ->
        HelpTopicScreen(topicId = page.id, onBack = { backStack.removeLast() })
      }
      dialogEntries(backStack)
    },
  )
}

/** The account flows, each shown as a dialog over whatever is behind it rather than as a page. */
private fun EntryProviderScope<NavKey>.dialogEntries(backStack: TopLevelBackStack<NavKey>) {
  entry<AddAccount>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) {
    AddAccountScreen(onClose = { backStack.removeLast() })
  }
  entry<EditAccount>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) { edit ->
    EditAccountScreen(productId = edit.productId, onClose = { backStack.removeLast() })
  }
  entry<UpdateContributions>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) { update ->
    UpdateContributionsScreen(productId = update.productId, onClose = { backStack.removeLast() })
  }
  entry<SetAutoContribution>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) { auto ->
    AutoContributionScreen(productId = auto.productId, onClose = { backStack.removeLast() })
  }
  entry<SetGoal>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) {
    SetGoalScreen(onClose = { backStack.removeLast() })
  }
  entry<BackupRestore>(
    metadata =
      DialogSceneStrategy.dialog(
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
      ),
  ) {
    val activity = LocalActivity.current
    BackupRestoreDialog(
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
