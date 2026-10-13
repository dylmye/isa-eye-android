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
public val balance: ImageVector
  get() {
    if (_balance != null) {
      return _balance!!
    }
    _balance =
      ImageVector.Builder(
          name = "balance",
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
            moveTo(2f, 21f)
            verticalLineTo(19f)
            horizontalLineToRelative(9f)
            verticalLineTo(7.82f)
            quadTo(10.35f, 7.6f, 9.88f, 7.13f)
            reflectiveQuadTo(9.18f, 6f)
            horizontalLineTo(6f)
            lineToRelative(3f, 7f)
            quadToRelative(0f, 1.25f, -1.02f, 2.13f)
            reflectiveQuadTo(5.5f, 16f)
            reflectiveQuadTo(3.03f, 15.13f)
            reflectiveQuadTo(2f, 13f)
            lineTo(5f, 6f)
            horizontalLineTo(3f)
            verticalLineTo(4f)
            horizontalLineTo(9.18f)
            quadTo(9.48f, 3.13f, 10.25f, 2.56f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(1.75f, 0.56f)
            quadTo(14.53f, 3.13f, 14.83f, 4f)
            horizontalLineTo(21f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            lineToRelative(3f, 7f)
            quadToRelative(0f, 1.25f, -1.02f, 2.13f)
            reflectiveQuadTo(18.5f, 16f)
            reflectiveQuadTo(16.03f, 15.13f)
            reflectiveQuadTo(15f, 13f)
            lineTo(18f, 6f)
            horizontalLineTo(14.83f)
            quadTo(14.6f, 6.65f, 14.13f, 7.13f)
            reflectiveQuadTo(13f, 7.82f)
            verticalLineTo(19f)
            horizontalLineToRelative(9f)
            verticalLineToRelative(2f)
            horizontalLineTo(2f)
            close()
            moveTo(16.63f, 13f)
            horizontalLineToRelative(3.75f)
            lineTo(18.5f, 8.65f)
            lineTo(16.63f, 13f)
            close()
            moveToRelative(-13f, 0f)
            horizontalLineTo(7.38f)
            lineTo(5.5f, 8.65f)
            lineTo(3.63f, 13f)
            close()
            moveTo(12f, 6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(13f, 5.43f, 13f, 5f)
            reflectiveQuadTo(12.71f, 4.29f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(11.29f, 4.29f)
            reflectiveQuadTo(11f, 5f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 6f)
            close()
          }
        }
        .build()
    return _balance!!
  }

private var _balance: ImageVector? = null
