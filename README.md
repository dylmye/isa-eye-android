# ISA Eye - Budget Your ISA - Android app
## [Web & Infra](https://github.com/dylmye/isa-eye) | [Android](https://github.com/dylmye/isa-eye-android)

A privacy-first app for keeping track of your ISA allowance.

Keep an eye on Indiviual Saving Account balances across your bank accounts, investment managers and more. It's easy to make sure you don't go over your allowances - just add your accounts and keep your contributions up to date.

## Development

### Setup

Ensure you have:

- **OpenJDK 17**
- The **Android SDK** installed, with `local.properties` pointing at it (`sdk.dir=...`) or `ANDROID_HOME` set
- **GraphicsMagick** for icon generation

Install the git hooks once per clone:

```sh
git config core.hooksPath .githooks
```

### Useful commands

```sh
# Build and install on a connected device/emulator
./gradlew :app:installDebug

# Full verification gate: build, unit tests, instrumented-test compile, screenshot tests,
# detekt, ktlint, Android Lint
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest \
  :app:testScreenshotTestDefaultDebugTestSuite :app:detekt :app:ktlintCheck :app:lintDebug

# Just the static analysis (what the pre-commit hook runs)
./gradlew :app:ktlintCheck :app:detekt

# Unit tests
./gradlew :app:testDebugUnitTest

# Local unit-test coverage (HTML and XML under app/build/reports/coverage)
./gradlew :app:createDebugUnitTestCoverageReport

# Instrumented tests (needs a device/emulator)
./gradlew :app:connectedDebugAndroidTest
```

These are also set up as tasks in VS Code.

### Screenshot tests

The UI is covered by [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing-with-testsuites),
which renders the previews in `app/src/screenshotTest/` host-side with layoutlib - no device or
emulator needed.

```sh
# Compare against the committed reference images
./gradlew :app:testScreenshotTestDefaultDebugTestSuite

# Re-record them after an intended UI change
./gradlew :app:updateScreenshotTestDefaultDebugTestSuite
```

Reference images live in `app/src/screenshotTestDefaultDebug/reference/` and are **committed** -
they are the golden files. A failing run therefore means either a regression or a change you meant
to make and need to re-record. Every preview carries `@PreviewTest`; the window sizes, themes and
font scales come from the multi-preview annotations in `WindowSizePreviews.kt`.

### Debugging on device

The debug build disables Android Auto Backup, so uninstalling resets local state:

```sh
# Wipe the app's data without uninstalling
adb shell pm clear me.dylmye.isa

# Follow just this app's logs
adb logcat --pid=$(adb shell pidof -s me.dylmye.isa)

# Launch without reinstalling
adb shell am start -n me.dylmye.isa/.MainActivity
```

### Database (Room)

- Schemas are exported to `app/schemas/` (`exportSchema = true`) and **committed** - they are the migration history and are required for migration tests.
- When you change entities: bump `@Database(version = ...)` and add a `Migration` (or an `@AutoMigration`). Never change the schema without bumping the version.
- That rule has one exception in the history: the product-type colour column landed before
  v0.1.0, when nothing had shipped, so it was covered by `fallbackToDestructiveMigration`
  rather than a migration. From v0.1.0 onward, a schema change needs a real migration.
- Reference data is seeded from `app/src/main/assets/seed.sql` on first create and whenever `SeedData.SEED_VERSION` changes.

### Updating seed data

`seed/*.ts` are vendored from the [dylmye/isa-eye](https://github.com/dylmye/isa-eye) repo (`db/seedData`). To update:

1. Update the files in the web repo first, then copy them into `seed/`.
2. Regenerate the SQL asset (requires Node 22.6+, which strips TypeScript types natively):
   ```sh
   node scripts/generate-seed.mjs
   ```
3. Bump `SeedData.SEED_VERSION` so existing installs re-apply the seed.

### Launcher icons

Regenerate the PNG launcher icons:

```sh
scripts/generate-launcher-icons.sh
```

## License

ISC
