package me.dylmye.isa.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * Reports every change of this state to [onTextChange], including the text the state starts with.
 *
 * The state owns the text, so there is no asynchronous round trip that could rewrite the field
 * while the user types. Callers that keep a ViewModel draft in step can treat [onTextChange] as a
 * one-way notification.
 */
@Composable
internal fun TextFieldState.reportTextChanges(onTextChange: (String) -> Unit) {
  val currentOnTextChange by rememberUpdatedState(onTextChange)
  LaunchedEffect(this) {
    snapshotFlow { text.toString() }.collect { currentOnTextChange(it) }
  }
}

/**
 * A read-only field's state, kept in step with [value].
 *
 * Mirroring is safe here because the field cannot be edited. Editable fields must seed once instead
 * and own their text afterwards, or typing would be overwritten by the value they last reported.
 */
@Composable
internal fun rememberDisplayTextState(value: String): TextFieldState {
  val state = rememberTextFieldState(value)
  LaunchedEffect(value) {
    if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
  }
  return state
}
