package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/**
 * One Junior ISA account's use of its own allowance for a tax year. The Junior allowance is per
 * child and sits outside the shared one, so each account is measured against the limit on its own.
 */
data class JuniorAllowanceUiState(
  val productId: String,
  val accountName: String,
  val typeName: String,
  val allowancePence: Long,
  val usedPence: Long,
) {
  /** What this account can still take this year, or zero once it is over the limit. */
  val remainingPence: Long
    get() = (allowancePence - usedPence).coerceAtLeast(0L)

  val isUsedUp: Boolean
    get() = usedPence >= allowancePence

  /** How full the allowance is, capped at one so an overshoot still draws as complete. */
  val fraction: Float
    get() = if (allowancePence <= 0L) {
      0f
    } else {
      (usedPence.toFloat() / allowancePence).coerceIn(0f, 1f)
    }

  /** How much of the allowance is left, for a battery that drains as the child saves. */
  val remainingFraction: Float
    get() = 1f - fraction
}

/**
 * The allowance standing for a tax year: what the accounts hero leads with, and the Junior accounts
 * that sit outside it. The shared pot is the usual headline; a year holding only Junior ISAs leads
 * with the Junior allowance instead, because the shared one is not the user's to spend.
 */
internal data class AllowanceStanding(
  val headlinePence: Long?,
  val headlineIsJunior: Boolean,
  val juniorAllowances: List<JuniorAllowanceUiState>,
  /** The year's shared allowance, before any contributions. */
  val sharedAllowancePence: Long? = null,
  /** What the shared allowance has left, even in a year that only holds Junior accounts. */
  val sharedRemainingPence: Long? = null,
  /** Whether any account open in the year draws on the shared allowance. */
  val hasSharedAccounts: Boolean = false,
)

/**
 * Reads the year's standing from [accounts], their [balances], the [rulesets] and the per-type
 * [exceptions]. Product types whose exception is not part of the shared allowance (the Junior ISA)
 * are kept out of the shared figure and measured against their own limit instead.
 */
internal fun resolveAllowanceStanding(
  rulesetId: String?,
  accounts: List<AccountSummary>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  exceptions: List<RulesetExceptionEntity>,
  productTypes: List<ProductTypeEntity> = emptyList(),
): AllowanceStanding {
  val separate = separateExceptions(rulesetId, exceptions)
  val shared = remainingSharedAllowancePence(rulesetId, accounts, balances, rulesets, exceptions)
  val juniors = resolveJuniorAllowances(rulesetId, accounts, balances, exceptions, productTypes)
  val hasSharedAccounts =
    accounts.any { account ->
      isOpenDuringRuleset(rulesetId.orEmpty(), account.startTaxYear, account.endTaxYear) &&
        account.productTypeId !in separate
    }
  val juniorOnly = juniors.isNotEmpty() && !hasSharedAccounts
  return AllowanceStanding(
    headlinePence = if (juniorOnly) juniors.sumOf { it.remainingPence } else shared,
    headlineIsJunior = juniorOnly,
    juniorAllowances = juniors,
    sharedAllowancePence = rulesets.firstOrNull { it.id == rulesetId }?.sharedAllowancePence,
    sharedRemainingPence = shared,
    hasSharedAccounts = hasSharedAccounts,
  )
}

/**
 * The shared allowance left for a year: that year's allowance minus every contribution recorded
 * against a product type that shares it. Junior ISA contributions are left out because they have a
 * pot of their own. Null when the year is unknown.
 */
internal fun remainingSharedAllowancePence(
  rulesetId: String?,
  accounts: List<AccountSummary>,
  balances: List<AnnualBalanceEntity>,
  rulesets: List<RulesetEntity>,
  exceptions: List<RulesetExceptionEntity>,
): Long? {
  val allowance = rulesets.firstOrNull { it.id == rulesetId }?.sharedAllowancePence ?: return null
  val separate = separateExceptions(rulesetId, exceptions).keys
  val types = accounts.associate { it.productId to it.productTypeId }
  val contributed =
    balances
      .filter { it.rulesetId == rulesetId && types[it.productId] !in separate }
      .sumOf { it.deductedFromAllowancePence }
  return allowance - contributed
}

/**
 * Each Junior ISA account open in [rulesetId], with its own use of that year's Junior allowance.
 * Empty when the year has no separate allowance or the user holds no account of that type.
 */
internal fun resolveJuniorAllowances(
  rulesetId: String?,
  accounts: List<AccountSummary>,
  balances: List<AnnualBalanceEntity>,
  exceptions: List<RulesetExceptionEntity>,
  productTypes: List<ProductTypeEntity>,
): List<JuniorAllowanceUiState> {
  val year = rulesetId ?: return emptyList()
  val separate = separateExceptions(year, exceptions)
  return if (separate.isEmpty()) {
    emptyList()
  } else {
    val names = productTypes.associate { it.id to it.name }
    val used =
      balances.filter { it.rulesetId == year }.associate {
        it.productId to it.deductedFromAllowancePence
      }
    accounts
      .filter { isOpenDuringRuleset(year, it.startTaxYear, it.endTaxYear) }
      .mapNotNull { account ->
        val allowance = separate[account.productTypeId] ?: return@mapNotNull null
        JuniorAllowanceUiState(
          productId = account.productId,
          accountName = account.friendlyName,
          typeName = names[account.productTypeId].orEmpty(),
          allowancePence = allowance,
          usedPence = used[account.productId] ?: 0L,
        )
      }
  }
}

/**
 * The product types with their own pot for [rulesetId], mapped to that pot's allowance. These are
 * the exceptions the data marks as excluded from the shared allowance; today that is the Junior ISA.
 */
private fun separateExceptions(
  rulesetId: String?,
  exceptions: List<RulesetExceptionEntity>,
): Map<String, Long> = exceptions
  .filter { it.rulesetId == rulesetId && !it.includedInShared }
  .associate { it.productTypeId to it.allowancePence }
