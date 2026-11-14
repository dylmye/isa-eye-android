package me.dylmye.isa

import android.view.KeyEvent
import androidx.navigation3.runtime.NavKey

/**
 * The top-level tab a Ctrl+number shortcut selects, in the order the tabs appear in the navigation
 * bar. Null for any other key.
 */
internal fun tabShortcutTarget(keyCode: Int): NavKey? = when (keyCode) {
  KeyEvent.KEYCODE_1 -> Accounts
  KeyEvent.KEYCODE_2 -> Insights
  KeyEvent.KEYCODE_3 -> Help
  else -> null
}

/**
 * True for the add-account chord. Ctrl is the primary modifier on Android, while Meta is the
 * desktop-native one on macOS, Windows and ChromeOS keyboards, so either is accepted.
 */
internal fun isAddAccountChord(keyCode: Int, ctrlPressed: Boolean, metaPressed: Boolean): Boolean =
  keyCode == KeyEvent.KEYCODE_N && (ctrlPressed || metaPressed)
