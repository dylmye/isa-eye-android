package me.dylmye.isa

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.AccountDetailContent
import me.dylmye.isa.ui.accounts.AccountDetailUiState
import me.dylmye.isa.ui.accounts.AccountListItemUiState
import me.dylmye.isa.ui.accounts.AccountsContent
import me.dylmye.isa.ui.accounts.AccountsUiState
import me.dylmye.isa.ui.accounts.ContributionTrend
import me.dylmye.isa.ui.accounts.FlexibleField
import me.dylmye.isa.ui.accounts.RulesetHeader
import me.dylmye.isa.ui.accounts.RulesetState
import me.dylmye.isa.ui.help.HelpScreen
import me.dylmye.isa.ui.insights.AccountShare
import me.dylmye.isa.ui.insights.InsightsContent
import me.dylmye.isa.ui.insights.InsightsUiState
import me.dylmye.isa.ui.insights.TypeShare
import me.dylmye.isa.ui.insights.YearShare
import org.junit.Rule
import org.junit.Test

/** Guards the semantics a screen reader depends on, beyond what the visual screenshot suite sees. */
class AccessibilityTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

  @Test
  fun accountRow_isOneClickableItemWithItsNameProviderAndAmount() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        AccountsContent(
          uiState =
            AccountsUiState(
              accounts =
                listOf(
                  AccountListItemUiState(
                    productId = "1",
                    name = "Cash ISA",
                    providerName = "Lloyds",
                    providerColour = "#006A4D",
                    contributionsPence = 125_000,
                  ),
                ),
              ruleset = RulesetState(current = "2026/2027", canGoBack = true),
            ),
          onAccountClick = {},
          onReorderAccounts = {},
          onPreviousRuleset = {},
          onNextRuleset = {},
          onResetRuleset = {},
          onShowNotes = {},
        )
      }
    }

    // The row, not the individual labels, is the thing TalkBack focuses and activates.
    composeTestRule
      .onNodeWithContentDescription("Cash ISA, Lloyds, £1,250.00 contributed")
      .assertHasClickAction()
  }

  @Test
  fun rulesetControls_haveSpokenLabels() {
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

    composeTestRule
      .onNodeWithContentDescription("Allowances for the 2026/2027 tax year")
      .assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Previous tax year").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Next tax year").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Reset to the latest tax year").assertIsDisplayed()
  }

  @Test
  fun accountDetail_hasLabelledActionsAndOneClickableSummary() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        AccountDetailContent(
          uiState =
            AccountDetailUiState(
              name = "Cash ISA",
              typeName = "Cash ISA",
              contributionsPence = 125_000,
              ruleset = "2026/2027",
              previousRuleset = "2025/2026",
              isLatestRuleset = true,
              trend = ContributionTrend.Up,
              lastUpdatedLabel = "3 days ago",
              isCurrentTaxYear = true,
            ),
          showBackButton = true,
          onBack = {},
          onEdit = {},
          onUpdateContributions = {},
          onSetUpAutoContribution = {},
          onDeleteAccount = {},
        )
      }
    }

    composeTestRule.onNodeWithText("£1,250.00").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Back to accounts").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Edit account").assertHasClickAction()
    composeTestRule.onNodeWithContentDescription("Account actions").assertIsDisplayed()
  }

  @Test
  fun flexibleField_makesTheWholeLabelledRowTheSwitch() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        FlexibleField(flexible = false, onFlexibleChange = {})
      }
    }

    composeTestRule.onNodeWithText("Flexible").assertIsToggleable().assertIsOff()
  }

  @Test
  fun screenTitles_areHeadings() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) { HelpScreen(onTopicClick = {}, onBackupClick = {}) }
    }

    composeTestRule.onNodeWithText("Help").assert(heading)
  }

  @Test
  fun insightsCharts_haveSpokenSummaries() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        InsightsContent(
          uiState =
            InsightsUiState(
              currentYearTotalPence = 100_000,
              accountShares = listOf(AccountShare("a1", "Cash ISA", "#2E7D32", 100_000)),
              typeShares = listOf(TypeShare("CASH", "Cash ISA", "#2E7D32", 100_000)),
              yearShares =
                listOf(
                  YearShare(
                    rulesetId = "2026/2027",
                    label = "26/27",
                    totalPence = 100_000,
                    byType = listOf(TypeShare("CASH", "Cash ISA", "#2E7D32", 100_000)),
                  ),
                ),
            ),
          onEditGoal = {},
        )
      }
    }

    composeTestRule
      .onNodeWithContentDescription(
        "£1,000.00 this year across 1 account: Cash ISA £1,000.00 (100%)",
      )
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription(
        "Contributions by year: 2026/2027 £1,000.00 (Cash ISA £1,000.00)",
      )
      .assertIsDisplayed()
  }
}
