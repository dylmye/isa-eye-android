package me.dylmye.isa.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The F-Droid channel is deliberately unrestricted: there is no Billing client in this build and
 * no account limit, so the shared UI never shows the free-limit card or the purchase gate.
 */
internal class UnlimitedAccountLimitPolicy : AccountLimitPolicy {
  override val freeAccountLimit: Int? = null
  override val isUnlimited: StateFlow<Boolean> = MutableStateFlow(true)
  override val isRestoring: StateFlow<Boolean> = MutableStateFlow(false)

  override suspend fun refresh() = Unit

  override suspend fun restore(): RestoreResult = RestoreResult.Restored

  override suspend fun purchase(activity: Activity): PurchaseResult = PurchaseResult.AlreadyOwned
}
