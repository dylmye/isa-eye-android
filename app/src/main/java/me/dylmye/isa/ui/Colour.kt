package me.dylmye.isa.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

private val FallbackTint = Color(0xFF9E9E9E)

/** Parses a stored `#rrggbb` tint, falling back to grey when the value is not one. */
internal fun String.toTint(): Color = runCatching { Color(toColorInt()) }.getOrDefault(FallbackTint)
