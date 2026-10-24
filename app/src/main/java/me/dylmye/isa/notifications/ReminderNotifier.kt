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
    if (lines.isNotEmpty()) {
      builder.setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
    }
    return builder.build()
  }
}
