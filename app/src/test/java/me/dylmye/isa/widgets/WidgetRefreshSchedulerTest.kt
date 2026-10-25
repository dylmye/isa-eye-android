package me.dylmye.isa.widgets

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WidgetRefreshSchedulerTest {
  @Test
  fun nextWidgetRefreshRun_usesTodayWhenFivePastMidnightIsStillToCome() {
    assertEquals(at("2026-10-25 00:05"), nextWidgetRefreshRun(at("2026-10-25 00:00")))
  }

  @Test
  fun nextWidgetRefreshRun_usesTomorrowOnceFivePastMidnightHasPassed() {
    assertEquals(at("2026-10-26 00:05"), nextWidgetRefreshRun(at("2026-10-25 00:05")))
  }

  @Test
  fun nextWidgetRefreshRun_crossesTheEndOfTheMonth() {
    assertEquals(at("2026-11-01 00:05"), nextWidgetRefreshRun(at("2026-10-31 23:59")))
  }

  @Test
  fun nextWidgetRefreshRun_handlesTheClocksGoingBack() {
    // The UK leaves British Summer Time at 02:00 on 25 October 2026; the countdown still needs to
    // land just after the new day, which is an hour longer.
    assertEquals(at("2026-10-26 00:05"), nextWidgetRefreshRun(at("2026-10-25 12:00")))
  }
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
