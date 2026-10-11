package me.dylmye.isa.ui

import java.text.NumberFormat
import java.util.Locale

private val GbpFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.UK)

/** Formats an integer amount of [pence] as pounds sterling, e.g. `1250` becomes `£12.50`. */
internal fun formatGbp(pence: Long): String = GbpFormat.format(pence / 100.0)
