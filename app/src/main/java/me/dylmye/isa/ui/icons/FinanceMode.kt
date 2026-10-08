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
public val financeMode: ImageVector
  get() {
    if (_financeMode != null) {
      return _financeMode!!
    }
    _financeMode =
      ImageVector.Builder(
          name = "financeMode",
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
            moveTo(8f, 13.65f)
            verticalLineTo(6f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(7.65f)
            lineTo(9.5f, 12.25f)
            lineTo(8f, 13.65f)
            close()
            moveToRelative(5f, 1.5f)
            verticalLineTo(2f)
            horizontalLineToRelative(3f)
            verticalLineTo(12.15f)
            lineToRelative(-3f, 3f)
            close()
            moveTo(3f, 18.6f)
            verticalLineTo(10f)
            horizontalLineTo(6f)
            verticalLineToRelative(5.6f)
            lineToRelative(-3f, 3f)
            close()
            moveToRelative(0f, 2.45f)
            lineTo(9.45f, 14.6f)
            lineTo(13f, 17.65f)
            lineToRelative(5.6f, -5.6f)
            horizontalLineTo(17f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(5f)
            horizontalLineTo(20f)
            verticalLineToRelative(-1.6f)
            lineToRelative(-6.9f, 6.9f)
            lineTo(9.55f, 17.3f)
            lineTo(5.8f, 21.05f)
            horizontalLineTo(3f)
            close()
          }
        }
        .build()
    return _financeMode!!
  }

private var _financeMode: ImageVector? = null
