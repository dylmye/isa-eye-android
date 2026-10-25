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
 * This year's savings goal as a card that fills from the bottom, with the percentage saved as the
 * hero. No money is shown: the app is one tap away for the figures.
 */
class GoalWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Responsive(WidgetSizes)
  override val previewSizeMode = SizeMode.Responsive(WidgetSizes)

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { GoalContent(state) } }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { GoalContent(state) } }
  }
}

class GoalWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = GoalWidget()
}

@Composable
private fun GoalContent(state: WidgetState) {
  val goal = state.goal
  if (state.rulesetId == null || goal == null) {
    val empty =
      if (state.rulesetId == null) {
        "No tax year yet" to "Open ISA Eye to update its tax year data."
      } else {
        "No goal set" to "Tap to set this year's savings goal in Insights."
      }
    WidgetCard(
      tab = WidgetTab.Insights,
      fraction = 0f,
      description = "${empty.first}. ${empty.second}",
    ) {
      CardEmpty(title = empty.first, message = empty.second)
    }
    return
  }
  WidgetCard(
    tab = WidgetTab.Insights,
    fraction = goal.fraction,
    description = goalDescription(goal),
  ) {
    GoalFace(goal = goal)
  }
}

/** What the card says to a screen reader: money saved against the goal, not just a percentage. */
private fun goalDescription(goal: me.dylmye.isa.ui.insights.GoalProgress): String {
  val percent = (goal.fraction * 100).roundToInt()
  return if (goal.isMet) {
    "Savings goal reached: ${formatWidgetGbp(goal.contributedPence)} saved."
  } else {
    "Savings goal: ${formatWidgetGbp(goal.contributedPence)} of " +
      "${formatWidgetGbp(goal.targetPence)} saved, $percent percent."
  }
}

@Composable
private fun ColumnScope.GoalFace(goal: me.dylmye.isa.ui.insights.GoalProgress) {
  val percent = (goal.fraction * 100).roundToInt()
  Row(verticalAlignment = Alignment.CenterVertically) {
    WidgetBadge(R.drawable.ic_widget_target)
    Spacer(GlanceModifier.width(10.dp))
    CardLabel("Savings goal")
  }
  Spacer(GlanceModifier.defaultWeight())
  CardHero(percent = percent, qualifier = if (goal.isMet) "reached" else "saved")
}
