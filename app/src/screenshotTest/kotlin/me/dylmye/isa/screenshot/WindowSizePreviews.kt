package me.dylmye.isa.screenshot

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/*
 * Preview names must not contain a full stop. The engine derives a reference image's filename by
 * taking everything after the last one, which both truncates the name and drops the class-name
 * prefix, so two previews sharing a set of parameters collide on a single file.
 */

/**
 * The nine window sizes used for screen-level screenshot tests: compact, medium and expanded widths
 * (400, 610 and 900dp) crossed with short, regular and tall heights (400, 500 and 1000dp).
 *
 * The widths straddle the 600dp breakpoint the layouts branch on, so all three adaptive
 * presentations are covered. The last two entries pin the theme and the font scale, which the
 * window sizes above deliberately leave at their defaults.
 */
@Preview(name = "Compact 400x400", widthDp = 400, heightDp = 400)
@Preview(name = "Compact 400x500", widthDp = 400, heightDp = 500)
@Preview(name = "Compact 400x1000", widthDp = 400, heightDp = 1000)
@Preview(name = "Medium 610x400", widthDp = 610, heightDp = 400)
@Preview(name = "Medium 610x500", widthDp = 610, heightDp = 500)
@Preview(name = "Medium 610x1000", widthDp = 610, heightDp = 1000)
@Preview(name = "Expanded 900x400", widthDp = 900, heightDp = 400)
@Preview(name = "Expanded 900x500", widthDp = 900, heightDp = 500)
@Preview(name = "Expanded 900x1000", widthDp = 900, heightDp = 1000)
@Preview(
  name = "Dark 400x500",
  widthDp = 400,
  heightDp = 500,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Large font 400x500", widthDp = 400, heightDp = 500, fontScale = 1.5f)
annotation class ScreenPreviews

/**
 * The compact-width sizes, for the account forms. Below 600dp the app renders them full-screen, so
 * these previews pass `isCompact = true` and the widths here are the ones that match.
 */
@Preview(name = "Compact 400x400", widthDp = 400, heightDp = 400)
@Preview(name = "Compact 400x500", widthDp = 400, heightDp = 500)
@Preview(name = "Compact 400x1000", widthDp = 400, heightDp = 1000)
@Preview(
  name = "Dark 400x500",
  widthDp = 400,
  heightDp = 500,
  uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Large font 400x500", widthDp = 400, heightDp = 500, fontScale = 1.5f)
annotation class CompactFormPreviews

/**
 * The medium and expanded widths, for the account forms. From 600dp the app floats them as a card,
 * so these previews pass `isCompact = false`.
 */
@Preview(name = "Medium 610x400", widthDp = 610, heightDp = 400)
@Preview(name = "Medium 610x500", widthDp = 610, heightDp = 500)
@Preview(name = "Medium 610x1000", widthDp = 610, heightDp = 1000)
@Preview(name = "Expanded 900x400", widthDp = 900, heightDp = 400)
@Preview(name = "Expanded 900x500", widthDp = 900, heightDp = 500)
@Preview(name = "Expanded 900x1000", widthDp = 900, heightDp = 1000)
annotation class ExpandedFormPreviews
