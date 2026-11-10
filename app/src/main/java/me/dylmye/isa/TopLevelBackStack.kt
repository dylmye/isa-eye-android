package me.dylmye.isa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class TopLevelBackStack<T : NavKey>(startKey: T) {
  private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> =
    linkedMapOf(startKey to mutableStateListOf(startKey))

  var topLevelKey by mutableStateOf(startKey)
    private set

  val backStack = mutableStateListOf(startKey)

  private fun updateBackStack() = backStack.apply {
    clear()
    addAll(topLevelStacks.flatMap { it.value })
  }

  fun addTopLevel(key: T) {
    if (topLevelStacks[key] == null) {
      topLevelStacks[key] = mutableStateListOf(key)
    } else {
      topLevelStacks.apply { remove(key)?.let { put(key, it) } }
    }
    select(key)
  }

  /**
   * A tab press. Another tab switches to it with whatever it had open; the tab already showing pops
   * back to its root, so a page stacked over a list is one tap from the list again. A tab that is
   * already at its root is left alone, so its scroll position survives.
   */
  fun selectTopLevel(key: T) {
    when {
      key != topLevelKey -> addTopLevel(key)
      topLevelStacks[key].orEmpty().size > 1 -> resetTopLevel(key)
    }
  }

  /**
   * Opens [key]'s tab at its own root, dropping anything that was stacked above it. The key moves to
   * the end of the map, because that order is what [updateBackStack] flattens: a tab that kept its
   * old position would leave another tab's page showing while the nav bar highlighted this one.
   */
  fun resetTopLevel(key: T) {
    topLevelStacks.remove(key)
    topLevelStacks[key] = mutableStateListOf(key)
    select(key)
  }

  private fun select(key: T) {
    topLevelKey = key
    updateBackStack()
  }

  fun add(key: T) {
    topLevelStacks[topLevelKey]?.add(key)
    updateBackStack()
  }

  /**
   * Pops the page over the current tab, or closes the tab when only its root is left. The last tab
   * is never popped: with nowhere to go back to, a system back gesture would empty the navigation.
   */
  fun removeLast() {
    val stack = topLevelStacks[topLevelKey] ?: return
    when {
      stack.size > 1 -> {
        stack.removeAt(stack.lastIndex)
      }

      topLevelStacks.size > 1 -> {
        topLevelStacks.remove(topLevelKey)
        topLevelKey = topLevelStacks.keys.last()
      }

      else -> {
        return
      }
    }
    updateBackStack()
  }

  /**
   * Closes the page stacked over the current tab (usually a dialog) and shows [key]'s root, so a
   * flow that can start from any tab lands where its result is visible.
   */
  fun popAndResetTopLevel(key: T) {
    removeLast()
    resetTopLevel(key)
  }

  /**
   * A saveable snapshot of the per-tab stacks and the selected tab. Used by
   * [TopLevelBackStackSaver] so a configuration change or process death does not reset navigation.
   */
  internal fun saveState(): String? = runCatching {
    NavigationStateJson.encodeToString(
      SavedNavigationState(
        topLevelKey = topLevelKey,
        stacks = topLevelStacks.values.map { it.toList() },
      ),
    )
  }
    .getOrNull()

  private constructor(startKey: T, stacks: List<List<T>>) : this(startKey) {
    require(stacks.isNotEmpty() && stacks.all { it.isNotEmpty() })
    topLevelStacks =
      linkedMapOf<T, SnapshotStateList<T>>().apply {
        stacks.forEach { stack -> put(stack.first(), stack.toMutableStateList()) }
      }
    topLevelKey = startKey
    updateBackStack()
  }

  internal companion object {
    /** Rebuilds a stack from [saveState], or null when the snapshot is malformed. */
    fun restore(state: String): TopLevelBackStack<NavKey>? =
      runCatching { NavigationStateJson.decodeFromString<SavedNavigationState>(state) }
        .getOrNull()
        ?.takeIf { saved ->
          saved.stacks.isNotEmpty() &&
            saved.stacks.all { stack -> stack.isNotEmpty() } &&
            saved.stacks.any { stack -> stack.first() == saved.topLevelKey }
        }
        ?.let { saved -> TopLevelBackStack(saved.topLevelKey, saved.stacks) }
  }
}

/**
 * The navigation snapshot. Every key is already [Serializable], so kotlinx.serialization stores the
 * polymorphic [NavKey] hierarchy rather than a hand-written route format that would need updating
 * for each new destination.
 */
@Serializable
private data class SavedNavigationState(val topLevelKey: NavKey, val stacks: List<List<NavKey>>)

private val NavigationStateJson = Json {
  serializersModule =
    SerializersModule {
      polymorphic(NavKey::class) {
        subclass(Accounts::class, Accounts.serializer())
        subclass(AccountDetail::class, AccountDetail.serializer())
        subclass(Insights::class, Insights.serializer())
        subclass(Help::class, Help.serializer())
        subclass(HelpTopicPage::class, HelpTopicPage.serializer())
        subclass(AddAccount::class, AddAccount.serializer())
        subclass(EditAccount::class, EditAccount.serializer())
        subclass(UpdateContributions::class, UpdateContributions.serializer())
        subclass(SetAutoContribution::class, SetAutoContribution.serializer())
        subclass(SetGoal::class, SetGoal.serializer())
        subclass(BackupRestore::class, BackupRestore.serializer())
      }
    }
}

/** Saves [TopLevelBackStack] across configuration changes and process death. */
internal val TopLevelBackStackSaver: Saver<TopLevelBackStack<NavKey>, String> =
  Saver(
    save = { it.saveState() },
    restore = { TopLevelBackStack.restore(it) },
  )
