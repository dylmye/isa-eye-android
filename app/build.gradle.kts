plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktlint)
}

android {
    namespace = "me.dylmye.isa"
    compileSdk = 37
    defaultConfig {
        applicationId = "me.dylmye.isa"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
// written, so the 512 MB a test worker gets by default runs out once the suite grows past a couple
// of hundred previews.
tasks.withType<Test>().configureEach {
  maxHeapSize = "2g"
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

  // Room (SQLite persistence)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)
  implementation(libs.coil.svg)

  // Static analysis
  detektPlugins(libs.detekt.rules.ktlint.wrapper)
}
