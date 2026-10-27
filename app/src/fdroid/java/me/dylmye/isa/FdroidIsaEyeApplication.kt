package me.dylmye.isa

import me.dylmye.isa.billing.AccountLimitPolicy
import me.dylmye.isa.billing.UnlimitedAccountLimitPolicy

/** F-Droid build: no Billing client and no account limit. */
class FdroidIsaEyeApplication : IsaEyeApplication() {
  override fun createAccountLimitPolicy(): AccountLimitPolicy = UnlimitedAccountLimitPolicy()
}
