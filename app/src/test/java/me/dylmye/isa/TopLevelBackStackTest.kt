package me.dylmye.isa

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable private data object TabA : NavKey

@Serializable private data object TabB : NavKey

@Serializable private data class Detail(val id: String) : NavKey

class TopLevelBackStackTest {
  @Test
  fun addTopLevel_switchesTab_andKeepsEachStack() {
    val backStack = TopLevelBackStack<NavKey>(TabA)
    backStack.add(Detail("1"))
    backStack.addTopLevel(TabB)

    assertEquals(TabB, backStack.topLevelKey)
    assertEquals(listOf(TabA, Detail("1"), TabB), backStack.backStack.toList())

    backStack.addTopLevel(TabA)
    assertEquals(TabA, backStack.topLevelKey)
    assertEquals(listOf(TabB, TabA, Detail("1")), backStack.backStack.toList())
  }

  @Test
  fun add_addsToCurrentTabOnly() {
    val backStack = TopLevelBackStack<NavKey>(TabA)
    backStack.add(Detail("1"))
    backStack.addTopLevel(TabB)
    backStack.add(Detail("2"))

    assertEquals(listOf(TabA, Detail("1"), TabB, Detail("2")), backStack.backStack.toList())

    backStack.addTopLevel(TabA)
    assertEquals(listOf(TabB, Detail("2"), TabA, Detail("1")), backStack.backStack.toList())
    assertEquals(TabA, backStack.topLevelKey)
  }

  @Test
  fun removeLast_popsCurrentTab_andReturnsToPreviousTab() {
    val backStack = TopLevelBackStack<NavKey>(TabA)
    backStack.addTopLevel(TabB)
    backStack.add(Detail("1"))
    backStack.removeLast()

    assertEquals(listOf(TabA, TabB), backStack.backStack.toList())
    assertEquals(TabB, backStack.topLevelKey)

    backStack.removeLast()
    assertEquals(listOf(TabA), backStack.backStack.toList())
    assertEquals(TabA, backStack.topLevelKey)
  }
}
