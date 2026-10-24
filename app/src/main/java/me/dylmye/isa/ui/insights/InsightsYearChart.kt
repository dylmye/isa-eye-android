package me.dylmye.isa.ui.insights

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.LegendItem
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.toTint

/**
 * The by-year column chart: one stack per year with a series per product type, a legend naming the
 * types, and a marker that names the pressed year and its total. The model is built by the caller,
 * because a producer would animate its first model in and a screenshot would miss it.
 */
@Composable
internal fun rememberYearChart(years: List<YearShare>, types: List<TypeShare>): CartesianChart {
  val colours = types.associate { it.productTypeId to it.colour.toTint() }
  // Built here rather than in the legend's item lambda, which is not composable.
  val legendLabel =
    rememberTextComponent(TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))
  return rememberCartesianChart(
    rememberColumnCartesianLayer(
      columnProvider =
        ColumnCartesianLayer.ColumnProvider.series(
          types.map { type ->
            rememberLineComponent(
              fill = Fill(colours.getValue(type.productTypeId)),
              thickness = BAR_WIDTH,
            )
          },
        ),
      mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
    ),
    startAxis = VerticalAxis.rememberStart(valueFormatter = MoneyAxisFormatter),
    bottomAxis =
      HorizontalAxis.rememberBottom(
        valueFormatter =
          CartesianValueFormatter { _, x, _ -> years.getOrNull(x.toInt())?.label.orEmpty() },
      ),
    legend =
      rememberHorizontalLegend(
        items = { _ ->
          types.forEach { type ->
            add(
              LegendItem(
                ShapeComponent(Fill(colours.getValue(type.productTypeId)), CircleShape),
                legendLabel,
                type.name,
              ),
            )
          }
        },
      ),
    marker = rememberYearMarker(years),
    // A tap leaves the marker up until the same column is tapped again, so the figure can be read
    // without holding a finger on the chart.
    markerController = CartesianMarkerController.rememberToggleOnTap(),
  )
}

/** The tap marker: a single label naming the pressed year and what went in that year. */
@Composable
private fun rememberYearMarker(years: List<YearShare>): CartesianMarker {
  val label =
    rememberTextComponent(
      style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
      background =
        rememberShapeComponent(
          fill = Fill(MaterialTheme.colorScheme.surfaceContainerHighest),
          shape = MaterialTheme.shapes.small,
        ),
      padding = Insets(horizontal = 8.dp, vertical = 4.dp),
    )
  // The year and its total, not a line per series: the stack's parts are already named by the
  // legend, and the year is what the bar stands for.
  val formatter =
    remember(years) {
      DefaultCartesianMarker.ValueFormatter { _, targets ->
        val target = targets.filterIsInstance<ColumnCartesianLayerMarkerTarget>().firstOrNull()
        val x = target?.columns?.firstOrNull()?.entry?.x?.toInt() ?: -1
        years.getOrNull(x)?.let { "${it.rulesetId} · ${formatGbp(it.totalPence)}" }.orEmpty()
      }
    }
  return rememberDefaultCartesianMarker(
    label = label,
    valueFormatter = formatter,
    // Beside the pressed bar, not in a strip reserved at the top: the chart keeps its full height
    // and the label reads as a tooltip.
    labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
  )
}

private val MoneyAxisFormatter = CartesianValueFormatter { _, value, _ -> axisMoney(value) }

private val BAR_WIDTH = 16.dp
