package me.dylmye.isa.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.formatGbp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/** One sector of a ring: the colour to paint it, the name to call it and what it amounts to. */
internal data class DonutSlice(val colour: Color, val label: String, val pence: Long)

/** A pressed sector, and where the press landed so its tooltip can be anchored there. */
internal data class DonutSelection(val index: Int, val tap: Offset)

/**
 * A ring split in proportion to [slices], starting at twelve o'clock so the largest slice reads
 * first. The ring is drawn as a circle centred in whatever space it is given, since a card is wider
 * than it is tall and drawing into the full canvas would stretch it into an ellipse. [description]
 * is what a screen reader hears in place of the ring itself.
 *
 * Pressing a sector reports it through [onSelect], which the card uses to show a tooltip with that
 * slice's name and amount; pressing the hole or the space around the ring clears the selection.
 */
@Composable
internal fun Donut(
  slices: List<DonutSlice>,
  description: String,
  selection: DonutSelection?,
  onSelect: (DonutSelection?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val total = slices.sumOf { it.pence }
  val selected = selection?.takeIf { it.index in slices.indices }?.index
  Layout(
    modifier = modifier.semantics { contentDescription = description },
    content = {
      Canvas(
        modifier =
          Modifier.fillMaxSize().pointerInput(slices, total) {
            detectTapGestures { tap ->
              val index =
                sliceIndexAt(slices, total, Size(size.width.toFloat(), size.height.toFloat()), tap)
              onSelect(index?.let { DonutSelection(it, tap) })
            }
          },
      ) {
        drawRing(slices = slices, total = total, selected = selected)
      }
      if (selected != null) {
        DonutTooltip(slice = slices[selected], total = total)
      }
    },
  ) { measurables, constraints ->
    val canvas = measurables.first().measure(constraints)
    val margin = TOOLTIP_MARGIN.roundToPx()
    val tooltip =
      measurables.getOrNull(1)?.measure(
        Constraints(maxWidth = (canvas.width - 2 * margin).coerceAtLeast(0)),
      )
    layout(canvas.width, canvas.height) {
      canvas.place(0, 0)
      if (tooltip != null && selection != null) {
        val maxX = (canvas.width - tooltip.width - margin).coerceAtLeast(margin)
        val maxY = (canvas.height - tooltip.height - margin).coerceAtLeast(margin)
        val x = (selection.tap.x - tooltip.width / 2f).roundToInt().coerceIn(margin, maxX)
        val above = (selection.tap.y - tooltip.height - TOOLTIP_GAP.roundToPx()).roundToInt()
        val y =
          if (above >= margin) {
            above.coerceIn(margin, maxY)
          } else {
            (selection.tap.y + TOOLTIP_GAP.roundToPx()).roundToInt().coerceIn(margin, maxY)
          }
        tooltip.place(x, y)
      }
    }
  }
}

/**
 * The sector at [tap], or null when the tap misses the ring — the hole, a corner or the space
 * around it. [size] is the drawing area the ring is centred in.
 */
internal fun sliceIndexAt(slices: List<DonutSlice>, total: Long, size: Size, tap: Offset): Int? {
  if (total <= 0L) return null
  val side = size.minDimension
  val stroke = side * RING_THICKNESS
  val vector = tap - Offset(size.width / 2, size.height / 2)
  val distance = vector.getDistance()
  val angle = atan2(vector.y, vector.x) * RADIANS_TO_DEGREES
  var index: Int? = null
  if (distance in side / 2 - stroke..side / 2) {
    var start = START_ANGLE
    for ((i, slice) in slices.withIndex()) {
      val sweep = slice.pence.toFloat() / total * FULL_CIRCLE
      var delta = (angle - start) % FULL_CIRCLE
      if (delta < 0f) delta += FULL_CIRCLE
      if (delta < sweep) {
        index = i
        break
      }
      start += sweep
    }
  }
  return index
}

/** Paints [slices] around the middle of the area, fading all but the [selected] one. */
private fun DrawScope.drawRing(slices: List<DonutSlice>, total: Long, selected: Int?) {
  if (total <= 0L) return
  val side = size.minDimension
  val stroke = side * RING_THICKNESS
  val topLeft =
    Offset(
      x = (size.width - side) / 2 + stroke / 2,
      y = (size.height - side) / 2 + stroke / 2,
    )
  var startAngle = START_ANGLE
  slices.forEachIndexed { index, slice ->
    val sweep = slice.pence.toFloat() / total * FULL_CIRCLE
    drawArc(
      color = slice.colour,
      startAngle = startAngle,
      sweepAngle = sweep,
      useCenter = false,
      topLeft = topLeft,
      size = Size(side - stroke, side - stroke),
      style = Stroke(width = stroke),
      // Everything but the pressed sector fades, so the tooltip and the ring agree on what is
      // being described.
      alpha = if (selected != null && selected != index) UNSELECTED_ALPHA else 1f,
    )
    startAngle += sweep
  }
}

/** The pressed sector's name and amount, floating near the finger that pressed it. */
@Composable
private fun DonutTooltip(slice: DonutSlice, total: Long) {
  Surface(
    color = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    shape = MaterialTheme.shapes.small,
    shadowElevation = 4.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = slice.label,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "${formatGbp(slice.pence)} · ${percentOf(slice.pence, total)}",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

/** A colour swatch, a product type and its share of the year. */
@Composable
internal fun LegendRow(colour: Color, label: String, value: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      color = colour,
      shape = CircleShape,
      modifier = Modifier.size(SWATCH_SIZE).clearAndSetSemantics {},
    ) {}
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * One series per product type: its contribution in each year, in whole pounds so the axis picks
 * round pound steps, and zero where the type was not used.
 */
internal fun seriesValues(years: List<YearShare>, typeId: String): List<Number> =
  years.map<YearShare, Number> { year ->
    (year.byType.firstOrNull { it.productTypeId == typeId }?.pence ?: 0L) / 100.0
  }

/** Spoken summary of the by-account donut, naming the total the legend's shares are taken from. */
internal fun accountDonutDescription(uiState: InsightsUiState): String {
  val count = uiState.accountShares.size
  val accounts = if (count == 1) "1 account" else "$count accounts"
  val total = uiState.currentYearTotalPence
  return uiState.accountShares.joinToString(
    prefix = "${formatGbp(total)} this year across $accounts: ",
    separator = ", ",
  ) { share ->
    "${share.name} ${formatGbp(share.pence)} (${percentOf(share.pence, total)})"
  }
}

/** Spoken summary of the by-type donut, which the legend conveys visually. */
internal fun typeDonutDescription(uiState: InsightsUiState): String {
  val total = uiState.currentYearTotalPence
  return uiState.typeShares.joinToString(
    prefix = "${formatGbp(total)} this year: ",
    separator = ", ",
  ) { share ->
    "${share.name} ${formatGbp(share.pence)} (${percentOf(share.pence, total)})"
  }
}

/** Spoken summary of the bar chart, whose axis labels a screen reader cannot follow. */
internal fun barDescription(uiState: InsightsUiState): String =
  uiState.yearShares.joinToString(prefix = "Contributions by year: ", separator = ", ") { year ->
    val byType =
      year.byType.joinToString(separator = ", ") { type ->
        "${type.name} ${formatGbp(type.pence)}"
      }
    "${year.rulesetId} ${formatGbp(year.totalPence)} ($byType)"
  }

/** [pence] as a whole percentage of [total], for the legend and the spoken summaries. */
internal fun percentOf(pence: Long, total: Long): String =
  if (total <= 0L) "0%" else "${(pence * 100.0 / total).roundToInt()}%"

/**
 * Axis labels in pounds, thousands abbreviated to one decimal so the value is never misstated —
 * rounding to whole thousands would render £1,200 as "£1k".
 */
internal fun axisMoney(pounds: Double): String {
  if (pounds < THOUSANDS) return "£${pounds.roundToInt()}"
  val thousands = (pounds / THOUSANDS * 10).roundToInt() / 10.0
  return if (thousands % 1.0 == 0.0) "£${thousands.roundToInt()}k" else "£${thousands}k"
}

private val SWATCH_SIZE = 12.dp
private val TOOLTIP_MARGIN = 4.dp
private val TOOLTIP_GAP = 8.dp
private const val RING_THICKNESS = 0.28f
private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val UNSELECTED_ALPHA = 0.35f
private const val THOUSANDS = 1000.0
private val RADIANS_TO_DEGREES = (180.0 / PI).toFloat()
