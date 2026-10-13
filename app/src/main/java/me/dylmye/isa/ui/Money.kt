package me.dylmye.isa.ui

import java.text.NumberFormat
import java.util.Locale

private val GbpFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.UK)

/** Caps the pounds digits so the parsed pence always stays well inside [Long] range. */
private const val MAX_WHOLE_DIGITS = 9

/** Formats an integer amount of [pence] as pounds sterling, e.g. `1250` becomes `£12.50`. */
internal fun formatGbp(pence: Long): String = GbpFormat.format(pence / 100.0)

/** Keeps only a valid pounds entry: digits, an optional decimal point and up to two decimals. */
internal fun sanitizePoundsInput(text: String): String {
  val cleaned = text.filter { it.isDigit() || it == '.' }
  val point = cleaned.indexOf('.')
  val whole = (if (point == -1) cleaned else cleaned.substring(0, point)).take(MAX_WHOLE_DIGITS)
  if (point == -1) return whole
  val fraction = cleaned.substring(point + 1).filter { it.isDigit() }.take(2)
  return "$whole.$fraction"
}

/** Parses a pounds entry such as "1250" or "1250.50" into pence, or null when it isn't a number. */
internal fun parsePoundsToPence(text: String): Long? {
  val point = text.indexOf('.')
  val whole = (if (point == -1) text else text.substring(0, point)).ifEmpty { "0" }
  val fraction = if (point == -1) "" else text.substring(point + 1)
  val pounds = whole.takeIf { it.length <= MAX_WHOLE_DIGITS }?.toLongOrNull()
  val pence = fraction.takeIf { it.length <= 2 }?.padEnd(2, '0')?.toLongOrNull()
  if (pounds == null || pence == null || text.none { it.isDigit() }) return null
  return pounds * 100 + pence
}

/** Renders [pence] as a plain pounds entry for editing, e.g. `1250` becomes "12.50". */
internal fun penceToPoundsInput(pence: Long): String {
  val positive = pence.coerceAtLeast(0)
  val pounds = positive / 100
  val remainder = positive % 100
  if (remainder == 0L) return pounds.toString()
  return "$pounds.${remainder.toString().padStart(2, '0')}"
}
