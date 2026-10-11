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
public val trendingFlat: ImageVector
  get() {
    if (_trendingFlat != null) {
      return _trendingFlat!!
    }
    _trendingFlat =
      ImageVector.Builder(
          name = "trendingFlat",
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
            moveTo(17.5f, 16.5f)
            lineTo(16.08f, 15.1f)
            lineTo(18.18f, 13f)
            horizontalLineTo(3f)
            verticalLineTo(11f)
            horizontalLineTo(18.18f)
            lineTo(16.1f, 8.9f)
            lineTo(17.53f, 7.5f)
            lineTo(22f, 12f)
            lineToRelative(-4.5f, 4.5f)
            close()
          }
        }
        .build()
    return _trendingFlat!!
  }

private var _trendingFlat: ImageVector? = null
