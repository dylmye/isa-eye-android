package me.dylmye.isa.widgets

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import me.dylmye.isa.ui.formatGbp

/**
 * Wraps a widget in Glance's Material 3 theme. The night mode is read from the system resources and
 * the composition gets a matching configuration context: a widget update can run in a process whose
 * own configuration is stale, and mixing the two leaves text and baked bitmaps on different themes.
 */
@Composable
internal fun WidgetTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val systemUiMode = Resources.getSystem().configuration.uiMode
  val night =
    systemUiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
  val configuration =
    Configuration(context.resources.configuration).apply {
      uiMode =
        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        (systemUiMode and Configuration.UI_MODE_NIGHT_MASK)
    }
  val themed = context.createConfigurationContext(configuration)
  val scheme =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (night) dynamicDarkColorScheme(themed) else dynamicLightColorScheme(themed)
    } else if (night) {
      darkColorScheme()
    } else {
      lightColorScheme()
    }
  CompositionLocalProvider(LocalContext provides themed) {
    GlanceTheme(colors = ColorProviders(scheme)) { content() }
  }
}

/** A muted line above the figure, e.g. the widget's title. */
@Composable
internal fun WidgetLabel(text: String, modifier: GlanceModifier = GlanceModifier) {
  Text(
    text = text,
    modifier = modifier,
    maxLines = 1,
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      ),
  )
}

/** A supporting line under the figure or meter. */
@Composable
internal fun WidgetCaption(
  text: String,
  modifier: GlanceModifier = GlanceModifier,
  maxLines: Int = 2,
) {
  Text(
    text = text,
    modifier = modifier,
    maxLines = maxLines,
    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
  )
}

/** A title row with an optional quiet value on the right, e.g. the days left in the year. */
@Composable
internal fun WidgetLabelRow(title: String, trailing: String? = null) {
  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    WidgetLabel(title, modifier = GlanceModifier.defaultWeight())
    if (trailing != null) {
      WidgetCaption(trailing, maxLines = 1)
    }
  }
}

/** The one big figure a widget leads with, with an optional quiet caption beside it. */
@Composable
internal fun WidgetHeadline(
  text: String,
  colour: ColorProvider,
  fontSize: TextUnit,
  caption: String? = null,
) {
  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = text,
      maxLines = 1,
      modifier = GlanceModifier.defaultWeight(),
      style = TextStyle(color = colour, fontSize = fontSize, fontWeight = FontWeight.Bold),
    )
    if (caption != null) {
      WidgetCaption(caption, maxLines = 1)
    }
  }
}

/** The empty state a widget shows until there is something to say. */
@Composable
internal fun WidgetEmpty(title: String, message: String) {
  Text(
    text = title,
    maxLines = 2,
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = if (isCompact) 16.sp else 20.sp,
        fontWeight = FontWeight.Bold,
      ),
  )
  Spacer(GlanceModifier.height(6.dp))
  WidgetCaption(message, maxLines = 4)
}

/** The one headline size every widget uses, so a resize changes type consistently. */
internal fun widgetHeadlineSize(compact: Boolean, large: Boolean): TextUnit = when {
  compact -> 24.sp
  large -> 40.sp
  else -> 32.sp
}

/** [pence] without a pointless ".00", so a widget's big figure stays inside its cell. */
internal fun formatWidgetGbp(pence: Long): String = formatGbp(pence).removeSuffix(".00")

/** "1 day" or "5 days". */
internal fun days(count: Int): String = if (count == 1) "1 day" else "$count days"
