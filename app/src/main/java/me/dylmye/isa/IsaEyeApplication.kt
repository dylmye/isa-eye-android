package me.dylmye.isa

import android.app.Application
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.GoalRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.notifications.ReminderNotifier
import me.dylmye.isa.notifications.ReminderScheduler

/** Holds the process-wide database and repositories (manual DI, no framework). */
class IsaEyeApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Register the channel at start-up so it is listed in the system's notification settings, and
    // book the next check even if the last one never ran (an update, a force-stop, a reboot).
    ReminderNotifier.createChannel(this)
    ReminderScheduler.ensureScheduled(this)
  }

  val database: IsaDatabase by lazy { IsaDatabase.build(this) }

  val accountRepository: AccountRepository by lazy { AccountRepository(database) }

  val goalRepository: GoalRepository by lazy { GoalRepository(database) }

  val autoContributionRepository: AutoContributionRepository by lazy {
    AutoContributionRepository(database)
  }

  val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
