package me.dylmye.isa.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.dylmye.isa.time.calendar
import me.dylmye.isa.time.isoDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Refreshes the date-dependent widgets just after midnight, so "days left" is right even if the app
 * is never opened. WorkManager runs it through the platform's JobScheduler, so it survives reboots
 * and needs no Play Services.
 */
class WidgetRefreshWorker(appContext: Context, params: WorkerParameters) :
  CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    // Book tomorrow before updating, so a failure below cannot end the daily series.
    WidgetRefreshScheduler.ensureScheduled(applicationContext)
    WidgetUpdater.updateAll(applicationContext)
    return Result.success()
  }
}

internal object WidgetRefreshScheduler {
  /**
   * Books the next just-after-midnight widget refresh. The run's date is part of the work name, so a
   * second call on the same day keeps the one already booked.
   */
  fun ensureScheduled(context: Context, nowMillis: Long = System.currentTimeMillis()) {
    val runAt = nextWidgetRefreshRun(nowMillis)
    val request =
      OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
        .setInitialDelay((runAt - nowMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context)
      .enqueueUniqueWork("widget-refresh-${isoDate(runAt)}", ExistingWorkPolicy.KEEP, request)
  }
}

/**
 * Five past midnight in the UK zone: today's when it is still to come, otherwise tomorrow's. The
 * small delay keeps the date change safely on the new day.
 */
internal fun nextWidgetRefreshRun(nowMillis: Long): Long {
  val run =
    calendar(nowMillis).apply {
      set(Calendar.HOUR_OF_DAY, WIDGET_REFRESH_HOUR)
      set(Calendar.MINUTE, WIDGET_REFRESH_MINUTE)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  if (run.timeInMillis <= nowMillis) run.add(Calendar.DAY_OF_MONTH, 1)
  return run.timeInMillis
}

private const val WIDGET_REFRESH_HOUR = 0
private const val WIDGET_REFRESH_MINUTE = 5
