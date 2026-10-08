package me.dylmye.isa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun bottomTabs_areDisplayed() {
    composeTestRule.onNodeWithText("Accounts").assertIsDisplayed()
    composeTestRule.onNodeWithText("Insights").assertIsDisplayed()
    composeTestRule.onNodeWithText("Help").assertIsDisplayed()
  }

  @Test
  fun addAccountFab_isDisplayed() {
    composeTestRule.onNodeWithText("Add account").assertIsDisplayed()
  }
}
