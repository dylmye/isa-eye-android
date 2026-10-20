package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.insights.SetGoalContent
import me.dylmye.isa.ui.insights.SetGoalUiState

/*
 * The goal dialog. It is a centred card at every width, so the shared screen sizes cover it
 * directly; only the first-run state needs a preview of its own.
 */

/** A goal already saved, so the large field, the cap and the remove action all show. */
@PreviewTest
@ScreenPreviews
@Composable
fun SetGoalScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    SetGoalContent(
      uiState = PreviewSetGoal,
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

/** The first time it is opened: the title invites a goal and there is nothing to remove. */
@PreviewTest
@Preview(name = "Compact 400x500", widthDp = 400, heightDp = 500)
@Composable
fun SetGoalEmptyScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    SetGoalContent(
      uiState = PreviewSetGoal.copy(input = "", savedPence = null),
      onAmountChange = {},
      onSave = {},
      onRemove = {},
      onClose = {},
    )
  }
}

private val PreviewSetGoal =
  SetGoalUiState(
    ruleset = "2026/2027",
    allowancePence = 2_000_000,
    input = "12000",
    savedPence = 1_200_000,
    isLoading = false,
  )
