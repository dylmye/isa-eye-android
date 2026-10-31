package me.dylmye.isa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey

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

  fun removeLast() {
    val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
    removedKey?.let { topLevelStacks.remove(it) }
    topLevelKey = topLevelStacks.keys.last()
    updateBackStack()
  }

  /**
   * A compact, saveable snapshot of the per-tab stacks and the selected tab. Used by
   * [TopLevelBackStackSaver] so a configuration change or process death does not reset navigation.
   */
  internal fun saveState(): String? {
    val top = topLevelKey.encode()
    val stacks = topLevelStacks.values.map { stack -> stack.map { it.encode() } }
    if (top == null || stacks.any { stack -> stack.any { it == null } }) return null
    return buildString {
      append(top)
      stacks.forEach { stack ->
        append(STACK_SEPARATOR)
        append(stack.joinToString(ITEM_SEPARATOR) { it.orEmpty() })
      }
    }
  }

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
    private const val STACK_SEPARATOR = ";"
    private const val ITEM_SEPARATOR = "|"

    /** Rebuilds a stack from [saveState], or null when the snapshot is malformed. */
    fun restore(state: String): TopLevelBackStack<NavKey>? {
      val parts = state.split(STACK_SEPARATOR)
      val top = if (parts.size >= 2) decode(parts.first()) else null
      val stacks = parts.drop(1).map { part -> part.split(ITEM_SEPARATOR).map(::decode) }
      if (top == null ||
        stacks.any { stack -> stack.any { it == null } } ||
        stacks.none { it.firstOrNull() == top }
      ) {
        return null
      }
      return TopLevelBackStack(top, stacks.map { stack -> stack.filterNotNull() })
    }

    private fun NavKey.encode(): String? = when (this) {
      Accounts -> "accounts"
      is AccountDetail -> "account:$productId"
      Insights -> "insights"
      Help -> "help"
      is HelpTopicPage -> "topic:$id"
      AddAccount -> "add"
      is EditAccount -> "edit:$productId"
      is UpdateContributions -> "update:$productId"
      is SetAutoContribution -> "auto:$productId"
      SetGoal -> "goal"
      BackupRestore -> "backup"
      else -> null
    }

    private fun decode(value: String): NavKey? = when (value) {
      "accounts" -> Accounts
      "insights" -> Insights
      "help" -> Help
      "add" -> AddAccount
      "goal" -> SetGoal
      "backup" -> BackupRestore
      else -> decodePrefixed(value)
    }

    private fun decodePrefixed(value: String): NavKey? {
      val separator = value.indexOf(':')
      if (separator <= 0 || separator == value.lastIndex) return null
      val id = value.substring(separator + 1)
      return when (value.substring(0, separator)) {
        "account" -> AccountDetail(id)
        "topic" -> HelpTopicPage(id)
        "edit" -> EditAccount(id)
        "update" -> UpdateContributions(id)
        "auto" -> SetAutoContribution(id)
        else -> null
      }
    }
  }
}

/** Saves [TopLevelBackStack] across configuration changes and process death. */
internal val TopLevelBackStackSaver: Saver<TopLevelBackStack<NavKey>, String> =
  Saver(
    save = { it.saveState() },
    restore = { TopLevelBackStack.restore(it) },
  )
