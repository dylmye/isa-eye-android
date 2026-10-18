package me.dylmye.isa.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.formatGbp
import kotlin.math.roundToInt

/**
 * A ring split in proportion to [shares], starting at twelve o'clock so the largest slice reads
 * first. The ring is drawn as a circle centred in whatever space it is given, since a card is wider
 * than it is tall and drawing into the full canvas would stretch it into an ellipse. [description]
 * is what a screen reader hears in place of the ring itself.
 */
@Composable
internal fun Donut(
  shares: List<Pair<Color, Long>>,
  description: String,
  modifier: Modifier = Modifier,
) {
  val total = shares.sumOf { it.second }
  Canvas(modifier = modifier.semantics { contentDescription = description }) {
    val side = size.minDimension
    val stroke = side * RING_THICKNESS
    val topLeft =
      Offset(
        x = (size.width - side) / 2 + stroke / 2,
        y = (size.height - side) / 2 + stroke / 2,
      )
    var startAngle = START_ANGLE
    shares.forEach { (colour, pence) ->
      val sweep = pence.toFloat() / total * FULL_CIRCLE
      drawArc(
        color = colour,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = Size(side - stroke, side - stroke),
        style = Stroke(width = stroke),
      )
      startAngle += sweep
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
    Surface(color = colour, shape = CircleShape, modifier = Modifier.size(SWATCH_SIZE)) {}
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
    "${share.name} ${percentOf(share.pence, total)}"
  }
}

/** Spoken summary of the by-type donut, which the legend conveys visually. */
internal fun typeDonutDescription(uiState: InsightsUiState): String {
  val total = uiState.currentYearTotalPence
  return uiState.typeShares.joinToString(
    prefix = "${formatGbp(total)} this year: ",
    separator = ", ",
  ) { share ->
    "${share.name} ${percentOf(share.pence, total)}"
  }
}

/** Spoken summary of the bar chart, whose axis labels a screen reader cannot follow. */
internal fun barDescription(uiState: InsightsUiState): String =
  uiState.yearShares.joinToString(prefix = "Contributions by year: ", separator = ", ") { year ->
    "${year.rulesetId} ${formatGbp(year.totalPence)}"
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
private const val RING_THICKNESS = 0.28f
private const val START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private const val THOUSANDS = 1000.0
