package me.dylmye.isa

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.dylmye.isa.data.AccountOrderRepository
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.GoalRepository
import me.dylmye.isa.data.ProviderIconPolicy
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.notifications.ReminderNotifier
import me.dylmye.isa.notifications.ReminderScheduler
import me.dylmye.isa.widgets.WidgetRefreshScheduler
import me.dylmye.isa.widgets.WidgetThemeReceiver
import me.dylmye.isa.widgets.WidgetThemeScheduler
import me.dylmye.isa.widgets.WidgetUpdater
import okhttp3.OkHttpClient

/** Holds the process-wide database and repositories (manual DI, no framework). */
class IsaEyeApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Coil is the app's only network client. Pinning it to the provider-icon host keeps a tampered
    // database or seed URL from turning image loading into an exfiltration path.
    val imageHttpClient = OkHttpClient.Builder().addInterceptor(ProviderIconPolicy).build()
    SingletonImageLoader.setSafe { context ->
      ImageLoader.Builder(context)
        .components {
          add(OkHttpNetworkFetcherFactory(callFactory = { imageHttpClient }))
        }
        .build()
    }
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

  /**
   * Everything that holds a database instance. Replaced wholesale after an import swaps the
   * database file, so no repository keeps a closed connection alive.
   */
  private class Container(context: Context) {
    val database = IsaDatabase.build(context)
    val accountRepository = AccountRepository(database)
    val accountOrderRepository = AccountOrderRepository(database)
    val goalRepository = GoalRepository(database)
    val autoContributionRepository = AutoContributionRepository(database)
  }

  @Volatile private var container: Container? = null

  private fun container(): Container =
    container ?: synchronized(this) { container ?: Container(this).also { container = it } }

  val database: IsaDatabase
    get() = container().database

  val accountRepository: AccountRepository
    get() = container().accountRepository

  val accountOrderRepository: AccountOrderRepository
    get() = container().accountOrderRepository

  val goalRepository: GoalRepository
    get() = container().goalRepository

  val autoContributionRepository: AutoContributionRepository
    get() = container().autoContributionRepository

  /**
   * Closes the current database so an import can replace its file. The next access to [database]
   * builds a new container, and the caller recreates the activity so ViewModels rebind to it.
   */
  @Synchronized
  fun closeDatabaseForImport() {
    container?.database?.close()
    container = null
  }

  val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
