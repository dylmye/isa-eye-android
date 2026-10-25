package me.dylmye.isa.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * The original thick progress bar: a full pill track with the fill clipped straight at its leading
 * edge. Keeping the leading edge square avoids the double-rounded end that made short bars look
 * odd at 2x2.
 */
internal fun progressBar(
  fraction: Float,
  widthPx: Int,
  heightPx: Int,
  track: Int,
  fill: Int,
): Bitmap {
  val width = widthPx.coerceAtLeast(2)
  val height = heightPx.coerceAtLeast(2)
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val radius = height / 2f
  val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  paint.color = track
  canvas.drawRoundRect(bounds, radius, radius, paint)
  val filled = fraction.coerceIn(0f, 1f)
  if (filled > 0f) {
    canvas.save()
    canvas.clipRect(0f, 0f, width * filled, height.toFloat())
    paint.color = fill
    canvas.drawRoundRect(bounds, radius, radius, paint)
    canvas.restore()
  }
  return bitmap
}

/**
 * Draws a progress ring starting at twelve o'clock. The remaining track is a full circle and the
 * filled share is laid over it with rounded caps, so the transition is shaped by the progress end
 * rather than cut by a gap. The centre stays transparent for the widget's own text.
 */
internal fun progressRing(
  fraction: Float,
  sizePx: Int,
  strokePx: Int,
  track: Int,
  fill: Int,
): Bitmap {
  val size = sizePx.coerceAtLeast(4)
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val stroke = strokePx.coerceIn(1, size / 2)
  val inset = stroke / 2f
  val bounds = RectF(inset, inset, size - inset, size - inset)
  val paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = stroke.toFloat()
      strokeCap = Paint.Cap.ROUND
    }
  paint.color = track
  canvas.drawArc(bounds, 0f, 360f, false, paint)
  val filled = fraction.coerceIn(0f, 1f)
  if (filled > 0f) {
    paint.color = fill
    canvas.drawArc(bounds, -90f, 360f * filled, false, paint)
  }
  return bitmap
}

/**
 * The card that is itself the progress bar: a [fraction] of the card is tinted from the bottom up,
 * with a crisp edge so the fill reads as a fill rather than a gradient. The card colour stays behind
 * it so the percentage on top keeps its contrast in either theme.
 */
internal fun progressCard(
  widthPx: Int,
  heightPx: Int,
  fraction: Float,
  card: Int,
  fill: Int,
): Bitmap {
  val width = widthPx.coerceAtLeast(1)
  val height = heightPx.coerceAtLeast(1)
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  canvas.drawColor(card)
  val filled = fraction.coerceIn(0f, 1f)
  if (filled <= 0f) return bitmap
  val top = height * (1f - filled)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  paint.color = fill
  paint.alpha = FILL_ALPHA
  canvas.drawRect(0f, top, width.toFloat(), height.toFloat(), paint)
  return bitmap
}

/** A filled circle for a widget badge, drawn rather than clipped so it is round before Android 12. */
internal fun badgeCircle(sizePx: Int, colour: Int): Bitmap {
  val size = sizePx.coerceAtLeast(2)
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  paint.color = colour
  canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
  return bitmap
}

private const val FILL_ALPHA = 76
