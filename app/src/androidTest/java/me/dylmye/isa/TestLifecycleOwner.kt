package me.dylmye.isa

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A lifecycle owner the test can drive.
 *
 * [androidx.compose.ui.test.junit4.StateRestorationTester] disposes and rebuilds the composition
 * without touching the hosting Activity's lifecycle, which stays `RESUMED` throughout. A real
 * configuration change drops below `RESUMED` before the composition is torn down, and
 * `Modifier.restoreFocusAfterConfigChange` relies on that difference to tell an interactive focus
 * change from teardown. Tests that emulate a configuration change move this owner to `CREATED`
 * before restoring saved state and back to `RESUMED` afterwards.
 */
internal class TestLifecycleOwner : LifecycleOwner {
  override val lifecycle = LifecycleRegistry(this)

  init {
    lifecycle.currentState = Lifecycle.State.RESUMED
  }
}
