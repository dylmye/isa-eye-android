package me.dylmye.isa.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds the widgets when the system configuration changes. Their card and ring colours are baked
 * into bitmaps, so a light/dark switch needs a new composition rather than just a re-render of the
 * existing RemoteViews.
 */
class WidgetThemeReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_CONFIGURATION_CHANGED) return
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        WidgetUpdater.updateAll(context)
      } finally {
        pending.finish()
      }
    }
  }
}
