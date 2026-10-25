package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.help.HelpScreen
import me.dylmye.isa.ui.help.HelpTopicScreen

/** The topic list. */
@PreviewTest
@ScreenPreviews
@Composable
fun HelpScreenScreenshot() {
  ISAEyeTheme(dynamicColor = false) { HelpScreen(onTopicClick = {}, onBackupClick = {}) }
}

/** The longest article, which covers headings, bullets, links and a callout. */
@PreviewTest
@ScreenPreviews
@Composable
fun HelpTopicScreenshot() {
  ISAEyeTheme(dynamicColor = false) {
    HelpTopicScreen(topicId = "maximising-isa-allowance", onBack = {})
  }
}
