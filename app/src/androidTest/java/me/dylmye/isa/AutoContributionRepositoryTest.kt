package me.dylmye.isa

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.dylmye.isa.data.AutoContributionRepository
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.ProductEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.ProviderEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class AutoContributionRepositoryTest {
  private lateinit var database: IsaDatabase
  private lateinit var repository: AutoContributionRepository

  @Before
  fun setUp() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, IsaDatabase::class.java).build()
    repository = AutoContributionRepository(database)
    database.providerDao().upsertAll(listOf(ProviderEntity("provider", "Bank", null, "#006A4D")))
    database.productTypeDao().upsertAll(
      listOf(ProductTypeEntity("CASH", "Cash ISA", "2008/2009", null, "", "", "#2E7D32")),
    )
    database.rulesetDao().upsertAll(
      listOf(RulesetEntity("2026/2027", 2_000_000, "2026-04-06", "2027-04-05", null)),
    )
    database.productDao().upsertAll(
      listOf(
        ProductEntity("a1", "2026/2027", null, "provider", "Cash ISA", "CASH", flexible = false),
        ProductEntity("a2", "2026/2027", null, "provider", "Stocks", "CASH", flexible = false),
      ),
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun applyDue_recordsWhatTheLastCreditAdded() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-10-01")

    repository.applyDue(at("2026-11-15 09:00"))

    val schedule = database.autoContributionDao().find("a1", "2026/2027")
    assertEquals("2026-11-01", schedule?.appliedThroughDate)
    assertEquals(50_000L, schedule?.lastCreditPence)
    assertEquals(100_000L, balance("a1"))
  }

  @Test
  fun undoLastCredit_takesExactlyOneMonthBackOut() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-10-01")
    repository.applyDue(at("2026-11-15 09:00"))

    assertTrue(repository.undoLastCredit("a1", "2026/2027"))

    assertEquals(50_000L, balance("a1"))
    assertNull(database.autoContributionDao().find("a1", "2026/2027")?.lastCreditPence)
    // The schedule keeps its place, so the month is not credited again.
    assertEquals(
      "2026-11-01",
      database.autoContributionDao().find("a1", "2026/2027")?.appliedThroughDate,
    )
    assertFalse(repository.undoLastCredit("a1", "2026/2027"))
    assertEquals(50_000L, balance("a1"))
  }

  @Test
  fun undoLastCredit_undoesOnlyOneMonthOfACatchUp() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-10-01")
    // The app was closed for two months, so October, November and December land together.
    repository.applyDue(at("2026-12-15 09:00"))
    assertEquals(150_000L, balance("a1"))

    assertTrue(repository.undoLastCredit("a1", "2026/2027"))
    // Only December comes back out; the earlier two months stay.
    assertEquals(100_000L, balance("a1"))

    assertFalse(repository.undoLastCredit("a1", "2026/2027"))
    assertFalse(repository.undoLastCredit("a1", "2026/2027"))
    assertEquals(100_000L, balance("a1"))
  }

  @Test
  fun undoLastCredit_usesTheAmountThatLandedWhenTheAmountChangedSince() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-11-01")
    repository.applyDue(at("2026-11-01 09:00"))
    repository.setSchedule("a1", "2026/2027", 70_000, "2026-11-01")

    repository.undoLastCredit("a1", "2026/2027")

    assertEquals(0L, balance("a1"))
  }

  @Test
  fun undoMonth_onlyTouchesTheAccountsItNames() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-10-01")
    repository.setSchedule("a2", "2026/2027", 50_000, "2026-10-01")
    repository.applyDue(at("2026-11-15 09:00"))

    val undone = repository.undoMonth(
      listOf("a1"),
      "2026/2027",
      "2026-11-01",
      at("2026-11-02 20:00"),
    )

    assertEquals(1, undone)
    assertEquals(50_000L, balance("a1"))
    assertEquals(100_000L, balance("a2"))

    // Tapping the same month again has nothing left to take back.
    assertEquals(
      0,
      repository.undoMonth(listOf("a1"), "2026/2027", "2026-11-01", at("2026-11-03 09:00")),
    )
    assertEquals(50_000L, balance("a1"))
  }

  @Test
  fun undoMonth_leavesALaterCreditAlone() = runTest {
    repository.setSchedule("a1", "2026/2027", 50_000, "2026-11-01")
    repository.applyDue(at("2026-12-15 09:00"))

    val undone = repository.undoMonth(
      listOf("a1"),
      "2026/2027",
      "2026-11-01",
      at("2026-12-16 20:00"),
    )

    assertEquals(0, undone)
    assertEquals(100_000L, balance("a1"))
  }

  private suspend fun balance(productId: String): Long =
    database.annualBalanceDao().find(productId, "2026/2027")?.deductedFromAllowancePence ?: 0L
}

private val ukZone: TimeZone = TimeZone.getTimeZone("Europe/London")

private fun at(text: String): Long {
  val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { timeZone = ukZone }
  return requireNotNull(parser.parse(text)).time
}
