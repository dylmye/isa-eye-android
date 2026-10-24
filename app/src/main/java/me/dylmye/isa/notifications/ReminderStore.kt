package me.dylmye.isa.notifications

import android.content.Context
import androidx.core.content.edit

/**
 * Remembers which reminders have been posted. A key names the month or tax year a reminder is
 * about, so recording it is enough to stop the same one going out again, however often the check
 * runs.
 */
internal class ReminderStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /** The keys of every reminder posted so far. */
  fun sentKeys(): Set<String> = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty().toSet()

  /** Records [keys] as posted. */
  fun markSent(keys: Collection<String>) {
    if (keys.isEmpty()) return
    prefs.edit { putStringSet(KEY_SENT, sentKeys() + keys) }
  }

  private companion object {
    const val PREFS_NAME = "isa_eye_reminders"
    const val KEY_SENT = "sent_keys"
  }
}
