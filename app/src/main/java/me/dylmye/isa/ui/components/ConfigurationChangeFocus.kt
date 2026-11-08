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
import androidx.compose.ui.platform.LocalInspectionMode
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
 * Set [focusOnOpen] on the first field of a form to give it focus as the screen or dialog appears,
 * so the user can start typing without clicking. The initial request is remembered across
 * configuration changes: a rebuilt composition takes the focus-restoration path instead of
 * replaying the request, which would pull focus back from wherever the user has since moved.
 *
 * Pair this with `KeyboardOptions(showKeyboardOnFocus = true)` so the keyboard comes back with the
 * focus.
 *
 * Previews and screenshot tests never request focus: opening a form there would show a cursor and
 * scroll the field into view, which makes the rendered layout depend on focus rather than just the
 * state being previewed.
 */
@Composable
fun Modifier.restoreFocusAfterConfigChange(focusOnOpen: Boolean = false): Modifier {
  val focusRequester = remember { FocusRequester() }
  val inspectionMode = LocalInspectionMode.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var wasFocused by rememberSaveable { mutableStateOf(false) }
  var didRequestFocusOnOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(focusRequester) {
    if (inspectionMode) return@LaunchedEffect
    when {
      wasFocused -> {
        focusRequester.requestFocus()
      }

      focusOnOpen && !didRequestFocusOnOpen -> {
        // Mark first so a rebuild after teardown does not request focus a second time.
        didRequestFocusOnOpen = true
        focusRequester.requestFocus()
      }
    }
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
