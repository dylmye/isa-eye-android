package me.dylmye.isa.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Accounts a free Google Play install may hold before it needs the `full_access` purchase. */
const val FULL_ACCESS_ACCOUNT_LIMIT = 4

/**
 * The account limit and purchase state for a distribution channel.
 *
 * The F-Droid implementation has no limit and never launches a purchase. Keeping this boundary
 * free of Billing types is what lets the shared UI and ViewModels compile for both flavours.
 */
interface AccountLimitPolicy {
  /** `null` when this build has no account limit; otherwise the free-tier limit. */
  val freeAccountLimit: Int?

  /** Whether adding accounts is currently unlimited. */
  val isUnlimited: StateFlow<Boolean>

  /** Whether a restore query is in flight. */
  val isRestoring: StateFlow<Boolean>

  /** Refreshes entitlement from the store, when this channel has one. */
  suspend fun refresh()

  /** Queries the store for a previous purchase and updates [isUnlimited]. */
  suspend fun restore(): RestoreResult

  /** Starts the store purchase flow and suspends until it finishes. */
  suspend fun purchase(activity: Activity): PurchaseResult
}

sealed interface RestoreResult {
  data object Restored : RestoreResult

  data object NotFound : RestoreResult

  data object Unavailable : RestoreResult

  data object Failed : RestoreResult
}

sealed interface PurchaseResult {
  data object Purchased : PurchaseResult

  data object AlreadyOwned : PurchaseResult

  data object Cancelled : PurchaseResult

  data object Pending : PurchaseResult

  data object Unavailable : PurchaseResult

  data class Failed(val message: String? = null) : PurchaseResult
}
