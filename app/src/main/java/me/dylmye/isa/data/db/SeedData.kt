package me.dylmye.isa.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Sample reference data inserted the first time the database is created, so the app has something
 * to display before real account entry exists. Safe to delete once data entry is implemented.
 */
internal object SeedData {
  val callback =
    object : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        statements.forEach(db::execSQL)
      }
    }

  private val statements =
    listOf(
      // Rulesets (id = tax year).
      "INSERT INTO rulesets (_id, sharedAllowancePence, startDate, endDate, notes) " +
        "VALUES ('2024/25', 2000000, '2024-04-06', '2025-04-05', NULL)",
      "INSERT INTO rulesets (_id, sharedAllowancePence, startDate, endDate, notes) " +
        "VALUES ('2025/26', 2000000, '2025-04-06', '2026-04-05', NULL)",
      // Product types.
      "INSERT INTO productTypes (_id, name, introducedWithRuleset, removedWithRuleset, " +
        "shortDescription, longDescription) VALUES " +
        "('cash', 'Cash ISA', '2024/25', NULL, 'Tax-free cash savings.', " +
        "'A Cash ISA lets you save cash without paying tax on the interest.')",
      "INSERT INTO productTypes (_id, name, introducedWithRuleset, removedWithRuleset, " +
        "shortDescription, longDescription) VALUES " +
        "('stocks', 'Stocks & Shares ISA', '2024/25', NULL, 'Tax-free investments.', " +
        "'A Stocks & Shares ISA lets you invest without paying tax on gains or dividends.')",
      "INSERT INTO productTypes (_id, name, introducedWithRuleset, removedWithRuleset, " +
        "shortDescription, longDescription) VALUES " +
        "('htb', 'Help to Buy ISA', '2024/25', NULL, 'Government bonus for first homes.', " +
        "'A Help to Buy ISA was a savings account with a government bonus for a first home.')",
      "INSERT INTO productTypes (_id, name, introducedWithRuleset, removedWithRuleset, " +
        "shortDescription, longDescription) VALUES " +
        "('junior', 'Junior ISA', '2024/25', NULL, 'Tax-free savings for under 18s.', " +
        "'A Junior ISA is a tax-free savings account for a child under 18.')",
      // Providers.
      "INSERT INTO providers (_id, name, iconRelativeUrl, colour) " +
        "VALUES ('lloyds', 'Lloyds Bank', NULL, '#006A4D')",
      "INSERT INTO providers (_id, name, iconRelativeUrl, colour) " +
        "VALUES ('vanguard', 'Vanguard', NULL, '#8A1E2D')",
      "INSERT INTO providers (_id, name, iconRelativeUrl, colour) " +
        "VALUES ('nationwide', 'Nationwide', NULL, '#004B87')",
      "INSERT INTO providers (_id, name, iconRelativeUrl, colour) " +
        "VALUES ('hl', 'Hargreaves Lansdown', NULL, '#003A70')",
      // Products.
      "INSERT INTO products (_id, startTaxYear, endTaxYear, providerId, friendlyName, " +
        "productTypeCode, flexible) VALUES " +
        "('1', '2025/26', NULL, 'lloyds', 'Cash ISA 2025/26', 'cash', 0)",
      "INSERT INTO products (_id, startTaxYear, endTaxYear, providerId, friendlyName, " +
        "productTypeCode, flexible) VALUES " +
        "('2', '2025/26', NULL, 'vanguard', 'Stocks & Shares ISA', 'stocks', 0)",
      "INSERT INTO products (_id, startTaxYear, endTaxYear, providerId, friendlyName, " +
        "productTypeCode, flexible) VALUES " +
        "('3', '2025/26', NULL, 'nationwide', 'Help to Buy ISA', 'htb', 0)",
      "INSERT INTO products (_id, startTaxYear, endTaxYear, providerId, friendlyName, " +
        "productTypeCode, flexible) VALUES " +
        "('4', '2025/26', NULL, 'hl', 'Junior ISA', 'junior', 0)",
      // Annual balances for the current tax year.
      "INSERT INTO annualBalances (productId, rulesetId, lastUpdatedDateUnix, " +
        "deductedFromAllowancePence) VALUES ('1', '2025/26', 0, 0)",
      "INSERT INTO annualBalances (productId, rulesetId, lastUpdatedDateUnix, " +
        "deductedFromAllowancePence) VALUES ('2', '2025/26', 0, 0)",
      "INSERT INTO annualBalances (productId, rulesetId, lastUpdatedDateUnix, " +
        "deductedFromAllowancePence) VALUES ('3', '2025/26', 0, 0)",
      "INSERT INTO annualBalances (productId, rulesetId, lastUpdatedDateUnix, " +
        "deductedFromAllowancePence) VALUES ('4', '2025/26', 0, 0)",
    )
}
