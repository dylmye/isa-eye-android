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

/**
 * Gives a text field focus again after a configuration change, such as rotating, folding or
 * resizing the window.
 *
 * Compose restores the text a field holds but not its focus, so without this the field the user was
 * typing in goes quiet and the soft keyboard closes whenever the window is rebuilt. The flag only
 * latches when the field gains focus; losing focus while the old window is torn down must not clear
 * it.
 *
 * Pair this with `KeyboardOptions(showKeyboardOnFocus = true)` so the keyboard comes back with the
 * focus.
 */
@Composable
fun Modifier.restoreFocusAfterConfigChange(): Modifier {
  val focusRequester = remember { FocusRequester() }
  var wasFocused by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(focusRequester) {
    if (wasFocused) focusRequester.requestFocus()
  }

  return this.focusRequester(focusRequester).onFocusChanged { focusState ->
    if (focusState.isFocused) wasFocused = true
  }
}
