package me.dylmye.isa.ui.help

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.icons.arrowBack

/** Wider than this and prose gets uncomfortable to track, so the copy is centred within a limit. */
private val MaxReadingWidth = 680.dp

/** Stateful entry point: resolves the topic and decides whether this pane needs a way back. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HelpTopicScreen(topicId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
  val topic = remember(topicId) { HelpTopics.firstOrNull { it.id == topicId } }
  val blocks = remember(topicId) { HelpContent[topicId].orEmpty() }
  // A pane that has the topic list beside it needs no way back; a lone one does. This asks the same
  // question of the same directive that the list-detail scene strategy is built with.
  val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
  val showBackButton =
    remember(windowAdaptiveInfo) {
      calculatePaneScaffoldDirective(windowAdaptiveInfo).maxHorizontalPartitions == 1
    }
  HelpTopicContent(
    title = topic?.title.orEmpty(),
    blocks = blocks,
    showBackButton = showBackButton,
    onBack = onBack,
    modifier = modifier,
  )
}

/**
 * Stateless help topic page: the article's heading in the app bar, its copy below. The copy is
 * generated from the markdown in `app/src/main/assets/help` by `scripts/generate-help.mjs`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTopicContent(
  title: String,
  blocks: List<HelpBlock>,
  showBackButton: Boolean,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier =
      modifier.semantics {
        paneTitle = title
      },
    topBar = {
      TopAppBar(
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        navigationIcon = {
          if (showBackButton) {
            IconButton(onClick = onBack) {
              Icon(arrowBack, contentDescription = "Back to help")
            }
          }
        },
      )
    },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = MaxReadingWidth)
            .verticalScroll(rememberScrollState())
            // The extra bottom room clears the add-account button floating over every tab.
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
      ) {
        HelpArticle(blocks)
      }
    }
  }
}

/** Shown in the page pane while the topic list is beside it and nothing is selected. */
@Composable
fun HelpTopicPlaceholder(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      "Select a topic to read it",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@FormFactorPreviews
@Composable
private fun HelpTopicContentPreview() {
  val topic = HelpTopics.first()
  ISAEyeTheme {
    HelpTopicContent(
      title = topic.title,
      blocks = HelpContent[topic.id].orEmpty(),
      showBackButton = true,
      onBack = {},
    )
  }
}
