import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktlint)
}

// Release signing is configured from keystore.properties (gitignored) or environment variables.
// Contributors without either still get an unsigned release build.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
  Properties().apply {
    if (keystorePropertiesFile.exists()) {
      keystorePropertiesFile.inputStream().use { load(it) }
    }
  }

fun signingValue(environmentVariable: String, property: String): String? =
  System.getenv(environmentVariable)?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("ISA_EYE_KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingValue("ISA_EYE_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("ISA_EYE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("ISA_EYE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning =
  listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null }

android {
    namespace = "me.dylmye.isa"
    compileSdk = 37
    defaultConfig {
        applicationId = "me.dylmye.isa"
        minSdk = 24
        targetSdk = 37
        versionCode = 14
        versionName = "2.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Coverage for the local unit tests. The instrumented tests are compile-only here, so
            // there is nothing to instrument for those.
            enableUnitTestCoverage = true
        }
    }

    testCoverage {
        // Pinned rather than left to AGP's default, so the agent and the reporting tool agree and a
        // report can be reproduced.
        jacocoVersion = libs.versions.jacoco.get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
      // The SQLCipher libraries ship prebuilt. A machine with an NDK installed has AGP strip them,
      // while one without ships them untouched, so the same source would produce different bytes.
      // Keeping the symbols unconditionally makes the artifact independent of the build host.
      jniLibs {
        keepDebugSymbols += "**/*.so"
      }
    }

    // Compose Preview Screenshot Testing. The suite renders previews from `src/screenshotTest`
    // host-side with layoutlib, so no device is needed.
    testOptions {
      screenshotTests {
        create("screenshotTest") {
          engineVersion = libs.versions.screenshot.get()
          targetVariants.add("debug")
          dependencies {
            implementation(libs.screenshot.validation.api)
          }
        }
      }
    }
}

kotlin {
    jvmToolchain(17)
}

// The screenshot suite renders every preview in one JVM and keeps the images alive until they are
// written, so the 512 MB a test worker gets by default runs out long before the suite does. At a
// couple of hundred previews 2 GB is already marginal, so this leaves room for the suite to grow.
tasks.withType<Test>().configureEach {
  maxHeapSize = "3g"
}

detekt {
  buildUponDefaultConfig.set(true)
  config.setFrom(rootProject.file("config/detekt/detekt.yml"))
  parallel.set(true)
}

// Generated Material Symbols vectors are not hand-authored; leave them out of analysis.
tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
  exclude("**/ui/icons/**")
}

ktlint {
  // Pinned to the ktlint version bundled by detekt's `ktlint` rule set (2.0.0-alpha.6)
  // so the standalone checks and detekt agree.
  version.set("1.8.0")
  filter {
    exclude("**/ui/icons/**")
  }
}

ksp {
  // Export Room schemas so migrations can be diffed/tested.
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.documentfile)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  // Material 3 XML theme (DayNight window background); the UI itself is Compose Material 3.
  implementation(libs.material)

  // Adaptive
  implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
  implementation(libs.androidx.compose.material3.adaptive.navigation3)

  // Charts
  implementation(libs.vico.compose.m3)

  // Home screen widgets. Glance renders RemoteViews from Compose-like code and needs no Play
  // Services, so it stays compatible with an F-Droid build.
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.kotlinx.coroutines.test)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room (SQLite persistence) + SQLCipher encryption at rest
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.sqlcipher.android)
  ksp(libs.androidx.room.compiler)

  // Background reminder checks. WorkManager schedules through the platform JobScheduler and has no
  // Play Services dependency, so it stays compatible with an F-Droid build.
  implementation(libs.androidx.work.runtime.ktx)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)
  implementation(libs.coil.svg)

  // Static analysis
  detektPlugins(libs.detekt.rules.ktlint.wrapper)
}
