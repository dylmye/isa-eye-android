package me.dylmye.isa.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import me.dylmye.isa.R
import kotlin.math.roundToInt

/**
 * The shared ISA allowance as a card that fills from the bottom as the year's £20,000 is used. The
 * percentage used is the figure the widget leads with, so it fills alongside the savings goal; the
 * Junior ISA has its own widget because its allowance is per child.
 */
class AllowanceWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Responsive(WidgetSizes)
  override val previewSizeMode = SizeMode.Responsive(WidgetSizes)

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { AllowanceContent(state) } }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { AllowanceContent(state) } }
  }
}

class AllowanceWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = AllowanceWidget()
}

@Composable
private fun AllowanceContent(state: WidgetState) {
  val empty =
    when {
      !state.hasAccounts -> {
        "No accounts yet" to "Tap to add your first ISA in ISA Eye."
      }

      state.rulesetId == null -> {
        "No tax year yet" to "Open ISA Eye to update its tax year data."
      }

      !state.hasSharedAccounts -> {
        "Junior ISAs only" to "Use the Junior ISA allowance widget for this year."
      }

      else -> {
        null
      }
    }
  if (empty != null) {
    WidgetCard(tab = WidgetTab.Accounts, fraction = 0f) {
      CardEmpty(title = empty.first, message = empty.second)
    }
    return
  }
  WidgetCard(tab = WidgetTab.Accounts, fraction = state.sharedUsedFraction) {
    AllowanceFace(state)
  }
}

@Composable
private fun ColumnScope.AllowanceFace(state: WidgetState) {
  val percent = (state.sharedUsedFraction * 100).roundToInt()
  Row(verticalAlignment = Alignment.CenterVertically) {
    WidgetBadge(R.drawable.ic_widget_piggy_bank)
    Spacer(GlanceModifier.width(10.dp))
    CardLabel("ISA allowance")
  }
  Spacer(GlanceModifier.defaultWeight())
  CardHero(percent = percent, qualifier = "used")
}
