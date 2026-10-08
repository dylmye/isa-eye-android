package me.dylmye.isa

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.dylmye.isa.data.db.IsaDatabase
import me.dylmye.isa.data.db.entity.ProviderEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IsaDatabaseTest {
  private lateinit var database: IsaDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, IsaDatabase::class.java).build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun providerDao_persistsAndReads() = runTest {
    database.providerDao().upsertAll(
      listOf(
        ProviderEntity(
          id = "lloyds",
          name = "Lloyds Bank",
          iconRelativeUrl = null,
          colour = "#006A4D",
        ),
      ),
    )

    val providers = database.providerDao().observeAll().first()

    assertEquals(1, providers.size)
    assertEquals("Lloyds Bank", providers.first().name)
  }
}
