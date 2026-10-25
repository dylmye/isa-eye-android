package me.dylmye.isa.widgets

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import me.dylmye.isa.MainActivity

/**
 * The size buckets every widget provides. They follow the platform's minimum widget sizes: 2x2,
 * 4x2 and 4x4. Glance composes each one and the launcher picks the best fit, so the widgets respond
 * to a resize instead of stretching one layout. A very tall, narrow 2x4 placement takes the 2x2
 * composition rather than a cramped layout written for a width it does not have.
 */
internal val WidgetSizes =
  setOf(
    DpSize(110.dp, 110.dp),
    DpSize(250.dp, 110.dp),
    DpSize(250.dp, 250.dp),
  )

/** True for the 2x2 bucket, where only the essential line or two fit. */
internal val isCompact: Boolean
  @Composable get() = LocalSize.current.width < 200.dp

/** True for the 4x4 bucket, which has room for every supporting line. */
internal val isLarge: Boolean
  @Composable get() = LocalSize.current.height > 200.dp && LocalSize.current.width >= 200.dp

/**
 * The surface every widget sits on. Marks the view as the widget background for smooth launcher
 * transitions, paints it with the theme's widget colour, rounds it on Android 12+, and makes the
 * whole widget open [tab]. The click target is the full widget, which is well above the 48dp
 * minimum.
 */
@Composable
internal fun WidgetFrame(
  tab: WidgetTab,
  description: String? = null,
  background: ColorProvider? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val context = LocalContext.current
  val intent = widgetIntent(context, tab)
  Column(
    modifier =
      GlanceModifier.fillMaxSize()
        .appWidgetBackground()
        .background(background ?: GlanceTheme.colors.widgetBackground)
        .roundedOnS()
        .clickable(actionStartActivity(intent))
        .then(
          if (description == null) {
            GlanceModifier
          } else {
            GlanceModifier.semantics { contentDescription = description }
          },
        )
        .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
    horizontalAlignment = Alignment.Start,
  ) {
    content()
  }
}

/** The intent a widget tap opens: the app, on the tab the widget is about. */
internal fun widgetIntent(context: Context, tab: WidgetTab): Intent =
  Intent(context, MainActivity::class.java).apply {
    putExtra(EXTRA_WIDGET_TAB, tab.extra)
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
  }

/** Rounds the widget background, which the platform only supports from Android 12. */
internal fun GlanceModifier.roundedOnS(radius: Dp? = null): GlanceModifier =
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
    this
  } else if (radius == null) {
    cornerRadius(android.R.dimen.system_app_widget_background_radius)
  } else {
    cornerRadius(radius)
  }
