package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/** One product type's own allowance for a tax year, e.g. the Lifetime ISA's £4,000. */
data class RulesetExceptionUiState(
  val name: String,
  val allowancePence: Long,
  val includedInShared: Boolean,
)

/**
 * What the tax year's notes surface shows: the year's changelog, its shared allowance and any
 * per-type exceptions, mirroring the web's ruleset description dialog.
 */
data class RulesetNotesUiState(
  val ruleset: String? = null,
  val notes: String? = null,
  val sharedAllowancePence: Long? = null,
  val exceptions: List<RulesetExceptionUiState> = emptyList(),
)

/**
 * Resolves the notes for [rulesetId] from the reference [rulesets], their per-type [exceptions] and
 * the [productTypes] that name them. An unknown year yields an empty state, and an exception whose
 * product type is missing is dropped rather than shown unnamed.
 */
internal fun resolveRulesetNotes(
  rulesetId: String?,
  rulesets: List<RulesetEntity>,
  exceptions: List<RulesetExceptionEntity>,
  productTypes: List<ProductTypeEntity>,
): RulesetNotesUiState {
  val ruleset = rulesets.firstOrNull { it.id == rulesetId } ?: return RulesetNotesUiState()
  val names = productTypes.associate { it.id to it.name }
  val rows =
    exceptions
      .filter { it.rulesetId == ruleset.id }
      .mapNotNull { exception ->
        val name = names[exception.productTypeId] ?: return@mapNotNull null
        RulesetExceptionUiState(
          name = name,
          allowancePence = exception.allowancePence,
          includedInShared = exception.includedInShared,
        )
      }
  return RulesetNotesUiState(
    ruleset = ruleset.id,
    notes = ruleset.notes,
    sharedAllowancePence = ruleset.sharedAllowancePence,
    exceptions = rows,
  )
}
