package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.insights.AccountShare
import me.dylmye.isa.ui.insights.GoalProgress
import me.dylmye.isa.ui.insights.InsightsContent
import me.dylmye.isa.ui.insights.InsightsUiState
import me.dylmye.isa.ui.insights.TaxYearStage
import me.dylmye.isa.ui.insights.TypeShare
import me.dylmye.isa.ui.insights.YearShare

/** The countdown, the goal, both donuts and the stacked bar chart, with contributions in every year. */
@PreviewTest
@ScreenPreviews
@Composable
fun InsightsScreenshot() {
  ISAEyeTheme(dynamicColor = false) { InsightsContent(uiState = PreviewInsights, onEditGoal = {}) }
}

/**
 * Nothing recorded this year: both donuts drop out, but the bar chart keeps its earlier years and
 * the goal stays, waiting for its first contribution. This is what an account added before any
 * contribution looks like.
 */
@PreviewTest
@ScreenPreviews
@Composable
fun InsightsEmptyYearScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    InsightsContent(uiState = PreviewInsights.withoutThisYear(), onEditGoal = {})
  }
}

/** A brand-new user: the countdown and the goal prompt, since no year has a contribution either. */
@PreviewTest
@ScreenPreviews
@Composable
fun InsightsEmptyScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    InsightsContent(
      uiState = PreviewInsights.withoutThisYear().copy(yearShares = emptyList(), goal = null),
      onEditGoal = {},
    )
  }
}

/** A goal reached, where the card trades the monthly arithmetic for the news that it is done. */
@PreviewTest
@ScreenPreviews
@Composable
fun InsightsGoalMetScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    InsightsContent(uiState = PreviewInsights.goalMet(), onEditGoal = {})
  }
}

/**
 * The three-column layout the grid is capped at. The shared screen previews top out at 900dp, which
 * only reaches two columns, so the widget grid needs a size of its own to be covered.
 */
@PreviewTest
@Preview(name = "Tablet landscape 1280x800", widthDp = 1280, heightDp = 800)
@Composable
fun InsightsWideScreenshot() {
  ISAEyeTheme(dynamicColor = false) { InsightsContent(uiState = PreviewInsights, onEditGoal = {}) }
}

/** The same state with the current year emptied, keeping the years that still have contributions. */
private fun InsightsUiState.withoutThisYear() =
  copy(
    currentYearTotalPence = 0,
    accountShares = emptyList(),
    typeShares = emptyList(),
    goal =
      goal?.copy(
        contributedPence = 0,
        monthlySuggestionPence = 333_400,
      ),
  )

/** The same state with a goal it has already reached, so the card's met copy is covered too. */
private fun InsightsUiState.goalMet() =
  copy(
    goal =
      goal?.copy(
        targetPence = currentYearTotalPence,
        contributedPence = currentYearTotalPence,
        monthlySuggestionPence = 0,
      ),
  )

private val PreviewInsights =
  InsightsUiState(
    ruleset = "2026/2027",
    daysRemaining = 169,
    stage = TaxYearStage.Mid,
    currentYearTotalPence = 165_000,
    accountShares =
      listOf(
        AccountShare("a1", "Cash ISA 2025/26", "#006A4D", 40_000),
        AccountShare("a2", "Stocks & Shares ISA", "#8A1E2D", 125_000),
      ),
    typeShares =
      listOf(
        TypeShare("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0", 125_000),
        TypeShare("CASH", "Cash ISA", "#2E7D32", 40_000),
      ),
    yearShares =
      listOf(
        YearShare(
          rulesetId = "2024/2025",
          label = "24/25",
          totalPence = 80_000,
          byType = listOf(TypeShare("CASH", "Cash ISA", "#2E7D32", 80_000)),
        ),
        YearShare(
          rulesetId = "2025/2026",
          label = "25/26",
          totalPence = 140_000,
          byType =
            listOf(
              TypeShare("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0", 90_000),
              TypeShare("CASH", "Cash ISA", "#2E7D32", 50_000),
            ),
        ),
        YearShare(
          rulesetId = "2026/2027",
          label = "26/27",
          totalPence = 165_000,
          byType =
            listOf(
              TypeShare("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0", 125_000),
              TypeShare("CASH", "Cash ISA", "#2E7D32", 40_000),
            ),
        ),
      ),
    goal =
      GoalProgress(
        targetPence = 2_000_000,
        contributedPence = 165_000,
        monthsRemaining = 6,
        monthlySuggestionPence = 305_900,
      ),
  )
