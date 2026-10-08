// Generates app/src/main/assets/seed.sql from the vendored web-app seed data in seed/.
//
// Run with Node (type-stripping handles the .ts sources):
//   node scripts/generate-seed.mjs
//
// Only reference data is seeded (providers + aliases, product types, rulesets + exceptions).
// User data (products, balances) is not seeded.

import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import banks from "../seed/banks.ts";
import isaTypes from "../seed/isaTypes.ts";
import rulesets from "../seed/rulesets.ts";

const sqlString = (value) =>
  value === undefined || value === null ? "NULL" : `'${String(value).replace(/'/g, "''")}'`;

const statements = [];

// Rulesets first (referenced by product types and ruleset exceptions).
for (const ruleset of rulesets) {
  statements.push(
    "INSERT INTO rulesets (_id, sharedAllowancePence, startDate, endDate, notes) VALUES " +
      `(${sqlString(ruleset.name)}, ${ruleset.sharedAllowancePence}, ${sqlString(ruleset.startDate)}, ` +
      `${sqlString(ruleset.endDate)}, ${sqlString(ruleset.notes)});`,
  );
}

// Product types.
for (const type of isaTypes) {
  statements.push(
    "INSERT INTO productTypes (_id, name, introducedWithRuleset, removedWithRuleset, " +
      "shortDescription, longDescription) VALUES " +
      `(${sqlString(type.code)}, ${sqlString(type.name)}, ${sqlString(type.introducedWithRuleset)}, ` +
      `${sqlString(type.removedWithRuleset)}, ${sqlString(type.shortDescription)}, ` +
      `${sqlString(type.longDescription)});`,
  );
}

// Providers and their aliases.
for (const bank of banks) {
  statements.push(
    "INSERT INTO providers (_id, name, iconRelativeUrl, colour) VALUES " +
      `(${sqlString(bank.id)}, ${sqlString(bank.name)}, ${sqlString(bank.iconRelativeUrl)}, ` +
      `${sqlString(bank.colour ?? "#ffffff")});`,
  );
  (bank.aliases ?? []).forEach((alias, index) => {
    statements.push(
      "INSERT INTO providerAliases (_id, alias, providerId) VALUES " +
        `(${sqlString(`${bank.id}-${index}`)}, ${sqlString(alias)}, ${sqlString(bank.id)});`,
    );
  });
}

// Ruleset exceptions (product-type-specific allowances).
for (const ruleset of rulesets) {
  for (const exception of ruleset.productSpecificRulesets ?? []) {
    statements.push(
      "INSERT INTO rulesetExceptions (_id, productTypeId, rulesetId, allowancePence, notes, " +
        "includedInShared) VALUES " +
        `(${sqlString(`${ruleset.name}-${exception.code}`)}, ${sqlString(exception.code)}, ` +
        `${sqlString(ruleset.name)}, ${exception.allowancePence}, ${sqlString(exception.notes)}, ` +
        `${exception.includedInOverall ? 1 : 0});`,
    );
  }
}

const output = fileURLToPath(new URL("../app/src/main/assets/seed.sql", import.meta.url));
mkdirSync(fileURLToPath(new URL("../app/src/main/assets", import.meta.url)), { recursive: true });
writeFileSync(output, statements.join("\n") + "\n");

console.log(`Wrote ${statements.length} statements to ${output}`);
