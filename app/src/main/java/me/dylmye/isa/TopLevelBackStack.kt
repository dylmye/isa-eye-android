package me.dylmye.isa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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

  /** Opens [key]'s tab at its own root, dropping anything that was stacked above it. */
  fun resetTopLevel(key: T) {
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
}
