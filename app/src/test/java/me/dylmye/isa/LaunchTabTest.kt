package me.dylmye.isa

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTabTest {
  @Test
  fun launchTab_opensInsightsOnlyOnceAnAccountExists() {
    assertEquals(Insights, launchTab(hasAccount = true))
    assertEquals(Accounts, launchTab(hasAccount = false))
  }
}
