package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.accounts.AutoContributionContent
import me.dylmye.isa.ui.accounts.AutoContributionUiState

/*
 * The auto-contribution dialog. It is a centred card at every width, so the shared screen sizes
 * cover it directly; only the first-run state needs a preview of its own.
 */

/** A saved schedule, so the filled field, the projection and the turn-off action all show. */
@PreviewTest
@ScreenPreviews
@Composable
fun AutoContributionScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AutoContributionContent(
      uiState = PreviewAutoContribution,
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

/** The first time it is opened: empty field, no turn-off action, and the button invites setup. */
@PreviewTest
@Preview(name = "Compact 400x500", widthDp = 400, heightDp = 500)
@Composable
fun AutoContributionEmptyScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    AutoContributionContent(
      uiState = PreviewAutoContribution.copy(input = "", savedMonthlyPence = null),
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

private val PreviewAutoContribution =
  AutoContributionUiState(
    ruleset = "2026/2027",
    input = "500",
    savedMonthlyPence = 50_000,
    firstCreditDate = "2026-11-01",
    lastCreditDate = "2027-04-01",
    credits = 6,
    isLoading = false,
  )
