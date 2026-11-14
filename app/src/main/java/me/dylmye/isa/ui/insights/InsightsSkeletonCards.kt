package me.dylmye.isa.ui.insights

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.dylmye.isa.ui.components.SkeletonBox
import me.dylmye.isa.ui.components.SkeletonCard
import me.dylmye.isa.ui.components.SkeletonLine

/** The height of a chart's plot area, matched by its placeholder. */
private val CHART_PLACEHOLDER_HEIGHT = 160.dp

/**
 * The cards a returning user with contributions this year and no Junior ISA is about to get, so the
 * screen's first frame has the loaded layout's shape rather than an unrelated spinner. The Junior
 * card is omitted for the same reason: it is conditional, and this is the common case.
 */
internal fun LazyGridScope.insightsSkeletonCards() {
  item(
    key = "loading-countdown",
    contentType = "loading-countdown",
    span = { GridItemSpan(maxLineSpan) },
  ) {
    SkeletonCard {
      SkeletonLine(modifier = Modifier.width(160.dp), height = 48.dp)
      SkeletonLine(modifier = Modifier.width(180.dp), height = 20.dp)
    }
  }
  item(key = "loading-goal", contentType = "loading-goal", span = { GridItemSpan(maxLineSpan) }) {
    SkeletonCard {
      SkeletonLine(modifier = Modifier.width(140.dp), height = 20.dp)
      SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 8.dp)
      SkeletonLine(modifier = Modifier.width(220.dp), height = 20.dp)
      SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
    }
  }
  item(key = "loading-account-donut", contentType = "loading-donut", span = { GridItemSpan(1) }) {
    SkeletonChartCard()
  }
  item(key = "loading-type-donut", contentType = "loading-donut", span = { GridItemSpan(1) }) {
    SkeletonChartCard()
  }
  item(key = "loading-years", contentType = "loading-years", span = { GridItemSpan(maxLineSpan) }) {
    SkeletonCard {
      SkeletonLine(modifier = Modifier.width(180.dp), height = 20.dp)
      SkeletonBox(
        modifier = Modifier.fillMaxWidth().height(CHART_PLACEHOLDER_HEIGHT),
        shape = MaterialTheme.shapes.medium,
      )
    }
  }
}

/** A chart card's placeholder: a title, the chart's area and the two legend rows under it. */
@Composable
private fun SkeletonChartCard() {
  SkeletonCard {
    SkeletonLine(modifier = Modifier.width(160.dp), height = 20.dp)
    SkeletonBox(
      modifier = Modifier.fillMaxWidth().height(CHART_PLACEHOLDER_HEIGHT),
      shape = MaterialTheme.shapes.medium,
    )
    SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), height = 16.dp)
    SkeletonLine(modifier = Modifier.fillMaxWidth(0.4f), height = 16.dp)
  }
}
