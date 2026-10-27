package me.dylmye.isa

import me.dylmye.isa.billing.AccountLimitPolicy
import me.dylmye.isa.billing.PlayAccountLimitPolicy

/** Google Play build: enforces the free account limit and sells `full_access`. */
class GpeIsaEyeApplication : IsaEyeApplication() {
  override fun createAccountLimitPolicy(): AccountLimitPolicy =
    PlayAccountLimitPolicy(this, userPreferences)
}
