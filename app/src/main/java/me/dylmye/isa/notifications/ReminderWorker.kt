package me.dylmye.isa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.db.IsaDatabase

/** The daily check behind every reminder, run at 09:00 whether or not the app is open. */
class ReminderWorker(appContext: Context, params: WorkerParameters) :
  CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    // Book tomorrow before anything else, so a failure below cannot end the daily series.
    ReminderScheduler.ensureScheduled(applicationContext)
    val application = applicationContext as? IsaEyeApplication ?: return Result.success()
    val store = ReminderStore(applicationContext)
    val due =
      planReminders(
        snapshot = loadSnapshot(application.database),
        nowMillis = System.currentTimeMillis(),
        sentKeys = store.sentKeys(),
      )
    store.markSent(ReminderNotifier.post(applicationContext, due).map { it.key })
    return Result.success()
  }
}

/** Reads everything the planner needs in one pass, on the worker's thread. */
private suspend fun loadSnapshot(database: IsaDatabase): ReminderSnapshot = ReminderSnapshot(
  accounts = database.productDao().getSummaries(),
  balances = database.annualBalanceDao().getAll(),
  rulesets = database.rulesetDao().getAll(),
  exceptions = database.rulesetExceptionDao().getAll(),
  goals = database.goalDao().getAll(),
  schedules = database.autoContributionDao().getAll(),
)
