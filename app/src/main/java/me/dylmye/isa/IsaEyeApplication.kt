package me.dylmye.isa

import android.app.Application
import me.dylmye.isa.data.AccountRepository
import me.dylmye.isa.data.UserPreferences
import me.dylmye.isa.data.db.IsaDatabase

/** Holds the process-wide database and repositories (manual DI, no framework). */
class IsaEyeApplication : Application() {
  val database: IsaDatabase by lazy { IsaDatabase.build(this) }

  val accountRepository: AccountRepository by lazy { AccountRepository(database) }

  val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
