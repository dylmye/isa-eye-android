package me.dylmye.isa.notifications

import android.content.Intent

/** Where tapping a reminder lands. */
internal sealed interface ReminderTarget {
  /** The accounts list, for when more than one contribution is due. */
  data object Accounts : ReminderTarget

  /** One account's detail page, for when its contribution is the only one due. */
  data class AccountDetails(val productId: String) : ReminderTarget

  /** The insights tab, where the year's allowance and goal live. */
  data object Insights : ReminderTarget
}

/**
 * What a reminder's Undo action takes back: the month starting [monthDate] in [rulesetId], for the
 * accounts whose credit for that month is in the books.
 */
internal data class ReminderUndo(
  val rulesetId: String,
  val monthDate: String,
  val productIds: List<String>,
)

/**
 * A reminder that is due today and has not been posted. [key] records that it has been sent, so a
 * reminder about a month or a tax year never fires twice; [id] is the system notification id, so a
 * later reminder of the same type replaces the one it supersedes.
 */
internal data class Reminder(
  val id: Int,
  val key: String,
  val title: String,
  val text: String,
  val lines: List<String> = emptyList(),
  val target: ReminderTarget,
  val undo: ReminderUndo? = null,
)

/** The extra on a notification's intent that says which screen opening it should land on. */
internal const val EXTRA_REMINDER_TARGET = "me.dylmye.isa.extra.REMINDER_TARGET"

/** The account a reminder is about, when its target is [ReminderTarget.AccountDetails]. */
internal const val EXTRA_REMINDER_PRODUCT_ID = "me.dylmye.isa.extra.REMINDER_PRODUCT_ID"

/** Writes [target] onto a notification's intent. */
internal fun Intent.putReminderTarget(target: ReminderTarget) {
  when (target) {
    ReminderTarget.Accounts -> {
      putExtra(EXTRA_REMINDER_TARGET, TARGET_ACCOUNTS)
    }

    is ReminderTarget.AccountDetails -> {
      putExtra(EXTRA_REMINDER_TARGET, TARGET_ACCOUNT_DETAILS)
      putExtra(EXTRA_REMINDER_PRODUCT_ID, target.productId)
    }

    ReminderTarget.Insights -> {
      putExtra(EXTRA_REMINDER_TARGET, TARGET_INSIGHTS)
    }
  }
}

/** The target [intent] was opened with, or null when it was not opened from a reminder. */
internal fun reminderTarget(intent: Intent?): ReminderTarget? {
  val target = intent?.getStringExtra(EXTRA_REMINDER_TARGET)
  val productId = intent?.getStringExtra(EXTRA_REMINDER_PRODUCT_ID)
  return when (target) {
    TARGET_ACCOUNTS -> ReminderTarget.Accounts
    TARGET_ACCOUNT_DETAILS -> productId?.let(ReminderTarget::AccountDetails)
    TARGET_INSIGHTS -> ReminderTarget.Insights
    else -> null
  }
}

private const val TARGET_ACCOUNTS = "accounts"
private const val TARGET_ACCOUNT_DETAILS = "accountDetails"
private const val TARGET_INSIGHTS = "insights"
