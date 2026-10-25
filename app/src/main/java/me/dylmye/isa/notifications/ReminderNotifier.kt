package me.dylmye.isa.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.dylmye.isa.MainActivity
import me.dylmye.isa.R

/** Registers the reminders channel and posts due reminders to it. */
internal object ReminderNotifier {
  private const val CHANNEL_ID = "reminders"
  private const val CHANNEL_NAME = "Reminders"
  private const val CHANNEL_DESCRIPTION = "Monthly contribution nudges and tax year updates"

  /** Creates the one channel reminders use. Making it again leaves an existing one alone. */
  fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel =
      NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
    channel.description = CHANNEL_DESCRIPTION
    manager.createNotificationChannel(channel)
  }

  /**
   * Posts [reminders] and returns the ones actually delivered. Nothing is delivered when the user
   * has turned the app or the channel off, and those reminders stay unrecorded, so a later check
   * can still send them while they are current.
   */
  fun post(context: Context, reminders: List<Reminder>): List<Reminder> {
    // Lint reads the permission check inline, so it cannot be pulled into a helper.
    val permitted =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val manager = NotificationManagerCompat.from(context)
    val deliver = permitted && reminders.isNotEmpty() && canPost(context, manager)
    if (deliver) {
      reminders.forEach { manager.notify(it.id, it.notification(context)) }
    }
    return if (deliver) reminders else emptyList()
  }

  /** True when the app is allowed to post and the reminders channel has not been muted. */
  private fun canPost(context: Context, manager: NotificationManagerCompat): Boolean {
    val channelOff =
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        false
      } else {
        context
          .getSystemService(NotificationManager::class.java)
          ?.getNotificationChannel(CHANNEL_ID)
          ?.importance == NotificationManager.IMPORTANCE_NONE
      }
    return manager.areNotificationsEnabled() && !channelOff
  }

  /** The system notification for a reminder, tapping through to the tab it is about. */
  private fun Reminder.notification(context: Context): Notification {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        putReminderTarget(target)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    val contentIntent =
      PendingIntent.getActivity(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val builder =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        // The lock screen gets a generic public version; amounts and account names stay private.
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion(context, "You have a reminder"))
    if (lines.isNotEmpty()) {
      builder.setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
    }
    if (undo != null) {
      builder.addAction(
        NotificationCompat.Action.Builder(
          R.drawable.ic_notification,
          UNDO_LABEL,
          undoIntent(context, undo),
        ).build(),
      )
    }
    return builder.build()
  }

  /**
   * The Undo action's intent: a broadcast, so taking the month back out never opens the app. The
   * request code is the reminder's id, so a newer month's action replaces the older one instead of
   * stacking up.
   */
  private fun undoIntent(context: Context, undo: ReminderUndo): PendingIntent {
    val intent =
      Intent(context, UndoAutoContributionReceiver::class.java).apply {
        putExtra(EXTRA_UNDO_RULESET_ID, undo.rulesetId)
        putExtra(EXTRA_UNDO_MONTH_DATE, undo.monthDate)
        putStringArrayListExtra(EXTRA_UNDO_PRODUCT_IDS, ArrayList(undo.productIds))
        putExtra(EXTRA_UNDO_NOTIFICATION_ID, AUTO_CONTRIBUTION_NOTIFICATION_ID)
      }
    return PendingIntent.getBroadcast(
      context,
      AUTO_CONTRIBUTION_NOTIFICATION_ID,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  /**
   * Replaces the reminder with a short confirmation after its Undo action. A toast cannot do this
   * job: the system suppresses toasts from a background app, and the action runs while the shade is
   * over whatever the user was doing.
   */
  fun confirmUndo(context: Context, notificationId: Int, undone: Boolean) {
    val permitted =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val manager = NotificationManagerCompat.from(context)
    if (!permitted || !canPost(context, manager)) return
    val openApp =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
          putReminderTarget(ReminderTarget.Accounts)
          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val confirmation =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(if (undone) "Contribution undone" else "Nothing to undo")
        .setContentText(
          if (undone) {
            "This month's contribution is no longer counted."
          } else {
            "There was no credit left to take back out."
          },
        )
        .setContentIntent(openApp)
        .setAutoCancel(true)
        // It replaces the reminder it came from, so it must not alert a second time, and it goes
        // away on its own once it has been read.
        .setOnlyAlertOnce(true)
        .setTimeoutAfter(CONFIRMATION_MILLIS)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion(context, "ISA Eye"))
        .build()
    manager.notify(notificationId, confirmation)
  }

  /** The lock-screen-safe version of a notification; it never contains amounts or account names. */
  private fun publicVersion(context: Context, text: String): Notification {
    val openApp =
      PendingIntent.getActivity(
        context,
        PUBLIC_NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle("ISA Eye")
      .setContentText(text)
      .setContentIntent(openApp)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .build()
  }

  /** The monthly reminder's notification id, so the action can replace it afterwards. */
  private const val AUTO_CONTRIBUTION_NOTIFICATION_ID = 1

  private const val PUBLIC_NOTIFICATION_ID = 2

  private const val UNDO_LABEL = "Undo"

  /** How long the undo confirmation stays in the shade. */
  private const val CONFIRMATION_MILLIS = 5_000L
}
