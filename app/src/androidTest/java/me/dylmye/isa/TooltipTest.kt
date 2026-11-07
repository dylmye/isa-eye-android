package me.dylmye.isa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.RulesetHeader
import me.dylmye.isa.ui.accounts.RulesetState
import org.junit.Rule
import org.junit.Test

/** Guards the hover labels a pointer user relies on, which have no semantics until shown. */
class TooltipTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun iconOnlyAction_showsItsLabelOnHover() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        RulesetHeader(
          state = RulesetState(current = "2026/2027", canGoBack = true, canReset = true),
          onPrevious = {},
          onNext = {},
          onReset = {},
          onShowNotes = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Next tax year").performMouseInput {
      moveTo(center)
    }

    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule.onAllNodesWithText("Next tax year").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Next tax year").assertIsDisplayed()
  }
}
