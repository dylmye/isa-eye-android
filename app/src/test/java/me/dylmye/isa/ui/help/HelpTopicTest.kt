package me.dylmye.isa.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelpTopicTest {
  @Test
  fun fromId_resolvesEveryTopicItLists() {
    HelpTopic.entries.forEach { topic -> assertEquals(topic, HelpTopic.fromId(topic.id)) }
  }

  @Test
  fun fromId_returnsNullForAnUnknownTopic() {
    assertNull(HelpTopic.fromId("how-to-fold-a-fitted-sheet"))
  }

  @Test
  fun ids_areUniqueSoNoTopicIsShadowed() {
    val ids = HelpTopic.entries.map { it.id }

    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun titles_areUniqueSoTheCardsReadDistinctly() {
    val titles = HelpTopic.entries.map { it.title }

    assertEquals(titles.size, titles.toSet().size)
  }
}
