package me.dylmye.isa.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Test

class AddAccountGateActionTest {
  @Test
  fun opensTheFormBelowTheLimit() {
    assertEquals(
      AddAccountGateAction.OpenAddAccount,
      addAccountGateAction(freeAccountLimit = 4, isUnlimited = false, accountCount = 3),
    )
  }

  @Test
  fun startsAPurchaseAtTheLimit() {
    assertEquals(
      AddAccountGateAction.Purchase,
      addAccountGateAction(freeAccountLimit = 4, isUnlimited = false, accountCount = 4),
    )
  }

  @Test
  fun startsAPurchaseAboveTheLimit() {
    assertEquals(
      AddAccountGateAction.Purchase,
      addAccountGateAction(freeAccountLimit = 4, isUnlimited = false, accountCount = 9),
    )
  }

  @Test
  fun opensTheFormWhenThereIsNoLimit() {
    assertEquals(
      AddAccountGateAction.OpenAddAccount,
      addAccountGateAction(freeAccountLimit = null, isUnlimited = false, accountCount = 20),
    )
  }

  @Test
  fun opensTheFormWhenUnlimited() {
    assertEquals(
      AddAccountGateAction.OpenAddAccount,
      addAccountGateAction(freeAccountLimit = 4, isUnlimited = true, accountCount = 20),
    )
  }
}
