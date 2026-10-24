package me.dylmye.isa.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns an action that asks for notification permission. The app has no notification settings of
 * its own, so it asks at the two moments notifications start to be useful: saving a goal or an
 * auto-contribution. It is a no-op once granted, and on versions before Android 13, where posting
 * needs no permission.
 */
@Composable
internal fun rememberNotificationPermissionRequest(): () -> Unit {
  val context = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      // The answer needs no handling here: every post checks the permission again.
    }
  return {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
