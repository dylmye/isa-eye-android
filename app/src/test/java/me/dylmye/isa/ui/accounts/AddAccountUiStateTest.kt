package me.dylmye.isa.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddAccountUiStateTest {
  @Test
  fun accountDisplayName_usesNicknameWhenPresent() {
    assertEquals("My Cash ISA", accountDisplayName("My Cash ISA", "Lloyds Bank", "Cash ISA"))
  }

  @Test
  fun accountDisplayName_fallsBackToProviderAndType() {
    assertEquals("Lloyds Bank Cash ISA", accountDisplayName("   ", "Lloyds Bank", "Cash ISA"))
  }

  @Test
  fun canContinue_requiresProviderAndRuleset() {
    assertFalse(AddAccountUiState().canContinue)
    assertFalse(AddAccountUiState(selectedProviderId = "lloyds").canContinue)
    assertTrue(
      AddAccountUiState(selectedProviderId = "lloyds", selectedRulesetId = "2025/2026").canContinue,
    )
  }

  @Test
  fun canSave_requiresProductType() {
    assertFalse(AddAccountUiState().canSave)
    assertTrue(AddAccountUiState(selectedProductTypeId = "CASH").canSave)
  }

  @Test
  fun matches_checksLabelAndSearchTerms() {
    val option = PickerOption("lloyds", "Lloyds Bank", searchTerms = listOf("lloyds tsb", "tsb"))

    assertTrue(option.matches("lloyd"))
    assertTrue(option.matches("TSB"))
    assertFalse(option.matches("barclays"))
  }
}
