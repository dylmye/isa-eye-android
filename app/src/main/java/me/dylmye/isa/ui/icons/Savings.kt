package me.dylmye.isa.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val savings: ImageVector
  get() {
    if (_savings != null) {
      return _savings!!
    }
    _savings =
      ImageVector.Builder(
          name = "savings",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(16.71f, 10.71f)
            quadTo(17f, 10.43f, 17f, 10f)
            quadTo(17f, 9.57f, 16.71f, 9.29f)
            reflectiveQuadTo(16f, 9f)
            reflectiveQuadTo(15.29f, 9.29f)
            reflectiveQuadTo(15f, 10f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(16f, 11f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            close()
            moveTo(8f, 9f)
            horizontalLineToRelative(5f)
            verticalLineTo(7f)
            horizontalLineTo(8f)
            verticalLineTo(9f)
            close()
            moveTo(4.5f, 21f)
            quadTo(3.65f, 18.15f, 2.83f, 15.31f)
            reflectiveQuadTo(2f, 9.5f)
            quadTo(2f, 7.2f, 3.6f, 5.6f)
            reflectiveQuadTo(7.5f, 4f)
            horizontalLineToRelative(5f)
            quadTo(13.23f, 3.05f, 14.26f, 2.52f)
            reflectiveQuadTo(16.5f, 2f)
            quadToRelative(0.63f, 0f, 1.06f, 0.44f)
            reflectiveQuadTo(18f, 3.5f)
            quadToRelative(0f, 0.13f, -0.13f, 0.57f)
            quadToRelative(-0.1f, 0.27f, -0.19f, 0.56f)
            reflectiveQuadTo(17.55f, 5.22f)
            lineTo(19.83f, 7.5f)
            horizontalLineTo(22f)
            verticalLineToRelative(6.98f)
            lineTo(19.18f, 15.4f)
            lineTo(17.5f, 21f)
            horizontalLineTo(12f)
            verticalLineTo(19f)
            horizontalLineTo(10f)
            verticalLineToRelative(2f)
            horizontalLineTo(4.5f)
            close()
          }
        }
        .build()
    return _savings!!
  }

private var _savings: ImageVector? = null
