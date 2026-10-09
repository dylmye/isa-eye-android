package me.dylmye.isa.ui.accounts

import me.dylmye.isa.data.db.dao.AccountSummary
import org.junit.Assert.assertEquals
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
}
