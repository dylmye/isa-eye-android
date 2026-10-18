package me.dylmye.isa.screenshot

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.AccountDetailContent
import me.dylmye.isa.ui.accounts.AccountDetailUiState
import me.dylmye.isa.ui.accounts.AccountListItemUiState
import me.dylmye.isa.ui.accounts.AccountsContent
import me.dylmye.isa.ui.accounts.AccountsUiState
import me.dylmye.isa.ui.accounts.ContributionTrend
import me.dylmye.isa.ui.accounts.RulesetExceptionUiState
import me.dylmye.isa.ui.accounts.RulesetNotesBody
import me.dylmye.isa.ui.accounts.RulesetNotesUiState
import me.dylmye.isa.ui.accounts.RulesetState

/** The account list, with allowance left over. */
@PreviewTest
@ScreenPreviews
@Composable
fun AccountsScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AccountsContent(
      uiState = PreviewAccounts,
      onAccountClick = {},
      onPreviousRuleset = {},
      onNextRuleset = {},
      onResetRuleset = {},
      onShowNotes = {},
    )
  }
}

/** The account list before anything has been added. */
@PreviewTest
@ScreenPreviews
@Composable
fun AccountsEmptyScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AccountsContent(
      uiState = AccountsUiState(ruleset = RulesetState(current = "2026/2027", canGoBack = true)),
      onAccountClick = {},
      onPreviousRuleset = {},
      onNextRuleset = {},
      onResetRuleset = {},
      onShowNotes = {},
    )
  }
}

/** An account in the tax year being viewed, so the trend against the year before is shown. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@PreviewTest
@ScreenPreviews
@Composable
fun AccountDetailScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AccountDetailContent(
      uiState = PreviewAccountDetail,
      // The app only offers a way back when the pane stands alone. Ask the same question of the
      // same directive, so a wide preview does not draw a button the app would not.
      showBackButton = paneNeedsBackButton(),
      onBack = {},
      onEdit = {},
      onUpdateContributions = {},
    )
  }
}

/** An account in an earlier tax year, where the copy and the trend both differ. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@PreviewTest
@ScreenPreviews
@Composable
fun AccountDetailEarlierYearScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AccountDetailContent(
      uiState =
        PreviewAccountDetail.copy(
          name = "Stocks & Shares ISA",
          contributionsPence = 432_050,
          ruleset = "2024/2025",
          previousRuleset = "2023/2024",
          isLatestRuleset = false,
          trend = ContributionTrend.Down,
        ),
      showBackButton = paneNeedsBackButton(),
      onBack = {},
      onEdit = {},
      onUpdateContributions = {},
    )
  }
}

/** The ruleset notes, which carry the allowance breakdown. */
@PreviewTest
@ScreenPreviews
@Composable
fun RulesetNotesScreenshot() {
  ISAEyeTheme(dynamicColor = false) { RulesetNotesBody(PreviewRulesetNotes) }
}

/** A ruleset with no notes and no exceptions to list. */
@PreviewTest
@ScreenPreviews
@Composable
fun RulesetNotesEmptyScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    RulesetNotesBody(PreviewRulesetNotes.copy(notes = null, exceptions = emptyList()))
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun paneNeedsBackButton(): Boolean =
  calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions == 1

private val PreviewAccounts =
  AccountsUiState(
    accounts =
      listOf(
        AccountListItemUiState("1", "Cash ISA 2025/26", "Lloyds Bank", "#006A4D", 125_000),
        AccountListItemUiState("2", "Stocks & Shares ISA", "Vanguard", "#8A1E2D", 432_050),
        AccountListItemUiState("3", "Help to Buy ISA", "Nationwide", "#004B87", 0),
        AccountListItemUiState("4", "Junior ISA", "Hargreaves Lansdown", "#003A70", 0),
      ),
    ruleset = RulesetState(current = "2026/2027", canGoBack = true),
    remainingPence = 1_250_000,
  )

private val PreviewAccountDetail =
  AccountDetailUiState(
    name = "Cash ISA",
    typeName = "Cash ISA",
    longDescription =
      "A deposit account, where your cash is guaranteed. The rules for accessing the cash " +
        "once deposited may differ by account.",
    contributionsPence = 125_000,
    ruleset = "2026/2027",
    previousRuleset = "2025/2026",
    isLatestRuleset = true,
    trend = ContributionTrend.Up,
  )

private val PreviewRulesetNotes =
  RulesetNotesUiState(
    ruleset = "2026/2027",
    notes =
      "Long-Term Asset Funds (LTAFs) must now be held in Stocks & Shares ISAs only, not " +
        "Innovative Finance ISAs. Crypto Exchange Traded Notes (c-ETNs) must be held in " +
        "Innovative Finance ISAs only. There were no changes to allowances.",
    sharedAllowancePence = 2_000_000,
    exceptions =
      listOf(
        RulesetExceptionUiState("Lifetime ISA", 400_000, includedInShared = true),
        RulesetExceptionUiState("Junior ISA", 900_000, includedInShared = false),
      ),
  )
