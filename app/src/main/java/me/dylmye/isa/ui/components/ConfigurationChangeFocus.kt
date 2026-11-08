package me.dylmye.isa.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Gives a text field focus again after a configuration change, such as rotating, folding or
 * resizing the window.
 *
 * Compose restores the text a field holds but not its focus, so without this the field the user was
 * typing in goes quiet and the soft keyboard closes whenever the window is rebuilt. The flag latches
 * when the field gains focus and clears when the user moves focus to another control, so a field
 * the user has left does not steal focus or reopen the keyboard on the next configuration change.
 *
 * Losing focus while the old window is torn down is ignored: the hosting lifecycle has already
 * dropped below `RESUMED` by then, so the flag survives the change.
 *
 * Pair this with `KeyboardOptions(showKeyboardOnFocus = true)` so the keyboard comes back with the
 * focus.
 */
@Composable
fun Modifier.restoreFocusAfterConfigChange(): Modifier {
  val focusRequester = remember { FocusRequester() }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var wasFocused by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(focusRequester) {
    if (wasFocused) focusRequester.requestFocus()
  }

  return this.focusRequester(focusRequester).onFocusChanged { focusState ->
    if (focusState.isFocused) {
      wasFocused = true
    } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      // Still resumed, so the window is interactive and the user moved focus elsewhere. During a
      // configuration change this callback runs below RESUMED and must leave the flag alone.
      wasFocused = false
    }
  }
}
