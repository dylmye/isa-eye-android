package me.dylmye.isa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.PickerOption
import me.dylmye.isa.ui.accounts.UpdateContributionsContent
import me.dylmye.isa.ui.accounts.UpdateContributionsUiState
import org.junit.Rule
import org.junit.Test

/** Guards the Tier 3 promise that the field being edited survives a configuration change. */
class ConfigurationChangeFocusTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun contributionAmount_keepsFocusAndTextAcrossConfigurationChange() {
    val lifecycleOwner = TestLifecycleOwner()
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent {
      WithTestLifecycle(lifecycleOwner) {
        ISAEyeTheme(dynamicColor = false) {
          var amount by rememberSaveable { mutableStateOf("1000") }
          UpdateContributionsContent(
            uiState =
              UpdateContributionsUiState(
                accountName = "Cash ISA",
                yearOptions = listOf(PickerOption("2026/2027", "2026/2027")),
                selectedYearId = "2026/2027",
                amountInput = amount,
                isLoading = false,
              ),
            onYearSelected = {},
            onAmountChange = { amount = it },
            onSave = {},
            onClose = {},
            isCompact = true,
          )
        }
      }
    }

    composeTestRule
      .onNodeWithTag("robo_contribution_amount")
      .requestFocus()
      .assertIsFocused()
      .performTextReplacement("2500")

    // Rebuilds the composition from saved state, as a rotation or window resize would. The Activity
    // is below RESUMED by the time the old composition is disposed, as it is for a real change.
    composeTestRule.runOnIdle { lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED }
    restorationTester.emulateSavedInstanceStateRestore()
    composeTestRule.runOnIdle { lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED }

    composeTestRule
      .onNodeWithTag("robo_contribution_amount")
      .assertIsFocused()
      .assertTextContains("2500")
  }

  @Test
  fun unfocusedField_doesNotGrabFocusAfterConfigurationChange() {
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent {
      ISAEyeTheme(dynamicColor = false) {
        UpdateContributionsContent(
          uiState =
            UpdateContributionsUiState(
              accountName = "Cash ISA",
              yearOptions = listOf(PickerOption("2026/2027", "2026/2027")),
              selectedYearId = "2026/2027",
              amountInput = "1000",
              isLoading = false,
            ),
          onYearSelected = {},
          onAmountChange = {},
          onSave = {},
          onClose = {},
          isCompact = true,
        )
      }
    }

    restorationTester.emulateSavedInstanceStateRestore()

    composeTestRule.onNodeWithTag("robo_contribution_amount").assertIsNotFocused()
  }

  @Test
  fun fieldThatLostFocus_doesNotReclaimFocusAfterConfigurationChange() {
    val lifecycleOwner = TestLifecycleOwner()
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent {
      WithTestLifecycle(lifecycleOwner) {
        ISAEyeTheme(dynamicColor = false) {
          UpdateContributionsContent(
            uiState =
              UpdateContributionsUiState(
                accountName = "Cash ISA",
                yearOptions = listOf(PickerOption("2026/2027", "2026/2027")),
                selectedYearId = "2026/2027",
                amountInput = "1000",
                isLoading = false,
              ),
            onYearSelected = {},
            onAmountChange = {},
            onSave = {},
            onClose = {},
            isCompact = true,
          )
        }
      }
    }

    composeTestRule
      .onNodeWithTag("robo_contribution_amount")
      .requestFocus()
      .assertIsFocused()

    // Moving focus to another control, as a tap or keyboard navigation would, clears the flag.
    composeTestRule.onNodeWithTag("robo_contribution_year").requestFocus().assertIsFocused()
    composeTestRule.onNodeWithTag("robo_contribution_amount").assertIsNotFocused()

    // Rebuilds the composition from saved state, as a rotation or window resize would.
    composeTestRule.runOnIdle { lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED }
    restorationTester.emulateSavedInstanceStateRestore()
    composeTestRule.runOnIdle { lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED }

    composeTestRule.onNodeWithTag("robo_contribution_amount").assertIsNotFocused()
    composeTestRule.onNodeWithTag("robo_contribution_year").assertIsFocused()
  }
}

/** Supplies the lifecycle the focus-restoration modifier observes during a configuration change. */
@Composable
private fun WithTestLifecycle(lifecycleOwner: TestLifecycleOwner, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner, content = content)
}
