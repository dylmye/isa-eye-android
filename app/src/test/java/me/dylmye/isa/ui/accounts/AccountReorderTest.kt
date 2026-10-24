package me.dylmye.isa.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AccountReorderTest {
  @Test
  fun move_downwards_landsAfterTheRowItPassed() {
    assertEquals(listOf("b", "a", "c", "d"), listOf("a", "b", "c", "d").move(0, 1))
  }

  @Test
  fun move_upwards_landsBeforeTheRowItPassed() {
    assertEquals(listOf("a", "c", "b", "d"), listOf("a", "b", "c", "d").move(2, 1))
  }

  @Test
  fun move_toTheSamePlace_returnsTheSameList() {
    val accounts = listOf("a", "b", "c")

    assertSame(accounts, accounts.move(1, 1))
  }

  @Test
  fun move_outsideTheList_returnsTheSameList() {
    val accounts = listOf("a", "b", "c")

    assertSame(accounts, accounts.move(0, 3))
    assertSame(accounts, accounts.move(-1, 1))
  }
}
