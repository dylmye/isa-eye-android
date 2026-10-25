package me.dylmye.isa.widgets

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Watches the secure settings that carry the night-mode and Material You choices. A content-trigger
 * job is used because the configuration broadcast is not delivered to a cached app, and the widget
 * bitmaps have to be rebuilt rather than merely re-rendered.
 */
internal object WidgetThemeScheduler {
  private const val JOB_ID = 4271

  fun ensureScheduled(context: Context) {
    val component = ComponentName(context, WidgetThemeJobService::class.java)
    val nightMode = Settings.Secure.getUriFor(NIGHT_MODE)
    val overlays = Settings.Secure.getUriFor(THEME_OVERLAYS)
    val job =
      JobInfo.Builder(JOB_ID, component)
        .addTriggerContentUri(JobInfo.TriggerContentUri(nightMode, 0))
        .addTriggerContentUri(JobInfo.TriggerContentUri(overlays, 0))
        .setTriggerContentUpdateDelay(0L)
        .setTriggerContentMaxDelay(0L)
        .build()
    context.getSystemService(JobScheduler::class.java)?.schedule(job)
  }

  /** Written by the system when light/dark mode changes. */
  private const val NIGHT_MODE = "ui_night_mode"

  /** Written when the wallpaper-derived Material You palette changes. */
  private const val THEME_OVERLAYS = "theme_customization_overlay_packages"
}
