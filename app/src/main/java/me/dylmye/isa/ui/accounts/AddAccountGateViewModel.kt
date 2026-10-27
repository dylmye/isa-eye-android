package me.dylmye.isa.ui.accounts

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.billing.AccountLimitPolicy
import me.dylmye.isa.billing.PurchaseResult
import me.dylmye.isa.data.AccountLimitRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decides what the add-account button does. Below the free limit it opens the form; at the limit it
 * starts the store purchase flow, which remains the only way past the gate.
 */
class AddAccountGateViewModel(
  private val accountLimitRepository: AccountLimitRepository,
  private val policy: AccountLimitPolicy,
) : ViewModel() {
  private val eventChannel = Channel<AddAccountGateEvent>(Channel.BUFFERED)
  private val inProgress = AtomicBoolean(false)

  val events: Flow<AddAccountGateEvent> = eventChannel.receiveAsFlow()

  fun onAddClicked(activity: Activity) {
    if (!inProgress.compareAndSet(false, true)) return
    viewModelScope.launch {
      try {
        val limit = policy.freeAccountLimit
        if (addAccountGateAction(
            freeAccountLimit = limit,
            isUnlimited = policy.isUnlimited.value,
            accountCount = accountLimitRepository.countAccounts(),
          ) == AddAccountGateAction.OpenAddAccount
        ) {
          eventChannel.send(AddAccountGateEvent.OpenAddAccount)
          return@launch
        }

        // The cached flag may be stale after a reinstall or on a second device.
        policy.refresh()
        if (policy.isUnlimited.value) {
          eventChannel.send(AddAccountGateEvent.OpenAddAccount)
          return@launch
        }

        when (val result = policy.purchase(activity)) {
          PurchaseResult.Purchased,
          PurchaseResult.AlreadyOwned,
          -> {
            eventChannel.send(AddAccountGateEvent.OpenAddAccount)
          }

          PurchaseResult.Cancelled -> {
            Unit
          }

          PurchaseResult.Pending -> {
            eventChannel.send(AddAccountGateEvent.ShowMessage(PENDING_PURCHASE_MESSAGE))
          }

          PurchaseResult.Unavailable -> {
            eventChannel.send(AddAccountGateEvent.ShowMessage(BILLING_UNAVAILABLE_MESSAGE))
          }

          is PurchaseResult.Failed -> {
            eventChannel.send(
              AddAccountGateEvent.ShowMessage(
                result.message ?: "The purchase could not be completed.",
              ),
            )
          }
        }
      } finally {
        inProgress.set(false)
      }
    }
  }

  companion object {
    private const val PENDING_PURCHASE_MESSAGE =
      "Purchase pending. Unlimited accounts will unlock once Play confirms the payment."
    private const val BILLING_UNAVAILABLE_MESSAGE =
      "Google Play billing isn't available on this device."

    val Factory = viewModelFactory {
      initializer {
        val application = this[APPLICATION_KEY] as IsaEyeApplication
        AddAccountGateViewModel(
          application.accountLimitRepository,
          application.accountLimitPolicy,
        )
      }
    }
  }
}

sealed interface AddAccountGateEvent {
  data object OpenAddAccount : AddAccountGateEvent

  data class ShowMessage(val message: String) : AddAccountGateEvent
}

internal enum class AddAccountGateAction {
  OpenAddAccount,
  Purchase,
}

/**
 * Pure gate decision, kept outside the ViewModel so it can be tested without an Activity or a
 * Billing client.
 */
internal fun addAccountGateAction(
  freeAccountLimit: Int?,
  isUnlimited: Boolean,
  accountCount: Int,
): AddAccountGateAction =
  if (freeAccountLimit == null || isUnlimited || accountCount < freeAccountLimit) {
    AddAccountGateAction.OpenAddAccount
  } else {
    AddAccountGateAction.Purchase
  }
