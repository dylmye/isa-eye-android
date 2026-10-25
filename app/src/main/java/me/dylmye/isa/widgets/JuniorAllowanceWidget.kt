package me.dylmye.isa.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.dylmye.isa.ui.accounts.JuniorAllowanceUiState
import kotlin.math.roundToInt

/**
 * The Junior ISA allowance, which is per child and sits outside the shared £20,000. A single child
 * gets a battery of their own; several children get one cell per account, because a combined figure
 * would misrepresent a limit each of them holds separately.
 */
class JuniorAllowanceWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Responsive(WidgetSizes)
  override val previewSizeMode = SizeMode.Responsive(WidgetSizes)

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { JuniorAllowanceContent(state) } }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { JuniorAllowanceContent(state) } }
  }
}

class JuniorAllowanceWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = JuniorAllowanceWidget()
}

@Composable
private fun JuniorAllowanceContent(state: WidgetState) {
  val juniors = state.juniors
  val description =
    when {
      !state.hasAccounts -> {
        "No accounts yet. Tap to add a Junior ISA in ISA Eye."
      }

      juniors.isEmpty() -> {
        "No Junior ISAs. Tap to add one for this tax year in ISA Eye."
      }

      juniors.size == 1 -> {
        juniorMeterDescription(juniors.first())
      }

      else -> {
        juniors.joinToString(prefix = "Junior ISA allowances: ", separator = " ") {
          juniorMeterDescription(it)
        }
      }
    }
  WidgetFrame(WidgetTab.Accounts, description = description) {
    when {
      !state.hasAccounts -> {
        WidgetEmpty("No accounts yet", "Tap to add a Junior ISA in ISA Eye.")
      }

      juniors.isEmpty() -> {
        WidgetEmpty("No Junior ISAs", "Tap to add one for this tax year in ISA Eye.")
      }

      juniors.size == 1 -> {
        JuniorSoleAccount(juniors.first())
      }

      else -> {
        JuniorAccounts(juniors)
      }
    }
  }
}

@Composable
private fun JuniorSoleAccount(junior: JuniorAllowanceUiState) {
  val compact = isCompact
  val large = isLarge
  val percent = (junior.remainingFraction * 100).roundToInt()
  WidgetLabelRow(
    title = junior.accountName.ifBlank { "Junior ISA" },
    trailing = if (compact) null else "$percent% left",
  )
  Spacer(GlanceModifier.height(2.dp))
  WidgetHeadline(
    text = if (junior.isUsedUp) "Used up" else formatWidgetGbp(junior.remainingPence),
    colour =
      if (junior.isUsedUp) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
    fontSize = widgetHeadlineSize(compact, large),
    caption =
      if (compact) {
        null
      } else if (junior.isUsedUp) {
        "of ${formatWidgetGbp(junior.allowancePence)} used"
      } else {
        "of ${formatWidgetGbp(junior.allowancePence)}"
      },
  )
  if (compact) {
    Spacer(GlanceModifier.height(2.dp))
    WidgetCaption(
      if (junior.isUsedUp) {
        "of ${formatWidgetGbp(junior.allowancePence)} used"
      } else {
        "of ${formatWidgetGbp(junior.allowancePence)}"
      },
      maxLines = 1,
    )
  }
  Spacer(GlanceModifier.height(if (large) 12.dp else 6.dp))
  WidgetBar(
    fraction = junior.remainingFraction,
    fill = GlanceTheme.colors.primary,
    height = when {
      large -> 40.dp
      compact -> 14.dp
      else -> 18.dp
    },
    description = juniorMeterDescription(junior),
  )
  if (large) {
    Spacer(GlanceModifier.height(10.dp))
    WidgetCaption(
      "${formatWidgetGbp(junior.usedPence.coerceAtMost(junior.allowancePence))} saved this year",
    )
  }
}

@Composable
private fun JuniorAccounts(juniors: List<JuniorAllowanceUiState>) {
  val size = LocalSize.current
  val wide = size.width >= 200.dp
  val columns = if (wide) 2 else 1
  // A wide 4x2 widget fits one row of two; the 4x4 fits a small grid. Anything beyond that is
  // counted in the title row rather than squeezed into an unreadable cell.
  val rows = if (size.height >= 200.dp) 3 else 1
  val shown = (rows * columns).coerceAtMost(juniors.size)
  val hidden = juniors.size - shown
  val typeName = juniors.first().typeName.ifBlank { "Junior ISA" }
  WidgetLabelRow(
    title = if (wide) "$typeName allowances" else "Junior ISA",
    trailing =
      when {
        hidden <= 0 -> null
        wide -> "$hidden more"
        else -> "$shown/${juniors.size}"
      },
  )
  Spacer(GlanceModifier.height(8.dp))
  juniors.take(shown).chunked(columns).forEachIndexed { index, row ->
    if (index > 0) Spacer(GlanceModifier.height(10.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
      row.forEachIndexed { cell, junior ->
        if (cell > 0) Spacer(GlanceModifier.width(12.dp))
        JuniorCell(junior, modifier = GlanceModifier.defaultWeight())
      }
      repeat(columns - row.size) {
        Spacer(GlanceModifier.defaultWeight())
      }
    }
  }
}

/** One child's cell in the multi-account widget: name, what is left, and a slim battery. */
@Composable
private fun JuniorCell(junior: JuniorAllowanceUiState, modifier: GlanceModifier) {
  Column(modifier = modifier) {
    WidgetLabel(junior.accountName.ifBlank { junior.typeName.ifBlank { "Junior ISA" } })
    Spacer(GlanceModifier.height(2.dp))
    Text(
      text = if (junior.isUsedUp) "Used up" else formatWidgetGbp(junior.remainingPence),
      maxLines = 1,
      style =
        TextStyle(
          color = GlanceTheme.colors.onSurface,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(GlanceModifier.height(5.dp))
    WidgetBar(
      fraction = junior.remainingFraction,
      fill = GlanceTheme.colors.primary,
      height = 8.dp,
      description = juniorMeterDescription(junior),
    )
  }
}

/** The screen-reader description of one Junior account's battery. */
private fun juniorMeterDescription(junior: JuniorAllowanceUiState): String = if (junior.isUsedUp) {
  "${junior.accountName} has used the " +
    "${formatWidgetGbp(junior.allowancePence)} Junior ISA allowance."
} else {
  "${junior.accountName} has ${formatWidgetGbp(junior.remainingPence)} of the " +
    "${formatWidgetGbp(junior.allowancePence)} Junior ISA allowance left."
}
