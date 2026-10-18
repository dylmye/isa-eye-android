package me.dylmye.isa.screenshot

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.insights.InsightsScreen

/** The insights tab, which is all placeholder charts for now. */
@PreviewTest
@ScreenPreviews
@Composable
fun InsightsScreenshot() {
  ISAEyeTheme(dynamicColor = false) { InsightsScreen() }
}
