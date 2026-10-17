package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesetNotesUiStateTest {
  @Test
  fun resolveRulesetNotes_reportsTheYearsChangelogAndSharedAllowance() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2026/2027",
        rulesets = rulesets(),
        exceptions = emptyList(),
        productTypes = productTypes(),
      )

    assertEquals("2026/2027", notes.ruleset)
    assertEquals("Frozen until 2030/2031.", notes.notes)
    assertEquals(2_000_000L, notes.sharedAllowancePence)
    assertTrue(notes.exceptions.isEmpty())
  }

  @Test
  fun resolveRulesetNotes_namesEachExceptionFromItsProductType() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2026/2027",
        rulesets = rulesets(),
        exceptions =
          listOf(
            exception("LIFETIME", "2026/2027", 400_000, includedInShared = true),
            exception("JUNIOR", "2026/2027", 900_000, includedInShared = false),
          ),
        productTypes = productTypes(),
      )

    assertEquals(listOf("Lifetime ISA", "Junior ISA"), notes.exceptions.map { it.name })
    assertEquals(listOf(400_000L, 900_000L), notes.exceptions.map { it.allowancePence })
  }

  @Test
  fun resolveRulesetNotes_flagsAllowancesOutsideTheSharedOne() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2026/2027",
        rulesets = rulesets(),
        exceptions =
          listOf(
            exception("LIFETIME", "2026/2027", 400_000, includedInShared = true),
            exception("JUNIOR", "2026/2027", 900_000, includedInShared = false),
          ),
        productTypes = productTypes(),
      )

    assertTrue(notes.exceptions.first { it.name == "Lifetime ISA" }.includedInShared)
    assertFalse(notes.exceptions.first { it.name == "Junior ISA" }.includedInShared)
  }

  @Test
  fun resolveRulesetNotes_ignoresExceptionsFromOtherYears() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2026/2027",
        rulesets = rulesets(),
        exceptions = listOf(exception("LIFETIME", "2025/2026", 400_000)),
        productTypes = productTypes(),
      )

    assertTrue(notes.exceptions.isEmpty())
  }

  @Test
  fun resolveRulesetNotes_dropsExceptionsWithNoProductTypeToNameThem() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2026/2027",
        rulesets = rulesets(),
        exceptions = listOf(exception("HELP_TO_BUY", "2026/2027", 240_000)),
        productTypes = productTypes(),
      )

    assertTrue(notes.exceptions.isEmpty())
  }

  @Test
  fun resolveRulesetNotes_returnsAnEmptyStateForAnUnknownYear() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "1999/2000",
        rulesets = rulesets(),
        exceptions = listOf(exception("LIFETIME", "1999/2000", 400_000)),
        productTypes = productTypes(),
      )

    assertNull(notes.ruleset)
    assertNull(notes.notes)
    assertNull(notes.sharedAllowancePence)
    assertTrue(notes.exceptions.isEmpty())
  }

  @Test
  fun resolveRulesetNotes_handlesAYearWithoutNotes() {
    val notes =
      resolveRulesetNotes(
        rulesetId = "2025/2026",
        rulesets = rulesets(),
        exceptions = emptyList(),
        productTypes = productTypes(),
      )

    assertNull(notes.notes)
  }

  @Test
  fun resolveAccountsUiState_carriesTheSelectedYearsNotes() {
    val state =
      resolveAccountsUiState(
        accounts = emptyList(),
        balances = emptyList(),
        rulesets = rulesets(),
        selectedId = "2025/2026",
        exceptions = listOf(exception("JUNIOR", "2025/2026", 900_000, includedInShared = false)),
        productTypes = productTypes(),
      )

    assertEquals("2025/2026", state.notes.ruleset)
    assertEquals(900_000L, state.notes.exceptions.single().allowancePence)
  }

  private fun rulesets() = listOf(
    RulesetEntity("2025/2026", 2_000_000, "2025-04-06", "2026-04-05", null),
    RulesetEntity("2026/2027", 2_000_000, "2026-04-06", "2027-04-05", "Frozen until 2030/2031."),
  )

  private fun productTypes() = listOf(
    productType("LIFETIME", "Lifetime ISA"),
    productType("JUNIOR", "Junior ISA"),
  )

  private fun productType(id: String, name: String) = ProductTypeEntity(
    id = id,
    name = name,
    introducedWithRuleset = "2017/2018",
    removedWithRuleset = null,
    shortDescription = "",
    longDescription = "",
  )

  private fun exception(
    productTypeId: String,
    rulesetId: String,
    allowancePence: Long,
    includedInShared: Boolean = true,
  ) = RulesetExceptionEntity(
    id = "$productTypeId-$rulesetId",
    productTypeId = productTypeId,
    rulesetId = rulesetId,
    allowancePence = allowancePence,
    notes = null,
    includedInShared = includedInShared,
  )
}
