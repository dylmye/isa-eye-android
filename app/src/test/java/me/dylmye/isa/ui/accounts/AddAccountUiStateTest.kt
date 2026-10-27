package me.dylmye.isa.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddAccountUiStateTest {
  @Test
  fun accountDisplayName_usesNicknameWhenPresent() {
    assertEquals("My Cash ISA", accountDisplayName("My Cash ISA", "Cash ISA"))
  }

  @Test
  fun accountDisplayName_fallsBackToTheProductType() {
    assertEquals("Cash ISA", accountDisplayName("   ", "Cash ISA"))
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

  @Test
  fun showLimitCard_isOnlyShownWhileLimitedAndUnderTheLimit() {
    val limited = AddAccountUiState(accountCount = 2, freeAccountLimit = 4)

    assertTrue(limited.showLimitCard)
    assertFalse(limited.copy(accountCount = 4).showLimitCard)
    assertFalse(limited.copy(isUnlimited = true).showLimitCard)
    assertFalse(limited.copy(freeAccountLimit = null).showLimitCard)
  }

  @Test
  fun canCreateAccount_allowsBelowTheLimitOrWhenUnlimited() {
    val limited = AddAccountUiState(accountCount = 3, freeAccountLimit = 4)

    assertTrue(limited.canCreateAccount)
    assertFalse(limited.copy(accountCount = 4).canCreateAccount)
    assertTrue(limited.copy(accountCount = 10, isUnlimited = true).canCreateAccount)
    assertTrue(limited.copy(freeAccountLimit = null).canCreateAccount)
  }
}
