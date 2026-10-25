package me.dylmye.isa.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dylmye.isa.IsaEyeApplication

/**
 * Takes the month's contribution back out when the reminder's Undo action is used. It runs from the
 * notification and never opens the app, so the action and a short toast are the whole interaction.
 */
class UndoAutoContributionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val application = context.applicationContext as? IsaEyeApplication ?: return
    val rulesetId = intent.getStringExtra(EXTRA_UNDO_RULESET_ID)
    val monthDate = intent.getStringExtra(EXTRA_UNDO_MONTH_DATE)
    if (rulesetId == null || monthDate == null) return
    val productIds = intent.getStringArrayListExtra(EXTRA_UNDO_PRODUCT_IDS).orEmpty()
    val notificationId = intent.getIntExtra(EXTRA_UNDO_NOTIFICATION_ID, 0)
    // The database work outlives onReceive, so the process is held until it and the toast are done.
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val undone =
          application.autoContributionRepository.undoMonth(
            productIds = productIds,
            rulesetId = rulesetId,
            monthDate = monthDate,
            nowMillis = System.currentTimeMillis(),
          )
        withContext(Dispatchers.Main) {
          // The notification is the reliable confirmation; the toast is a bonus that only survives
          // while the app itself is in front.
          ReminderNotifier.confirmUndo(context, notificationId, undone = undone > 0)
          val message = if (undone > 0) "Contribution undone" else "There was nothing to undo"
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
      } finally {
        pending.finish()
      }
    }
  }
}

/** The tax year the undone accounts belong to. */
internal const val EXTRA_UNDO_RULESET_ID = "me.dylmye.isa.extra.UNDO_RULESET_ID"

/** The first day of the month being taken back, as an ISO date. */
internal const val EXTRA_UNDO_MONTH_DATE = "me.dylmye.isa.extra.UNDO_MONTH_DATE"

/** The accounts whose credit for that month should be taken back out. */
internal const val EXTRA_UNDO_PRODUCT_IDS = "me.dylmye.isa.extra.UNDO_PRODUCT_IDS"

/** The reminder notification to dismiss once the undo has been recorded. */
internal const val EXTRA_UNDO_NOTIFICATION_ID = "me.dylmye.isa.extra.UNDO_NOTIFICATION_ID"
