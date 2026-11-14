package me.dylmye.isa.widgets

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Rebuilds the widgets when the night-mode or Material You setting changes. */
class WidgetThemeJobService : JobService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var job: Job? = null

  override fun onStartJob(params: JobParameters?): Boolean {
    // The system may start the job again before a stopped run has unwound; keep only the latest.
    job?.cancel()
    job =
      scope.launch {
        try {
          WidgetUpdater.updateAll(this@WidgetThemeJobService)
        } finally {
          // Finish on the main thread: the job callbacks run there, so a stop cannot interleave
          // between the liveness check and the calls below. A cancelled run skips the block and is
          // rescheduled by the system, so only a completed one arms the next theme change and
          // reports back.
          withContext(Dispatchers.Main.immediate) {
            if (isActive) {
              WidgetThemeScheduler.ensureScheduled(this@WidgetThemeJobService)
              jobFinished(params, false)
            }
          }
        }
      }
    return true
  }

  override fun onStopJob(params: JobParameters?): Boolean {
    job?.cancel()
    job = null
    // Ask the system to run the job again: the theme change it was rebuilding for still stands.
    return true
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
