package me.dylmye.isa.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlin.math.roundToInt

/**
 * A card that is itself the progress bar: the filled share rises from the bottom edge and the
 * percentage sits on top of it. Material You's primary container is the card and its primary is the
 * fill, so the card follows the wallpaper palette and stays legible in light and dark.
 */
@Composable
internal fun WidgetCard(
  tab: WidgetTab,
  fraction: Float,
  content: @Composable ColumnScope.() -> Unit,
) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val widthPx = (LocalSize.current.width.value * density).roundToInt().coerceAtLeast(1)
  val heightPx = (LocalSize.current.height.value * density).roundToInt().coerceAtLeast(1)
  val card = GlanceTheme.colors.primaryContainer.getColor(context).toArgb()
  val fill = GlanceTheme.colors.primary.getColor(context).toArgb()
  val bitmap =
    remember(widthPx, heightPx, fraction, card, fill) {
      progressCard(
        widthPx = widthPx,
        heightPx = heightPx,
        fraction = fraction,
        card = card,
        fill = fill,
      )
    }
  Column(
    modifier =
      GlanceModifier.fillMaxSize()
        .appWidgetBackground()
        .background(ImageProvider(bitmap))
        .roundedOnS()
        .clickable(actionStartActivity(widgetIntent(context, tab)))
        .padding(horizontal = if (isCompact) 14.dp else 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
    horizontalAlignment = Alignment.Start,
  ) {
    content()
  }
}

/** The circular badge at the top of a card, with a tinted symbol inside. */
@Composable
internal fun WidgetBadge(icon: Int) {
  val context = LocalContext.current
  val density = context.resources.displayMetrics.density
  val size = if (isCompact) 30.dp else 36.dp
  val sizePx = (size.value * density).roundToInt().coerceAtLeast(4)
  val circle = GlanceTheme.colors.primary.getColor(context).toArgb()
  val badge = remember(sizePx, circle) { badgeCircle(sizePx, circle) }
  Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
    Image(
      provider = ImageProvider(badge),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
    )
    Image(
      provider = ImageProvider(icon),
      contentDescription = null,
      modifier = GlanceModifier.size(size * ICON_FRACTION),
      colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
    )
  }
}

/** The short label beside the badge. */
@Composable
internal fun CardLabel(text: String) {
  Text(
    text = text,
    maxLines = 1,
    style =
      TextStyle(
        color = GlanceTheme.colors.onPrimaryContainer,
        fontSize = if (isCompact) 12.sp else 14.sp,
        fontWeight = FontWeight.Medium,
      ),
  )
}

/**
 * The percentage, pinned to the bottom of the card and sized to the available height. The qualifier
 * ("left" or "saved") sits beside it when there is width, and is left off the 2x2 card where it
 * would crowd the figure.
 */
@Composable
internal fun CardHero(percent: Int, qualifier: String) {
  val compact = isCompact
  val large = isLarge
  val figureStyle =
    TextStyle(
      color = GlanceTheme.colors.onPrimaryContainer,
      fontSize =
        when {
          large -> 60.sp
          compact -> 34.sp
          else -> 36.sp
        },
      fontWeight = FontWeight.Bold,
    )
  if (compact) {
    Text(text = "$percent%", maxLines = 1, style = figureStyle)
  } else {
    Row(verticalAlignment = Alignment.Bottom) {
      Text(text = "$percent%", maxLines = 1, style = figureStyle)
      Spacer(GlanceModifier.width(8.dp))
      Text(
        text = qualifier,
        maxLines = 1,
        style =
          TextStyle(
            color = GlanceTheme.colors.onPrimaryContainer,
            fontSize = if (large) 14.sp else 12.sp,
          ),
      )
    }
  }
}

/** The card's empty state, kept on the same container colour as the live card. */
@Composable
internal fun CardEmpty(title: String, message: String) {
  Text(
    text = title,
    maxLines = 2,
    style =
      TextStyle(
        color = GlanceTheme.colors.onPrimaryContainer,
        fontSize = if (isCompact) 18.sp else 22.sp,
        fontWeight = FontWeight.Bold,
      ),
  )
  Spacer(GlanceModifier.height(6.dp))
  Text(
    text = message,
    maxLines = 3,
    style =
      TextStyle(
        color = GlanceTheme.colors.onPrimaryContainer,
        fontSize = 12.sp,
      ),
  )
}

private const val ICON_FRACTION = 0.55f
