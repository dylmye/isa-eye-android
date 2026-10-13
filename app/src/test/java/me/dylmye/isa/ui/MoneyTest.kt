package me.dylmye.isa.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
  @Test
  fun sanitizePoundsInput_keepsDigitsAndOneDecimal() {
    assertEquals("1250", sanitizePoundsInput("1250"))
    assertEquals("1250.50", sanitizePoundsInput("1250.50"))
    assertEquals("1250.50", sanitizePoundsInput("1250.505"))
    assertEquals("1250.50", sanitizePoundsInput("£1,250.50"))
    assertEquals("1250", sanitizePoundsInput("-1250"))
    assertEquals("1.23", sanitizePoundsInput("1.2.3"))
    assertEquals(".5", sanitizePoundsInput(".5"))
    assertEquals("", sanitizePoundsInput("abc"))
  }

  @Test
  fun parsePoundsToPence_readsWholePoundsAndPence() {
    assertEquals(125_000L, parsePoundsToPence("1250"))
    assertEquals(125_050L, parsePoundsToPence("1250.5"))
    assertEquals(125_050L, parsePoundsToPence("1250.50"))
    assertEquals(50L, parsePoundsToPence(".5"))
    assertEquals(0L, parsePoundsToPence("0"))
  }

  @Test
  fun parsePoundsToPence_rejectsInputItCannotUse() {
    assertNull(parsePoundsToPence(""))
    assertNull(parsePoundsToPence("."))
    assertNull(parsePoundsToPence("12.345"))
    assertNull(parsePoundsToPence("1234567890"))
  }

  @Test
  fun penceToPoundsInput_roundTripsThroughTheParser() {
    assertEquals("1250", penceToPoundsInput(125_000))
    assertEquals("1250.50", penceToPoundsInput(125_050))
    assertEquals("0", penceToPoundsInput(0))
    assertEquals(125_050L, parsePoundsToPence(penceToPoundsInput(125_050)))
    assertEquals(0L, parsePoundsToPence(penceToPoundsInput(-500)))
  }
}
