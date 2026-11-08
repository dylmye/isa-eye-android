package me.dylmye.isa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.AddAccountContent
import me.dylmye.isa.ui.accounts.AddAccountStep
import me.dylmye.isa.ui.accounts.AddAccountUiState
import me.dylmye.isa.ui.accounts.PickerOption
import me.dylmye.isa.ui.accounts.UpdateContributionsContent
import me.dylmye.isa.ui.accounts.UpdateContributionsUiState
import me.dylmye.isa.ui.insights.SetGoalContent
import me.dylmye.isa.ui.insights.SetGoalUiState
import org.junit.Rule
import org.junit.Test

/** Guards the desktop promise that a form can be typed into as soon as it opens. */
class InitialFormFocusTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun setGoal_focusesItsAmountOnOpen() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        SetGoalContent(
          uiState = SetGoalUiState(input = "12000", savedPence = 1_200_000, isLoading = false),
          onAmountChange = {},
          onSave = {},
          onRemove = {},
          onClose = {},
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("robo_goal_amount").assertIsFocused()
  }

  @Test
  fun addAccountFollowUp_focusesTheFirstControlOfTheVisibleStep() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        AddAccountContent(
          uiState =
            AddAccountUiState(
              step = AddAccountStep.FollowUp,
              productTypeOptions = listOf(PickerOption("CASH", "Cash ISA")),
              selectedProductTypeId = "CASH",
            ),
          onNicknameChange = {},
          onProviderSelected = {},
          onRulesetSelected = {},
          onProductTypeSelected = {},
          onFlexibleChange = {},
          onNext = {},
          onBack = {},
          onSave = {},
          onRestorePurchases = {},
          onClose = {},
          isCompact = true,
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("robo_add_type").assertIsFocused()
  }

  @Test
  fun contributionAmount_keepsItsOpeningFocusAcrossConfigurationChange() {
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent {
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

    composeTestRule.onNodeWithTag("robo_contribution_amount").assertIsFocused()

    // The rebuilt composition restores the opening focus rather than losing it.
    restorationTester.emulateSavedInstanceStateRestore()

    composeTestRule.onNodeWithTag("robo_contribution_amount").assertIsFocused()
  }
}
