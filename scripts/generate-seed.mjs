// Generates app/src/main/assets/seed.sql from the vendored web-app seed data in seed/.
//
// Run with Node (type-stripping handles the .ts sources):
//   node scripts/generate-seed.mjs
//
// Only reference data is seeded (providers + aliases, product types, rulesets + exceptions).
// User data (products, balances) is not seeded.
//
// Each row is emitted as an idempotent upsert (`INSERT OR IGNORE` + `UPDATE`), so the same file
// can seed a fresh database and refresh an existing one without `INSERT OR REPLACE`'s
// delete-then-insert semantics (which would trigger ON DELETE CASCADE).
//
// Bump SeedData.SEED_VERSION when this data changes so existing installs re-run the seed.

import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import banks from "../seed/banks.ts";
import isaTypes from "../seed/isaTypes.ts";
import rulesets from "../seed/rulesets.ts";

const sqlString = (value) =>
  value === undefined || value === null ? "NULL" : `'${String(value).replace(/'/g, "''")}'`;

const statements = [];

/**
 * Emits an idempotent upsert for a row keyed by `_id`.
 * @param table table name
 * @param id already-quoted SQL literal for the primary key
 * @param columns array of `[columnName, sqlLiteral]` for the non-key columns
 */
const upsert = (table, id, columns) => {
  const names = ["_id", ...columns.map(([name]) => name)].join(", ");
  const values = [id, ...columns.map(([, value]) => value)].join(", ");
  statements.push(`INSERT OR IGNORE INTO ${table} (${names}) VALUES (${values});`);
  const assignments = columns.map(([name, value]) => `${name} = ${value}`).join(", ");
  statements.push(`UPDATE ${table} SET ${assignments} WHERE _id = ${id};`);
};

// Rulesets first (referenced by product types and ruleset exceptions).
for (const ruleset of rulesets) {
  upsert("rulesets", sqlString(ruleset.name), [
    ["sharedAllowancePence", ruleset.sharedAllowancePence],
    ["startDate", sqlString(ruleset.startDate)],
    ["endDate", sqlString(ruleset.endDate)],
    ["notes", sqlString(ruleset.notes)],
  ]);
}

// Product types.
for (const type of isaTypes) {
  upsert("productTypes", sqlString(type.code), [
    ["name", sqlString(type.name)],
    ["introducedWithRuleset", sqlString(type.introducedWithRuleset)],
    ["removedWithRuleset", sqlString(type.removedWithRuleset)],
    ["shortDescription", sqlString(type.shortDescription)],
    ["longDescription", sqlString(type.longDescription)],
  ]);
}

// Providers and their aliases. Alias ids are content-based so they stay stable if the alias order
// in the seed changes.
for (const bank of banks) {
  upsert("providers", sqlString(bank.id), [
    ["name", sqlString(bank.name)],
    ["iconRelativeUrl", sqlString(bank.iconRelativeUrl)],
    ["colour", sqlString(bank.colour ?? "#ffffff")],
  ]);
  for (const alias of bank.aliases ?? []) {
    upsert("providerAliases", sqlString(`${bank.id}-${alias}`), [
      ["alias", sqlString(alias)],
      ["providerId", sqlString(bank.id)],
    ]);
  }
}

// Ruleset exceptions (product-type-specific allowances).
for (const ruleset of rulesets) {
  for (const exception of ruleset.productSpecificRulesets ?? []) {
    upsert("rulesetExceptions", sqlString(`${ruleset.name}-${exception.code}`), [
      ["productTypeId", sqlString(exception.code)],
      ["rulesetId", sqlString(ruleset.name)],
      ["allowancePence", exception.allowancePence],
      ["notes", sqlString(exception.notes)],
      ["includedInShared", exception.includedInOverall ? 1 : 0],
    ]);
  }
}

const output = fileURLToPath(new URL("../app/src/main/assets/seed.sql", import.meta.url));
mkdirSync(fileURLToPath(new URL("../app/src/main/assets", import.meta.url)), { recursive: true });
writeFileSync(output, statements.join("\n") + "\n");

console.log(`Wrote ${statements.length} statements to ${output}`);
