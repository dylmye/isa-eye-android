package me.dylmye.isa.billing

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.dylmye.isa.data.UserPreferences
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Google Play entitlement policy.
 *
 * The store remains the source of truth: the local preference is only an offline cache, refreshed
 * on app start and resume. A successful query with no valid purchase revokes the cache, but never
 * touches the user's accounts.
 */
internal class PlayAccountLimitPolicy(context: Context, private val preferences: UserPreferences) :
  AccountLimitPolicy {
  private val appContext = context.applicationContext
  private val debugBuild =
    (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val verifier = PlayPurchaseVerifier(PLAY_LICENSING_PUBLIC_KEY)
  private val connectionMutex = Mutex()
  private val purchaseInProgress = AtomicBoolean(false)

  private val _isUnlimited = MutableStateFlow(preferences.fullAccessUnlocked)
  override val isUnlimited: StateFlow<Boolean> = _isUnlimited.asStateFlow()

  private val _isRestoring = MutableStateFlow(false)
  override val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

  override val freeAccountLimit: Int = FULL_ACCESS_ACCOUNT_LIMIT

  @Volatile private var pendingPurchase: CancellableContinuation<PurchaseResult>? = null
  private var warnedAboutMissingKey = false

  private val billingClient =
    BillingClient.newBuilder(appContext)
      .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
      .enablePendingPurchases(
        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
      )
      .enableAutoServiceReconnection()
      .build()

  override suspend fun refresh() {
    if (!ensureConnected()) return
    val purchases = queryPurchases() ?: return
    if (!applyPurchases(purchases)) setUnlimited(false)
  }

  override suspend fun restore(): RestoreResult {
    _isRestoring.value = true
    return try {
      if (!ensureConnected()) {
        RestoreResult.Unavailable
      } else {
        val purchases = queryPurchases()
        when {
          purchases == null -> {
            RestoreResult.Failed
          }

          applyPurchases(purchases) -> {
            RestoreResult.Restored
          }

          else -> {
            setUnlimited(false)
            RestoreResult.NotFound
          }
        }
      }
    } finally {
      _isRestoring.value = false
    }
  }

  override suspend fun purchase(activity: Activity): PurchaseResult {
    if (isUnlimited.value) return PurchaseResult.AlreadyOwned
    if (!purchaseInProgress.compareAndSet(false, true)) {
      return PurchaseResult.Failed("A purchase is already in progress.")
    }
    return try {
      if (!ensureConnected()) {
        PurchaseResult.Unavailable
      } else {
        // A previous install may already own the product, even when the local cache was lost.
        val owned = queryPurchases()
        if (owned != null && applyPurchases(owned)) {
          PurchaseResult.AlreadyOwned
        } else {
          val product = queryProduct() ?: return PurchaseResult.Unavailable
          launchPurchase(activity, product)
        }
      }
    } finally {
      purchaseInProgress.set(false)
    }
  }

  private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
    val continuation = pendingPurchase
    pendingPurchase = null
    scope.launch {
      val outcome =
        when (result.responseCode) {
          BillingClient.BillingResponseCode.OK -> {
            val verified = purchases?.let { applyPurchases(it) } == true
            when {
              verified -> PurchaseResult.Purchased
              purchases.orEmpty().any { it.isPendingFullAccess() } -> PurchaseResult.Pending
              else -> PurchaseResult.Failed("The purchase could not be verified.")
            }
          }

          BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
            val owned = queryPurchases()
            if (owned != null && applyPurchases(owned)) {
              PurchaseResult.AlreadyOwned
            } else {
              PurchaseResult.Failed("The purchase could not be restored.")
            }
          }

          BillingClient.BillingResponseCode.USER_CANCELED -> {
            PurchaseResult.Cancelled
          }

          BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
          BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
          BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
          BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
          -> {
            PurchaseResult.Unavailable
          }

          else -> {
            PurchaseResult.Failed("Google Play could not complete the purchase.")
          }
        }
      continuation?.resumeIfActive(outcome)
    }
  }

  private suspend fun launchPurchase(
    activity: Activity,
    productDetails: ProductDetails,
  ): PurchaseResult = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
      pendingPurchase = continuation
      continuation.invokeOnCancellation {
        if (pendingPurchase === continuation) pendingPurchase = null
      }
      val productParams =
        BillingFlowParams.ProductDetailsParams.newBuilder()
          .setProductDetails(productDetails)
          .apply {
            productDetails.oneTimePurchaseOfferDetailsList
              ?.firstOrNull()
              ?.offerToken
              ?.let(::setOfferToken)
          }
          .build()
      val params =
        BillingFlowParams.newBuilder()
          .setProductDetailsParamsList(listOf(productParams))
          .build()
      val launchResult = billingClient.launchBillingFlow(activity, params)
      if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
        pendingPurchase = null
        continuation.resumeIfActive(launchResult.toPurchaseResult())
      }
    }
  }

  private suspend fun queryProduct(): ProductDetails? =
    suspendCancellableCoroutine { continuation ->
      val params =
        QueryProductDetailsParams.newBuilder()
          .setProductList(
            listOf(
              QueryProductDetailsParams.Product.newBuilder()
                .setProductId(FULL_ACCESS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            ),
          )
          .build()
      billingClient.queryProductDetailsAsync(params) { result, queryResult ->
        continuation.resumeIfActive(
          if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            queryResult.productDetailsList.firstOrNull()
          } else {
            null
          },
        )
      }
    }

  private suspend fun queryPurchases(): List<Purchase>? =
    suspendCancellableCoroutine { continuation ->
      val params =
        QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.INAPP)
          .build()
      billingClient.queryPurchasesAsync(params) { result, purchases ->
        continuation.resumeIfActive(
          if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else null,
        )
      }
    }

  /** Verifies and acknowledges every valid `full_access` purchase, then caches the entitlement. */
  private suspend fun applyPurchases(purchases: List<Purchase>): Boolean {
    val purchase =
      purchases.firstOrNull { purchase ->
        FULL_ACCESS_PRODUCT_ID in purchase.products &&
          purchase.purchaseState == Purchase.PurchaseState.PURCHASED
      } ?: return false
    if (!isVerified(purchase)) {
      Log.w(TAG, "Ignoring a full_access purchase with an invalid signature.")
      return false
    }
    if (!purchase.isAcknowledged) {
      val acknowledged = acknowledge(purchase)
      if (!acknowledged) {
        // The purchase is still valid and the user should not be locked out; retry on the next
        // query, and Play will keep the purchase available until it is acknowledged.
        Log.w(TAG, "Could not acknowledge full_access; retrying on the next entitlement refresh.")
      }
    }
    setUnlimited(true)
    return true
  }

  private suspend fun acknowledge(purchase: Purchase): Boolean =
    suspendCancellableCoroutine { continuation ->
      val params =
        AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken)
          .build()
      billingClient.acknowledgePurchase(params) { result ->
        continuation.resumeIfActive(result.responseCode == BillingClient.BillingResponseCode.OK)
      }
    }

  private fun isVerified(purchase: Purchase): Boolean {
    if (verifier.verify(purchase)) return true
    if (!verifier.isConfigured) {
      if (debugBuild) {
        if (!warnedAboutMissingKey) {
          Log.w(
            TAG,
            "Play licensing key is not configured; debug builds accept purchases without signature verification.",
          )
          warnedAboutMissingKey = true
        }
        return true
      }
      Log.e(TAG, "Play licensing key is not configured; refusing to verify a purchase.")
    }
    return false
  }

  private fun setUnlimited(value: Boolean) {
    if (_isUnlimited.value != value) _isUnlimited.value = value
    if (preferences.fullAccessUnlocked != value) preferences.fullAccessUnlocked = value
  }

  private suspend fun ensureConnected(): Boolean = connectionMutex.withLock {
    if (billingClient.isReady) return@withLock true
    runCatching {
      suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(
          object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
              continuation.resumeIfActive(
                result.responseCode == BillingClient.BillingResponseCode.OK,
              )
            }

            override fun onBillingServiceDisconnected() = Unit
          },
        )
      }
    }
      .getOrDefault(false)
  }

  private fun BillingResult.toPurchaseResult(): PurchaseResult = when (responseCode) {
    BillingClient.BillingResponseCode.OK -> PurchaseResult.Purchased

    BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled

    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseResult.AlreadyOwned

    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
    -> PurchaseResult.Unavailable

    else -> PurchaseResult.Failed("Google Play could not start the purchase.")
  }

  private fun Purchase.isPendingFullAccess(): Boolean =
    FULL_ACCESS_PRODUCT_ID in products && purchaseState == Purchase.PurchaseState.PENDING

  private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
  }

  private companion object {
    const val TAG = "PlayAccountLimitPolicy"
  }
}
