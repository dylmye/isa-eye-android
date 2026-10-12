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
public val quickReference: ImageVector
  get() {
    if (_quickReference != null) {
      return _quickReference!!
    }
    _quickReference =
      ImageVector.Builder(
          name = "quickReference",
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
            moveTo(16.5f, 20f)
            horizontalLineToRelative(1f)
            verticalLineTo(16f)
            horizontalLineToRelative(-1f)
            verticalLineToRelative(4f)
            close()
            moveTo(17f, 15f)
            quadToRelative(0.2f, 0f, 0.35f, -0.15f)
            reflectiveQuadTo(17.5f, 14.5f)
            reflectiveQuadTo(17.35f, 14.15f)
            reflectiveQuadTo(17f, 14f)
            reflectiveQuadToRelative(-0.35f, 0.15f)
            reflectiveQuadTo(16.5f, 14.5f)
            reflectiveQuadToRelative(0.15f, 0.35f)
            reflectiveQuadTo(17f, 15f)
            close()
            moveTo(5f, 4f)
            verticalLineTo(9f)
            verticalLineToRelative(1f)
            reflectiveQuadToRelative(0f, 2.05f)
            reflectiveQuadTo(5f, 17f)
            quadToRelative(0f, 0.8f, 0f, 1.56f)
            reflectiveQuadTo(5f, 20f)
            verticalLineTo(4f)
            verticalLineTo(9f)
            verticalLineTo(4f)
            close()
            moveTo(7f, 14f)
            horizontalLineToRelative(3.68f)
            quadToRelative(0.28f, -0.58f, 0.64f, -1.08f)
            reflectiveQuadTo(12.13f, 12f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveToRelative(0f, 4f)
            horizontalLineToRelative(3.08f)
            quadTo(10f, 17.5f, 10f, 17f)
            reflectiveQuadToRelative(0.08f, -1f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(5f, 22f)
            quadTo(4.18f, 22f, 3.59f, 21.41f)
            reflectiveQuadTo(3f, 20f)
            verticalLineTo(4f)
            quadTo(3f, 3.17f, 3.59f, 2.59f)
            reflectiveQuadTo(5f, 2f)
            horizontalLineToRelative(8f)
            lineToRelative(6f, 6f)
            verticalLineToRelative(2.3f)
            quadTo(18.53f, 10.15f, 18.03f, 10.07f)
            reflectiveQuadTo(17f, 10f)
            verticalLineTo(9f)
            horizontalLineTo(12f)
            verticalLineTo(4f)
            horizontalLineTo(5f)
            verticalLineTo(20f)
            horizontalLineToRelative(5.68f)
            quadToRelative(0.28f, 0.57f, 0.64f, 1.07f)
            reflectiveQuadTo(12.13f, 22f)
            horizontalLineTo(5f)
            close()
            moveTo(20.54f, 13.46f)
            quadTo(22f, 14.93f, 22f, 17f)
            reflectiveQuadToRelative(-1.46f, 3.54f)
            reflectiveQuadTo(17f, 22f)
            reflectiveQuadTo(13.46f, 20.54f)
            reflectiveQuadTo(12f, 17f)
            reflectiveQuadToRelative(1.46f, -3.54f)
            reflectiveQuadTo(17f, 12f)
            reflectiveQuadToRelative(3.54f, 1.46f)
            close()
          }
        }
        .build()
    return _quickReference!!
  }

private var _quickReference: ImageVector? = null
