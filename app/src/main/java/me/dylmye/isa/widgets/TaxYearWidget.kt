package me.dylmye.isa.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlin.math.roundToInt

/**
 * How much of the tax year is left, in a circular 2x2 widget. The ring drains with the remaining
 * days and the countdown sits inside it; there is no year-in-pixels grid while the ring is the
 * chosen shape.
 */
class TaxYearWidget : GlanceAppWidget() {
  // Exact sizing lets the ring take its 75% of the real 2x2 card rather than of a size bucket.
  override val sizeMode = SizeMode.Exact

  override val previewSizeMode = SizeMode.Single

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { TaxYearContent(state) } }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val state = loadWidgetState(context)
    provideContent { WidgetTheme { TaxYearContent(state) } }
  }
}

class TaxYearWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = TaxYearWidget()
}

@Composable
private fun TaxYearContent(state: WidgetState) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val card = LocalSize.current
  val circleSize = minOf(card.width, card.height)
  val circlePx = (circleSize.value * density).roundToInt().coerceAtLeast(4)
  val circleColour = GlanceTheme.colors.surface.getColor(context).toArgb()
  val circle = remember(circlePx, circleColour) { badgeCircle(circlePx, circleColour) }
  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .appWidgetBackground()
        .clickable(actionStartActivity(widgetIntent(context, WidgetTab.Insights))),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      provider = ImageProvider(circle),
      contentDescription = null,
      modifier = GlanceModifier.size(circleSize),
      contentScale = ContentScale.Fit,
    )
    val daysLeft = state.daysRemaining
    val totalDays = state.totalDays
    if (state.rulesetId == null || daysLeft == null || totalDays == null) {
      TaxYearEmpty()
    } else {
      TaxYearFace(state = state, daysLeft = daysLeft, totalDays = totalDays, size = circleSize)
    }
  }
}

@Composable
private fun TaxYearFace(state: WidgetState, daysLeft: Int, totalDays: Int, size: Dp) {
  val stage = stageColour(state.stage)
  WidgetRing(
    fraction = state.remainingYearFraction,
    size = size * RING_FRACTION,
    fill = stage,
    track = GlanceTheme.colors.surfaceVariant,
    description = "$daysLeft of $totalDays days left in the ${state.yearLabel} tax year.",
  ) {
    Column(
      modifier = GlanceModifier.padding(horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = daysLeft.toString(),
        maxLines = 1,
        style = TextStyle(color = stage, fontSize = 34.sp, fontWeight = FontWeight.Bold),
      )
      Text(
        text = if (daysLeft == 1) "day left" else "days left",
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
      )
    }
  }
}

@Composable
private fun TaxYearEmpty() {
  Column(
    modifier = GlanceModifier.padding(horizontal = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "No tax year",
      maxLines = 2,
      style =
        TextStyle(
          color = GlanceTheme.colors.onSurface,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(GlanceModifier.height(4.dp))
    Text(
      text = "Open ISA Eye to update its data.",
      maxLines = 3,
      style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
    )
  }
}

/** The ring fills three quarters of the circular card. */
private const val RING_FRACTION = 0.88f
