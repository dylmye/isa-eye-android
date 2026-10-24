package me.dylmye.isa.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReminderSchedulerTest {
  @Test
  fun nextReminderRun_usesTodayWhenNineIsStillToCome() {
    assertEquals(at("2026-11-01 09:00"), nextReminderRun(at("2026-11-01 08:59")))
  }

  @Test
  fun nextReminderRun_usesTomorrowOnceNineHasPassed() {
    assertEquals(at("2026-11-02 09:00"), nextReminderRun(at("2026-11-01 09:00")))
  }

  @Test
  fun nextReminderRun_crossesTheEndOfTheMonth() {
    assertEquals(at("2026-12-01 09:00"), nextReminderRun(at("2026-11-30 18:00")))
  }
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
