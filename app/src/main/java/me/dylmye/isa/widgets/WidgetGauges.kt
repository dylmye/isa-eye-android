package me.dylmye.isa.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import me.dylmye.isa.ui.insights.TaxYearStage
import kotlin.math.roundToInt

/**
 * A thick progress bar for [fraction] of the allowance still being available. The body is plain:
 * the Junior widget has no need for the battery cap the allowance card replaced.
 */
@Composable
internal fun WidgetBar(fraction: Float, fill: ColorProvider, height: Dp, description: String?) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val contentWidth = LocalSize.current.width - if (isCompact) 24.dp else 32.dp
  val widthPx = (contentWidth.value * density).roundToInt().coerceAtLeast(4)
  val heightPx = (height.value * density).roundToInt().coerceAtLeast(4)
  val track = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb()
  val fillArgb = fill.getColor(context).toArgb()
  val bitmap =
    remember(widthPx, heightPx, fraction, track, fillArgb) {
      progressBar(
        fraction = fraction,
        widthPx = widthPx,
        heightPx = heightPx,
        track = track,
        fill = fillArgb,
      )
    }
  Image(
    provider = ImageProvider(bitmap),
    contentDescription = description,
    modifier = GlanceModifier.fillMaxWidth().height(height),
    contentScale = ContentScale.FillBounds,
  )
}

/**
 * A progress ring with [content] laid over its transparent centre. The centre is where a widget
 * puts its one figure, so the ring carries the shape and the text carries the number.
 */
@Composable
internal fun WidgetRing(
  fraction: Float,
  size: Dp,
  fill: ColorProvider,
  track: ColorProvider,
  description: String?,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val sizePx = (size.value * density).roundToInt().coerceAtLeast(8)
  val strokePx = (sizePx * RING_STROKE_FRACTION).roundToInt().coerceAtLeast(2)
  val trackArgb = track.getColor(context).toArgb()
  val fillArgb = fill.getColor(context).toArgb()
  val bitmap =
    remember(sizePx, strokePx, fraction, trackArgb, fillArgb) {
      progressRing(
        fraction = fraction,
        sizePx = sizePx,
        strokePx = strokePx,
        track = trackArgb,
        fill = fillArgb,
      )
    }
  Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
    Image(
      provider = ImageProvider(bitmap),
      contentDescription = description,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
    )
    content()
  }
}

/** The stage colour the tax year has reached, on the same traffic-light scale as Insights. */
@Composable
internal fun stageColour(stage: TaxYearStage?): ColorProvider {
  val context = LocalContext.current
  val night = context.isNight()
  val resolved =
    remember(stage, night) {
      stage?.let { widgetColour(context, WidgetColours.stageResource(it)) }
    }
  return resolved ?: GlanceTheme.colors.primary
}

/** A colour resource resolved for the configuration the widget is composing in. */
private fun widgetColour(context: Context, resource: Int): ColorProvider =
  ColorProvider(Color(ContextCompat.getColor(context, resource)))

/** True when the device is in its night configuration. */
internal fun Context.isNight(): Boolean =
  resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
    Configuration.UI_MODE_NIGHT_YES

private const val RING_STROKE_FRACTION = 0.06f
