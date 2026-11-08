package me.dylmye.isa.ui

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Enforces a pounds amount while it is typed: digits, one decimal point and at most two decimals.
 *
 * Filtering happens on the [TextFieldBuffer], synchronously, so the field and the IME agree on the
 * accepted text immediately. Sanitising a value after it has been accepted, through an
 * `onValueChange` callback and a state update, leaves the IME and the cursor out of step whenever
 * characters are removed.
 */
internal object PoundsInputTransformation : InputTransformation {
  override fun TextFieldBuffer.transformInput() {
    val original = asCharSequence().toString()
    val sanitized = sanitizePoundsInput(original)
    // The sanitised text is always the original with characters removed, so matching it from the
    // end and deleting only what is left over keeps the cursor and the composing region in place.
    var bufferIndex = original.lastIndex
    var sanitizedIndex = sanitized.lastIndex
    while (bufferIndex >= 0) {
      if (sanitizedIndex >= 0 && original[bufferIndex] == sanitized[sanitizedIndex]) {
        bufferIndex--
        sanitizedIndex--
      } else {
        replace(bufferIndex, bufferIndex + 1, "")
        bufferIndex--
      }
    }
  }
}
