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

  /** Whether the user has at least one account, so the app can open on the Insights tab. */
  var hasAccount: Boolean
    get() = prefs.getBoolean(KEY_HAS_ACCOUNT, false)
    set(value) = prefs.edit { putBoolean(KEY_HAS_ACCOUNT, value) }

  private val _currentRulesetId = MutableStateFlow(prefs.getString(KEY_CURRENT_RULESET, null))

  /** The ruleset the accounts view is scoped to, or `null` to follow the latest one. */
  val currentRulesetId: StateFlow<String?> = _currentRulesetId.asStateFlow()

  /** Selects a ruleset, or clears the selection when [id] is `null` (follow the latest). */
  fun setCurrentRulesetId(id: String?) {
    prefs.edit { putString(KEY_CURRENT_RULESET, id) }
    _currentRulesetId.value = id
  }

  private companion object {
    const val PREFS_NAME = "isa_eye_preferences"
    const val KEY_HAS_ACCOUNT = "has_account"
    const val KEY_CURRENT_RULESET = "current_ruleset"
  }
}
