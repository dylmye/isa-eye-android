package me.dylmye.isa.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the generated help content: the topics and their copy are both produced by
 * `scripts/generate-help.mjs`, so these check the two stay in step rather than that either is
 * correct in isolation.
 */
class HelpContentTest {
  @Test
  fun everyTopicHasCopy() {
    HelpTopics.forEach { topic ->
      assertTrue("${topic.id} has no copy", !HelpContent[topic.id].isNullOrEmpty())
    }
  }

  @Test
  fun noCopyIsOrphanedFromATopic() {
    val ids = HelpTopics.map { it.id }.toSet()

    assertEquals(emptySet<String>(), HelpContent.keys - ids)
  }

  @Test
  fun topicsAreTitledAndDistinct() {
    assertEquals(HelpTopics.size, HelpTopics.map { it.id }.toSet().size)
    assertEquals(HelpTopics.size, HelpTopics.map { it.title }.toSet().size)
    HelpTopics.forEach { topic ->
      assertTrue("${topic.id} has a blank title", topic.title.isNotBlank())
    }
  }

  @Test
  fun noCopyRendersAsAnEmptySpan() {
    HelpContent.values.flatten().forEach { block ->
      val spans =
        when (block) {
          is HelpBlock.Heading -> block.spans
          is HelpBlock.Paragraph -> block.spans
          is HelpBlock.Callout -> block.spans
          is HelpBlock.Bullets -> block.items.flatten()
        }
      spans.forEach { span ->
        val text =
          when (span) {
            is HelpSpan.Text -> span.text
            is HelpSpan.Bold -> span.text
            is HelpSpan.Link -> span.text
          }
        assertTrue("a span rendered empty", text.isNotEmpty())
      }
    }
  }
}
