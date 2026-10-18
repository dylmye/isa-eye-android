package me.dylmye.isa

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTabTest {
  @Test
  fun launchTab_opensHelpUntilAnAccountExistsThenInsights() {
    assertEquals(Insights, launchTab(hasAccount = true))
    assertEquals(Help, launchTab(hasAccount = false))
  }
}
