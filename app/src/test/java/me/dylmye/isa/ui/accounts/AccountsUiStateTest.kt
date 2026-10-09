package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsUiStateTest {
  @Test
  fun toListItemUiState_mapsRow_andDefaultsBalanceToZero() {
    val summary =
      AccountSummary(
        productId = "p1",
        friendlyName = "Cash ISA",
        productTypeId = "CASH",
        providerId = "lloyds",
        providerName = "Lloyds Bank",
        providerColour = "#006A4D",
        providerIconUrl = "https://example.com/lloyds.svg",
      )

    val item = summary.toListItemUiState()

    assertEquals("p1", item.productId)
    assertEquals("Cash ISA", item.name)
    assertEquals("Lloyds Bank", item.providerName)
    assertEquals("#006A4D", item.providerColour)
    assertEquals("https://example.com/lloyds.svg", item.providerIconUrl)
    assertEquals(0L, item.balancePence)
  }

  @Test
  fun resolveRuleset_defaultsToTheLatest() {
    val ids = listOf("2024/2025", "2025/2026", "2026/2027")

    val latest = resolveRuleset(ids, selectedId = null)

    assertEquals("2026/2027", latest.current)
    assertTrue(latest.canGoBack)
    assertFalse(latest.canGoForward)
  }

  @Test
  fun resolveRuleset_reportsStepBoundsForTheSelection() {
    val ids = listOf("2024/2025", "2025/2026", "2026/2027")

    val middle = resolveRuleset(ids, selectedId = "2025/2026")
    assertEquals("2025/2026", middle.current)
    assertTrue(middle.canGoBack)
    assertTrue(middle.canGoForward)

    val oldest = resolveRuleset(ids, selectedId = "2024/2025")
    assertFalse(oldest.canGoBack)
    assertTrue(oldest.canGoForward)
  }

  @Test
  fun resolveRuleset_fallsBackToTheLatest_whenTheSelectionIsUnknown() {
    val ids = listOf("2024/2025", "2025/2026")

    val unknown = resolveRuleset(ids, selectedId = "1999/2000")

    assertEquals("2025/2026", unknown.current)
  }

  @Test
  fun resolveRuleset_handlesNoRulesets() {
    val empty = resolveRuleset(emptyList(), selectedId = null)

    assertNull(empty.current)
    assertFalse(empty.canGoBack)
    assertFalse(empty.canGoForward)
  }
}
