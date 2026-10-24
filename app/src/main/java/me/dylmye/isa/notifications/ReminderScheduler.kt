package me.dylmye.isa.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import me.dylmye.isa.time.calendar
import me.dylmye.isa.time.isoDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Books the daily reminder check for 09:00. WorkManager persists it across reboots and runs it
 * through the platform's JobScheduler, so no Play Services are needed and nothing keeps a timer of
 * its own.
 */
internal object ReminderScheduler {
  /**
   * Books the next 09:00 check. Safe to call from anywhere: the run's date is part of the work
   * name, so a second call for the same day keeps the one already booked instead of queuing another.
   */
  fun ensureScheduled(context: Context, nowMillis: Long = System.currentTimeMillis()) {
    val runAt = nextReminderRun(nowMillis)
    val request =
      OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay((runAt - nowMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context)
      .enqueueUniqueWork("reminders-${isoDate(runAt)}", ExistingWorkPolicy.KEEP, request)
  }
}

/**
 * The next 09:00 in the UK zone: today's when it is still to come, otherwise tomorrow's. The check
 * reschedules itself after every run, so this is always one day ahead.
 */
internal fun nextReminderRun(nowMillis: Long): Long {
  val time = calendar(nowMillis)
  time.set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
  time.set(Calendar.MINUTE, 0)
  time.set(Calendar.SECOND, 0)
  time.set(Calendar.MILLISECOND, 0)
  if (time.timeInMillis <= nowMillis) time.add(Calendar.DAY_OF_MONTH, 1)
  return time.timeInMillis
}

/** The hour reminders go out, in the UK zone the rest of the app already reads dates in. */
private const val REMINDER_HOUR = 9
