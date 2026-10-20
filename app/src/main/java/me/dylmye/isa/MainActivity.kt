package me.dylmye.isa

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.dylmye.isa.theme.ISAEyeTheme

class MainActivity : ComponentActivity() {
  /**
   * Requests to open the add-account flow, raised by the Meta+N shortcut. Navigation belongs to the
   * Compose tree, so the shortcut is forwarded there rather than acted on here.
   */
  private val addAccountRequests = Channel<Unit>(Channel.CONFLATED)

  /** The add-account shortcut's requests, for the Compose tree to collect. */
  val addAccountShortcut: Flow<Unit> = addAccountRequests.receiveAsFlow()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    // Read synchronously so the right tab shows on the first frame, with no database round trip.
    val hasLaunched = (application as IsaEyeApplication).userPreferences.hasLaunched
    setContent {
      ISAEyeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          MainNavigation(
            startTab = launchTab(hasLaunched),
            addAccountShortcut = addAccountShortcut,
          )
        }
      }
    }
  }

  /**
   * Catches up any auto-contribution credits that came due while the app was not running. It runs on
   * every foregrounding rather than on a timer: the app has to be running to write them, and this is
   * the one moment it certainly is.
   */
  override fun onStart() {
    super.onStart()
    lifecycleScope.launch(Dispatchers.IO) {
      (application as IsaEyeApplication)
        .autoContributionRepository
        .applyDue(System.currentTimeMillis())
    }
  }

  /**
   * Records that the app has been used, so Help only opens on the first launch. A configuration
   * change recreates the activity without the user leaving it, so that does not count as a launch.
   */
  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      (application as IsaEyeApplication).userPreferences.hasLaunched = true
    }
  }

  /**
   * Consumes the shortcut's key-down so nothing else acts on it. The action fires on key-up, because
   * a key held down repeats key-down and would otherwise open the flow over and over.
   */
  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
    if (isAddAccountShortcut(keyCode, event)) {
      true
    } else {
      super.onKeyDown(keyCode, event)
    }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (isAddAccountShortcut(keyCode, event)) {
      addAccountRequests.trySend(Unit)
      return true
    }
    return super.onKeyUp(keyCode, event)
  }

  /**
   * Publishes the shortcut to the system's Keyboard Shortcuts Helper, which Meta+/ opens. Without
   * this the shortcut works but is undiscoverable.
   */
  override fun onProvideKeyboardShortcuts(
    data: MutableList<KeyboardShortcutGroup>?,
    menu: Menu?,
    deviceId: Int,
  ) {
    data?.add(
      KeyboardShortcutGroup(
        getString(R.string.app_name),
        listOf(
          KeyboardShortcutInfo(
            getString(R.string.shortcut_add_account),
            KeyEvent.KEYCODE_N,
            KeyEvent.META_META_ON,
          ),
        ),
      ),
    )
    super.onProvideKeyboardShortcuts(data, menu, deviceId)
  }

  /**
   * Meta is the Command key on macOS keyboards, the Windows key on Windows ones and the Search key
   * on ChromeOS ones, so this is the desktop-native chord on each.
   */
  private fun isAddAccountShortcut(keyCode: Int, event: KeyEvent): Boolean =
    keyCode == KeyEvent.KEYCODE_N && event.isMetaPressed
}
