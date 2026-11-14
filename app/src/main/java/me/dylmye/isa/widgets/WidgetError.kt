package me.dylmye.isa.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.StringRes
import me.dylmye.isa.R

/**
 * The last-resort layout when Glance cannot compose a widget. It is built from text and a
 * background colour only, so drawing it allocates no bitmaps, and tapping it opens the app on
 * [tab] just like the composed widget would.
 */
@Suppress("TooGenericExceptionCaught")
internal fun showCompositionError(
  context: Context,
  appWidgetId: Int,
  tab: WidgetTab,
  @StringRes title: Int,
  throwable: Throwable,
) {
  Log.e(TAG, "Could not compose a widget; showing its text-only fallback.", throwable)
  try {
    val fallback =
      RemoteViews(context.packageName, R.layout.widget_error).apply {
        setTextViewText(R.id.widget_error_title, context.getString(title))
        setOnClickPendingIntent(R.id.widget_error_root, widgetPendingIntent(context, tab))
      }
    AppWidgetManager.getInstance(context)?.updateAppWidget(appWidgetId, fallback)
  } catch (error: RuntimeException) {
    Log.w(TAG, "Could not show the widget error fallback.", error)
  }
}

/** Tapping the fallback opens the same tab the composed widget would have. */
private fun widgetPendingIntent(context: Context, tab: WidgetTab): PendingIntent =
  PendingIntent.getActivity(
    context,
    tab.ordinal,
    widgetIntent(context, tab),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
  )

private const val TAG = "WidgetError"
