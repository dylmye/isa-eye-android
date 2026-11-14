package me.dylmye.isa

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.components.SkeletonCard
import me.dylmye.isa.ui.components.SkeletonGroup
import me.dylmye.isa.ui.components.SkeletonLine
import org.junit.Rule
import org.junit.Test

/** The skeleton placeholders are decorative; only their group is announced while a screen loads. */
class LoadingSkeletonTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun skeletonGroup_isOneLabelledNode() {
    composeTestRule.setContent {
      ISAEyeTheme(dynamicColor = false) {
        SkeletonGroup(label = "Loading content") {
          SkeletonCard { SkeletonLine(modifier = Modifier.fillMaxWidth()) }
        }
      }
    }

    composeTestRule
      .onAllNodesWithContentDescription("Loading content")
      .assertCountEquals(1)
    composeTestRule.onNodeWithContentDescription("Loading content").assertIsDisplayed()
  }
}
