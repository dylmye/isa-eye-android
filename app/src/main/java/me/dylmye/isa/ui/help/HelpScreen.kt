package me.dylmye.isa.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.containerTint
import me.dylmye.isa.ui.icons.chevronForward

private const val AUTHOR_URL = "https://dylmye.me"
private const val SOURCE_URL = "https://github.com/dylmye/isa-eye-android"

/**
 * The Help tab: the web's first-run intro, then a card per topic opening its own page. The topics are
 * registered as a list pane so a wide window shows the card list beside the page it opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onTopicClick: (String) -> Unit, modifier: Modifier = Modifier) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text("Help") }, scrollBehavior = scrollBehavior) },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentPadding =
        PaddingValues(
          start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
          top = innerPadding.calculateTopPadding() + 8.dp,
          end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
          // Clears the add-account button floating over every tab.
          bottom = innerPadding.calculateBottomPadding() + 88.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { HelpIntro() }
      items(HelpTopic.entries, key = { it.id }) { topic ->
        HelpTopicCard(topic = topic, onClick = { onTopicClick(topic.id) })
      }
      item { HelpAttribution() }
    }
  }
}

/** The intro shown first, carried over from the web's first-run dialog. */
@Composable
private fun HelpIntro(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("This is your ISA Eye.", style = MaterialTheme.typography.titleLarge)
    Text(
      text = "Keep track of your ISA contributions, across banks and years.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text =
        "Get started by adding your accounts. Then update these accounts with the amount you've " +
          "contributed to them in the current tax year (6th April to 5th April the following year).",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text =
        "The information you put into ISA Eye is kept entirely private and never leaves your device.",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

/** The attribution line, kept at the foot of the page under the topic cards. */
@Composable
private fun HelpAttribution(modifier: Modifier = Modifier) {
  Text(
    text = attribution(),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
  )
}

/**
 * A distinct wash per topic. These are unrelated to any account's provider colour; they exist to put
 * some colour into a page that would otherwise be a stack of identical cards.
 */
private val HelpTopic.tint: Color
  get() =
    when (this) {
      HelpTopic.WhatIsAnIsa -> Color(0xFF42A5F5)
      HelpTopic.WhatCanISaveWithAnIsa -> Color(0xFF66BB6A)
      HelpTopic.AreIsasRightForMe -> Color(0xFFFFA726)
      HelpTopic.FindingTheBestAccounts -> Color(0xFFAB47BC)
      HelpTopic.MaximisingTheAllowance -> Color(0xFF26A69A)
    }

/** One topic, opening its page. */
@Composable
private fun HelpTopicCard(topic: HelpTopic, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = topic.tint.containerTint()),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = topic.title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      Icon(
        imageVector = chevronForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** The attribution line, with the author and the source code as tappable links. */
private fun attribution(): AnnotatedString = buildAnnotatedString {
  append("ISA Eye is a ")
  withLink(LinkAnnotation.Url(AUTHOR_URL)) { append("dylmye") }
  append(" creation. You can view the source code for the app on ")
  withLink(LinkAnnotation.Url(SOURCE_URL)) { append("GitHub") }
  append(".")
}

@FormFactorPreviews
@Composable
private fun HelpScreenPreview() {
  ISAEyeTheme { HelpScreen(onTopicClick = {}) }
}
