package me.dylmye.isa.widgets

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Rebuilds the widgets when the night-mode or Material You setting changes. */
class WidgetThemeJobService : JobService() {
  override fun onStartJob(params: JobParameters?): Boolean {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        WidgetUpdater.updateAll(this@WidgetThemeJobService)
      } finally {
        // A content-trigger job is one-shot; arm it again for the next theme change.
        WidgetThemeScheduler.ensureScheduled(this@WidgetThemeJobService)
        jobFinished(params, false)
      }
    }
    return true
  }

  override fun onStopJob(params: JobParameters?): Boolean = true
}
