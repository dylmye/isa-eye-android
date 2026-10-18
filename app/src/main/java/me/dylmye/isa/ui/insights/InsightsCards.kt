package me.dylmye.isa.ui.insights

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.LegendItem
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import me.dylmye.isa.ui.toTint

/**
 * Days left in the tax year, on a card that turns green, amber then red as the year runs out. The
 * count is the point of the card, so it is set as large as a headline figure rather than as body
 * text.
 */
@Composable
internal fun CountdownCard(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  val days = uiState.daysRemaining ?: return
  val stage = uiState.stage ?: return
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = stage.container()),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "$days days",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold,
        color = stage.content(),
      )
      Text(
        text = "until tax year ends",
        style = MaterialTheme.typography.bodyLarge,
        color = stage.content(),
      )
    }
  }
}

/** The year's contributions split by account, in each provider's tint, with a legend naming them. */
@Composable
internal fun AccountDonutCard(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  InsightsCard(title = "This year by account", modifier = modifier) {
    Donut(
      shares = uiState.accountShares.map { it.colour.toTint() to it.pence },
      description = accountDonutDescription(uiState),
      modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      uiState.accountShares.forEach { share ->
        LegendRow(
          colour = share.colour.toTint(),
          label = share.name,
          value = percentOf(share.pence, uiState.currentYearTotalPence),
        )
      }
    }
  }
}

/** The year's contributions split by product type, with a legend naming the types on show. */
@Composable
internal fun ProductTypeDonutCard(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  InsightsCard(title = "This year by type", modifier = modifier) {
    Donut(
      shares = uiState.typeShares.map { it.colour.toTint() to it.pence },
      description = typeDonutDescription(uiState),
      modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      uiState.typeShares.forEach { share ->
        LegendRow(
          colour = share.colour.toTint(),
          label = share.name,
          value = percentOf(share.pence, uiState.currentYearTotalPence),
        )
      }
    }
  }
}

/**
 * Every year with a contribution, each bar stacked by product type. The years run oldest to newest
 * with abbreviated labels, and the axis scales to the best year so a modest record still fills the
 * card. The stacks are meaningless without a key, so the card carries a legend of its own.
 *
 * The model is built directly rather than through a producer: a producer delivers its first model
 * asynchronously and animates it in, which a screenshot would either miss or capture mid-flight.
 */
@Composable
internal fun YearBarCard(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  val years = uiState.yearShares
  val types = years.flatMap { it.byType }.distinctBy { it.productTypeId }
  val colours = types.associate { it.productTypeId to it.colour.toTint() }
  // Read outside the chart call: the legend's item lambda is not composable, so the label component
  // has to be built here and captured.
  val legendLabel = rememberLegendLabelComponent()
  val model =
    CartesianChartModel(
      ColumnCartesianLayerModel.build {
        types.forEach { type -> series(y = seriesValues(years, type.productTypeId)) }
      },
    )
  InsightsCard(title = "Contributions by year", modifier = modifier) {
    CartesianChartHost(
      chart =
        rememberCartesianChart(
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
        ),
      model = model,
      modifier =
        Modifier.fillMaxWidth()
          .height(CHART_HEIGHT)
          .semantics { contentDescription = barDescription(uiState) },
      zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
  }
}

/** Shared card chrome: a title above whatever the chart needs. */
@Composable
private fun InsightsCard(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      content()
    }
  }
}

/** The bar chart's legend labels, matched to the surrounding type. */
@Composable
private fun rememberLegendLabelComponent() =
  rememberTextComponent(TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))

/**
 * Traffic-light surface colours. Unlike the chart tints these need dark variants, because the whole
 * card is painted with them and a light tint on a dark screen reads as a hole punched in it.
 */
@Composable
private fun TaxYearStage.container(): Color = if (isSystemInDarkTheme()) {
  when (this) {
    TaxYearStage.Early -> Color(0xFF1B3A1E)
    TaxYearStage.Mid -> Color(0xFF3D2A12)
    TaxYearStage.Late -> Color(0xFF3A1A1A)
  }
} else {
  when (this) {
    TaxYearStage.Early -> Color(0xFFE8F5E9)
    TaxYearStage.Mid -> Color(0xFFFFF3E0)
    TaxYearStage.Late -> Color(0xFFFFEBEE)
  }
}

/** Traffic-light text colours, light enough to read on the dark surfaces above. */
@Composable
private fun TaxYearStage.content(): Color = if (isSystemInDarkTheme()) {
  when (this) {
    TaxYearStage.Early -> Color(0xFFA5D6A7)
    TaxYearStage.Mid -> Color(0xFFFFCC80)
    TaxYearStage.Late -> Color(0xFFEF9A9A)
  }
} else {
  when (this) {
    TaxYearStage.Early -> Color(0xFF1B5E20)
    TaxYearStage.Mid -> Color(0xFFE65100)
    TaxYearStage.Late -> Color(0xFFB71C1C)
  }
}

private val MoneyAxisFormatter = CartesianValueFormatter { _, value, _ -> axisMoney(value) }

private val CHART_HEIGHT = 160.dp
private val BAR_WIDTH = 16.dp
