package me.dylmye.isa.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh content to every placed widget. The app calls this when data changes and the worker
 * calls it after adding an automatic contribution, because a widget has no way to observe the
 * database by itself.
 */
internal object WidgetUpdater {
  suspend fun updateAll(context: Context) {
    update(context, AllowanceWidget())
    update(context, JuniorAllowanceWidget())
    update(context, TaxYearWidget())
    update(context, GoalWidget())
  }

  @Suppress("TooGenericExceptionCaught") // A failing widget must never take the app down.
  private suspend fun update(context: Context, widget: GlanceAppWidget) {
    // One provider failing must not stop the other three from refreshing.
    try {
      widget.updateAll(context)
    } catch (error: RuntimeException) {
      Log.w(TAG, "Could not update ${widget.javaClass.simpleName}", error)
    }
  }

  private const val TAG = "WidgetUpdater"
}
