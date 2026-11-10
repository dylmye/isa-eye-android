package me.dylmye.isa.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ISAEyeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
      ISAEyeDarkColorScheme
    } else {
      ISAEyeLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    shapes = ISAEyeShapes,
    typography = Typography,
    content = content,
  )
}
