package me.dylmye.isa

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutTest {
  @Test
  fun tabShortcuts_selectTheTopLevelTabsInNavigationOrder() {
    assertEquals(Accounts, tabShortcutTarget(KeyEvent.KEYCODE_1))
    assertEquals(Insights, tabShortcutTarget(KeyEvent.KEYCODE_2))
    assertEquals(Help, tabShortcutTarget(KeyEvent.KEYCODE_3))
  }

  @Test
  fun tabShortcuts_ignoreEveryOtherKey() {
    assertNull(tabShortcutTarget(KeyEvent.KEYCODE_4))
    assertNull(tabShortcutTarget(KeyEvent.KEYCODE_N))
  }

  @Test
  fun addAccountShortcut_acceptsCtrlOrMeta() {
    assertTrue(isAddAccountChord(KeyEvent.KEYCODE_N, ctrlPressed = true, metaPressed = false))
    assertTrue(isAddAccountChord(KeyEvent.KEYCODE_N, ctrlPressed = false, metaPressed = true))
    assertTrue(isAddAccountChord(KeyEvent.KEYCODE_N, ctrlPressed = true, metaPressed = true))
  }

  @Test
  fun addAccountShortcut_needsNWithAModifier() {
    assertFalse(isAddAccountChord(KeyEvent.KEYCODE_N, ctrlPressed = false, metaPressed = false))
    assertFalse(isAddAccountChord(KeyEvent.KEYCODE_M, ctrlPressed = true, metaPressed = true))
  }
}
