package me.dylmye.isa

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import me.dylmye.isa.ui.accounts.AccountDetailPlaceholder
import me.dylmye.isa.ui.accounts.AccountDetailScreen
import me.dylmye.isa.ui.accounts.AccountsScreen
import me.dylmye.isa.ui.accounts.AddAccountScreen
import me.dylmye.isa.ui.help.HelpScreen
import me.dylmye.isa.ui.insights.InsightsScreen
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavigation() {
  val backStack = remember { TopLevelBackStack<NavKey>(Accounts) }

  val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
  val directive = remember(windowAdaptiveInfo) {
    calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
  }
  val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
  val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      AppTabs.forEach { tab ->
        item(
          icon = { Icon(tab.icon, contentDescription = null) },
          label = { Text(tab.label) },
          selected = tab.key == backStack.topLevelKey,
          onClick = { backStack.addTopLevel(tab.key) },
        )
      }
    },
  ) {
    Scaffold(
      contentWindowInsets = WindowInsets(0.dp),
      floatingActionButton = {
        ExtendedFloatingActionButton(
          onClick = dropUnlessResumed { backStack.add(AddAccount) },
          icon = { Icon(AddIcon, contentDescription = null) },
          text = { Text("Add account") },
        )
      },
    ) { innerPadding ->
      AppNavDisplay(
        backStack = backStack,
        listDetailStrategy = listDetailStrategy,
        dialogStrategy = dialogStrategy,
        modifier = Modifier.padding(innerPadding),
      )
    }
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
        AccountsScreen(onAccountClick = { name -> backStack.add(AccountDetail(name)) })
      }
      entry<AccountDetail>(metadata = ListDetailSceneStrategy.detailPane()) { detail ->
        AccountDetailScreen(name = detail.name)
      }
      entry<Insights> { InsightsScreen() }
      entry<Help> { HelpScreen() }
      entry<AddAccount>(
        metadata =
          DialogSceneStrategy.dialog(
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
          ),
      ) {
        AddAccountScreen(onClose = { backStack.removeLast() })
      }
    },
  )
}
