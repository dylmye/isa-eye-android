package me.dylmye.isa

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.dylmye.isa.data.AccountOrderRepository
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.GoalRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.notifications.ReminderNotifier
import me.dylmye.isa.notifications.ReminderScheduler
import me.dylmye.isa.widgets.WidgetRefreshScheduler
import me.dylmye.isa.widgets.WidgetThemeReceiver
import me.dylmye.isa.widgets.WidgetThemeScheduler
import me.dylmye.isa.widgets.WidgetUpdater

/** Holds the process-wide database and repositories (manual DI, no framework). */
class IsaEyeApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Register the channel at start-up so it is listed in the system's notification settings, and
    // book the next check even if the last one never ran (an update, a force-stop, a reboot).
    ReminderNotifier.createChannel(this)
    ReminderScheduler.ensureScheduled(this)
    WidgetRefreshScheduler.ensureScheduled(this)
    WidgetThemeScheduler.ensureScheduled(this)
    // The widgets bake theme colours into bitmaps, so a light/dark switch needs a fresh composition.
    // This receiver lives only while the app process does; the manifest cannot receive the implicit
    // configuration broadcast on modern Android.
    ContextCompat.registerReceiver(
      this,
      WidgetThemeReceiver(),
      IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
  }

  /** A scope for work that should outlive an activity, such as refreshing widgets. */
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /**
   * The widgets bake their card and ring colours into bitmaps, so a light/dark switch has to rebuild
   * them rather than only re-render the existing RemoteViews.
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    applicationScope.launch { WidgetUpdater.updateAll(this@IsaEyeApplication) }
  }

  val database: IsaDatabase by lazy { IsaDatabase.build(this) }

  val accountRepository: AccountRepository by lazy { AccountRepository(database) }

  val accountOrderRepository: AccountOrderRepository by lazy { AccountOrderRepository(database) }

  val goalRepository: GoalRepository by lazy { GoalRepository(database) }

  val autoContributionRepository: AutoContributionRepository by lazy {
    AutoContributionRepository(database)
  }

  val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
