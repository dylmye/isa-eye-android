package me.dylmye.isa.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.intSetOf
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlin.reflect.KClass

/**
 * Publishes generated widget previews for the launcher's picker on Android 15+. The system rate
 * limits this to roughly two calls an hour, so the app rotates through the four widgets one call at
 * a time instead of pushing them all and being refused. Older devices fall back to the static
 * `previewImage` in each provider's metadata.
 */
internal object WidgetPreviews {
  private val receivers: List<KClass<out GlanceAppWidgetReceiver>> =
    listOf(
      AllowanceWidgetReceiver::class,
      JuniorAllowanceWidgetReceiver::class,
      TaxYearWidgetReceiver::class,
      GoalWidgetReceiver::class,
    )

  /** Publishes the next widget's preview when an hour has passed since the last attempt. */
  suspend fun publishNext(context: Context, nowMillis: Long = System.currentTimeMillis()) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && due(prefs, nowMillis)) {
      publishNextOn35(context, prefs, nowMillis)
    }
  }

  /**
   * The Android 15+ half of [publishNext]. A preview is a nicety, so any failure here is logged and
   * skipped rather than taking down the app as it leaves the foreground.
   */
  @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
  @Suppress("TooGenericExceptionCaught")
  private suspend fun publishNextOn35(
    context: Context,
    prefs: SharedPreferences,
    nowMillis: Long,
  ) {
    val index = prefs.getInt(KEY_NEXT, 0).mod(receivers.size)
    val result =
      try {
        publish(context, receivers[index])
      } catch (error: RuntimeException) {
        Log.w(TAG, "Could not publish the ${receivers[index].simpleName} preview", error)
        null
      }
    if (result == null) return
    // Advance even when rate-limited: retrying the same widget in a loop would keep hitting the
    // limit, while the next hour can try the next one.
    prefs.edit {
      putLong(KEY_LAST_PUBLISHED_AT, nowMillis)
      putInt(KEY_NEXT, (index + 1) % receivers.size)
    }
    if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED) {
      Log.i(TAG, "Widget preview publishing was rate-limited; will try the next widget later.")
    }
  }

  /** True when an hour has passed since the last attempt. */
  private fun due(prefs: SharedPreferences, nowMillis: Long): Boolean =
    nowMillis - prefs.getLong(KEY_LAST_PUBLISHED_AT, 0L) >= PUBLISH_INTERVAL_MILLIS

  @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
  private suspend fun publish(
    context: Context,
    receiver: KClass<out GlanceAppWidgetReceiver>,
  ): Int = GlanceAppWidgetManager(context)
    .setWidgetPreviews(
      receiver = receiver,
      widgetCategories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
    )

  private const val PREFS_NAME = "isa_eye_widget_previews"
  private const val KEY_LAST_PUBLISHED_AT = "last_published_at"
  private const val KEY_NEXT = "next_receiver"
  private const val PUBLISH_INTERVAL_MILLIS = 60 * 60 * 1000L
  private const val TAG = "WidgetPreviews"
}
