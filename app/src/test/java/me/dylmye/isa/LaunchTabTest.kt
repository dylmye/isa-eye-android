package me.dylmye.isa

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTabTest {
  @Test
  fun launchTab_opensHelpOnTheFirstLaunchAndInsightsAfterwards() {
    assertEquals(Insights, launchTab(hasLaunched = true))
    assertEquals(Help, launchTab(hasLaunched = false))
  }
}
