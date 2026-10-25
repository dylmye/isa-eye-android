package me.dylmye.isa.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small synchronous key-value store backed by SharedPreferences.
 *
 * Kept out of Room on purpose: the launch tab must be chosen before the first frame, and a Room
 * read is asynchronous, so it would either flash the wrong tab or block the main thread.
 */
class UserPreferences(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Whether the app has been opened before. It only gates the launch tab: Help introduces the app
   * on the first launch, and every later launch opens Insights whatever accounts exist by then.
   */
  var hasLaunched: Boolean
    get() = prefs.getBoolean(KEY_HAS_LAUNCHED, prefs.getBoolean(KEY_LEGACY_HAS_ACCOUNT, false))
    set(value) = prefs.edit { putBoolean(KEY_HAS_LAUNCHED, value) }

  private val _currentRulesetId = MutableStateFlow(prefs.getString(KEY_CURRENT_RULESET, null))

  /** The ruleset the accounts view is scoped to, or `null` to follow the latest one. */
  val currentRulesetId: StateFlow<String?> = _currentRulesetId.asStateFlow()

  /** Selects a ruleset, or clears the selection when [id] is `null` (follow the latest). */
  fun setCurrentRulesetId(id: String?) {
    prefs.edit { putString(KEY_CURRENT_RULESET, id) }
    _currentRulesetId.value = id
  }

  /**
   * The SAF tree URI the user chose for backup files. Null until the first export or import, which
   * is when the app asks the user to choose a folder.
   */
  var backupFolderUri: String?
    get() = prefs.getString(KEY_BACKUP_FOLDER_URI, null)
    set(value) =
      prefs.edit {
        if (value ==
          null
        ) {
          remove(KEY_BACKUP_FOLDER_URI)
        } else {
          putString(KEY_BACKUP_FOLDER_URI, value)
        }
      }

  private companion object {
    const val PREFS_NAME = "isa_eye_preferences"
    const val KEY_HAS_LAUNCHED = "has_launched"

    /**
     * Written by earlier builds to mean "an account exists". Reading it as well keeps an existing
     * install from being sent back to Help once.
     */
    const val KEY_LEGACY_HAS_ACCOUNT = "has_account"
    const val KEY_CURRENT_RULESET = "current_ruleset"
    const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
  }
}
