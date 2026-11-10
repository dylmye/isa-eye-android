package me.dylmye.isa.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * ISA Eye's fallback colour schemes, generated with Material Theme Builder from the launcher icon's
 * brand green (#01C181). Every Material 3 role is set so that devices without dynamic colour, and
 * screenshots rendered with dynamicColor = false, show the brand palette instead of the Compose
 * template purple.
 */

internal val ISAEyeLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF006C46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF66FDB7),
    onPrimaryContainer = Color(0xFF002112),
    inversePrimary = Color(0xFF43E09C),
    secondary = Color(0xFF4E6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D7),
    onSecondaryContainer = Color(0xFF0B1F15),
    tertiary = Color(0xFF3C6472),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC0E9FA),
    onTertiaryContainer = Color(0xFF001F28),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    surfaceTint = Color(0xFF006C46),
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFEFF1ED),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C1),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFDF8),
    surfaceDim = Color(0xFFD9DBD6),
    surfaceContainer = Color(0xFFEDEEEA),
    surfaceContainerHigh = Color(0xFFE7E9E4),
    surfaceContainerHighest = Color(0xFFE1E3DF),
    surfaceContainerLow = Color(0xFFF2F4F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
  )

internal val ISAEyeDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF43E09C),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFF66FDB7),
    inversePrimary = Color(0xFF006C46),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF203529),
    secondaryContainer = Color(0xFF364B3F),
    onSecondaryContainer = Color(0xFFD0E8D7),
    tertiary = Color(0xFFA4CDDD),
    onTertiary = Color(0xFF053542),
    tertiaryContainer = Color(0xFF234C59),
    onTertiaryContainer = Color(0xFFC0E9FA),
    background = Color(0xFF191C1A),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF191C1A),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    surfaceTint = Color(0xFF43E09C),
    inverseSurface = Color(0xFFE1E3DF),
    inverseOnSurface = Color(0xFF2E312E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF373A37),
    surfaceDim = Color(0xFF111412),
    surfaceContainer = Color(0xFF1D201E),
    surfaceContainerHigh = Color(0xFF272B28),
    surfaceContainerHighest = Color(0xFF323633),
    surfaceContainerLow = Color(0xFF191C1A),
    surfaceContainerLowest = Color(0xFF0C0F0D),
  )
