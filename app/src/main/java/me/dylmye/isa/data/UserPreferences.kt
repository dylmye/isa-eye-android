package me.dylmye.isa.data

import android.content.Context
import androidx.core.content.edit

/**
 * Small synchronous key-value store backed by SharedPreferences.
 *
 * Kept out of Room on purpose: the launch tab must be chosen before the first frame, and a Room
 * read is asynchronous, so it would either flash the wrong tab or block the main thread.
 */
class UserPreferences(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /** Whether the user has at least one account, so the app can open on the Insights tab. */
  var hasAccount: Boolean
    get() = prefs.getBoolean(KEY_HAS_ACCOUNT, false)
    set(value) = prefs.edit { putBoolean(KEY_HAS_ACCOUNT, value) }

  private companion object {
    const val PREFS_NAME = "isa_eye_preferences"
    const val KEY_HAS_ACCOUNT = "has_account"
  }
}
