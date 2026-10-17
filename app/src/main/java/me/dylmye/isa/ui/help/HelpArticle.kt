package me.dylmye.isa.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.components.InfoCalloutCard

/**
 * Renders a help article's parsed copy in the app's own typography, rather than in a generic
 * markdown look: headings step down through the type scale and callouts become callout cards.
 */
@Composable
internal fun HelpArticle(blocks: List<HelpBlock>, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    blocks.forEach { block ->
      when (block) {
        is HelpBlock.Heading -> {
          Text(
            text = block.spans.toHelpText(),
            style =
              if (block.level <= 2) {
                MaterialTheme.typography.titleLarge
              } else {
                MaterialTheme.typography.titleMedium
              },
            // Headings need more air above them than the uniform gap gives.
            modifier = Modifier.padding(top = 8.dp),
          )
        }

        is HelpBlock.Paragraph -> {
          Text(text = block.spans.toHelpText(), style = MaterialTheme.typography.bodyLarge)
        }

        is HelpBlock.Bullets -> {
          HelpBullets(block.items)
        }

        is HelpBlock.Callout -> {
          InfoCalloutCard(text = block.spans.toHelpText())
        }
      }
    }
  }
}

@Composable
private fun HelpBullets(items: List<List<HelpSpan>>) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items.forEach { item ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(
          text = "•",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = item.toHelpText(),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

/** Maps the parsed spans onto an annotated string, so links behave as they do elsewhere. */
private fun List<HelpSpan>.toHelpText(): AnnotatedString = buildAnnotatedString {
  forEach { span ->
    when (span) {
      is HelpSpan.Text -> {
        append(span.text)
      }

      is HelpSpan.Bold -> {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(span.text) }
      }

      is HelpSpan.Link -> {
        withLink(LinkAnnotation.Url(span.url)) { append(span.text) }
      }
    }
  }
}
