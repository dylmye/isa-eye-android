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
public val trendingDown: ImageVector
  get() {
    if (_trendingDown != null) {
      return _trendingDown!!
    }
    _trendingDown =
      ImageVector.Builder(
          name = "trendingDown",
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
            moveTo(16f, 18f)
            verticalLineTo(16f)
            horizontalLineToRelative(2.6f)
            lineTo(13.4f, 10.85f)
            lineToRelative(-4f, 4f)
            lineTo(2f, 7.4f)
            lineTo(3.4f, 6f)
            lineToRelative(6f, 6f)
            lineToRelative(4f, -4f)
            lineTo(20f, 14.6f)
            verticalLineTo(12f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(6f)
            horizontalLineTo(16f)
            close()
          }
        }
        .build()
    return _trendingDown!!
  }

private var _trendingDown: ImageVector? = null
