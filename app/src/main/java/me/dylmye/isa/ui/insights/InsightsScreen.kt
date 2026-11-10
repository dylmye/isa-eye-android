package me.dylmye.isa.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.verticalScrollbar

/** Stateful entry point: owns the insights ViewModel and hoists its state into [InsightsContent]. */
@Composable
fun InsightsScreen(onEditGoal: () -> Unit, modifier: Modifier = Modifier) {
  val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.factory())
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  InsightsContent(uiState = uiState, onEditGoal = onEditGoal, modifier = modifier)
}

/**
 * Stateless insights pane. Cards sit in a grid capped at [MAX_COLUMNS] columns so they keep a widget
 * width rather than stretching, and centred so a wide window gets gutters rather than wider cards. A
 * single column fills the width instead, which is what a narrow window gets.
 *
 * There is no app bar. The bottom bar already names the selected tab, and nothing lives in an app
 * bar's actions here, so its title only repeated what the tab said. The status bar inset that bar
 * used to carry is applied by the scaffold instead.
 *
 * Only the countdown and the goal are unconditional: both donuts describe this year and need a
 * contribution in it, while the bar chart covers every year and needs just one of them.
 */
@Composable
fun InsightsContent(
  uiState: InsightsUiState,
  onEditGoal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier =
      modifier.semantics {
        paneTitle = "Insights"
      },
    contentWindowInsets = WindowInsets.statusBars,
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      val gridState = rememberLazyGridState()
      LazyVerticalGrid(
        columns = WidgetGridCells,
        state = gridState,
        modifier =
          Modifier.cappedWidgetWidth()
            .align(Alignment.TopCenter)
            .testTag("robo_insights_grid")
            .verticalScrollbar(state = gridState.scrollIndicatorState),
        contentPadding =
          PaddingValues(
            start = PAGE_PADDING,
            top = PAGE_PADDING,
            end = PAGE_PADDING,
            bottom = FAB_CLEARANCE,
          ),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
      ) {
        insightsCards(uiState = uiState, onEditGoal = onEditGoal)
      }
    }
  }
}

/**
 * The cards, in the order they appear. Each names its type so the grid can reuse composition slots
 * for cards of the same kind as they scroll into and out of view.
 */
private fun LazyGridScope.insightsCards(uiState: InsightsUiState, onEditGoal: () -> Unit) {
  item(key = "countdown", contentType = "countdown", span = { GridItemSpan(maxLineSpan) }) {
    CountdownCard(uiState)
  }
  if (uiState.ruleset != null) {
    item(key = "goal", contentType = "goal", span = { GridItemSpan(maxLineSpan) }) {
      GoalCard(uiState = uiState, onEdit = onEditGoal)
    }
  }
  if (uiState.juniorAllowances.isNotEmpty()) {
    item(key = "junior", contentType = "junior", span = { GridItemSpan(maxLineSpan) }) {
      JuniorAllowanceCard(uiState)
    }
  }
  if (uiState.accountShares.isNotEmpty()) {
    item(key = "accounts", contentType = "accounts", span = { GridItemSpan(1) }) {
      AccountDonutCard(uiState)
    }
    item(key = "types", contentType = "types", span = { GridItemSpan(1) }) {
      ProductTypeDonutCard(uiState)
    }
  }
  if (uiState.yearShares.isNotEmpty()) {
    item(key = "years", contentType = "years", span = { GridItemSpan(maxLineSpan) }) {
      YearBarCard(uiState)
    }
  }
}

/**
 * The grid's columns. The cell sizes come from the width the grid is measured at, so the column
 * count needs no measurement of its own, unlike the
 * [BoxWithConstraints][androidx.compose.foundation.layout.BoxWithConstraints] this replaced.
 */
private object WidgetGridCells : GridCells {
  override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
    // availableSize excludes the page padding, but the padding forms part of the breakpoint.
    val columns = columnCount(availableSize + (PAGE_PADDING * 2).roundToPx())
    val cellsWidth = (availableSize - spacing * (columns - 1)).coerceAtLeast(0)
    val cellWidth = cellsWidth / columns
    val remainder = cellsWidth % columns
    return List(columns) { index -> if (index < remainder) cellWidth + 1 else cellWidth }
  }
}

/**
 * Caps the grid at [MAX_COLUMNS] card widths, including their gaps and padding, so a wide window
 * gets gutters rather than wider cards. The cap is applied in the layout pass, which keeps the
 * grid out of the [SubcomposeLayout][androidx.compose.ui.layout.SubcomposeLayout] that
 * [BoxWithConstraints][androidx.compose.foundation.layout.BoxWithConstraints] would introduce.
 */
private fun Modifier.cappedWidgetWidth(): Modifier = layout { measurable, constraints ->
  val columns = columnCount(constraints.maxWidth)
  val width =
    if (columns == 1) {
      constraints.maxWidth
    } else {
      gridWidth(columns).coerceAtMost(constraints.maxWidth)
    }
  val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
  layout(width = width, height = placeable.height) { placeable.place(0, 0) }
}

/** How many card columns fit in [widthPx], bounded so cards stay a widget width. */
private fun Density.columnCount(widthPx: Int): Int =
  (widthPx / MIN_CARD_WIDTH.roundToPx()).coerceIn(1, MAX_COLUMNS)

/** The width in pixels of [columns] cards, with their gaps and the page padding. */
private fun Density.gridWidth(columns: Int): Int =
  (MIN_CARD_WIDTH * columns + CARD_GAP * (columns - 1) + PAGE_PADDING * 2).roundToPx()

/** A card narrower than this would be cramped; wider, and the grid stops reading as widgets. */
private val MIN_CARD_WIDTH = 320.dp
private val CARD_GAP = 12.dp
private val PAGE_PADDING = 16.dp

/** Keeps the last card clear of the add-account button the scaffold floats over it. */
private val FAB_CLEARANCE = 88.dp
private const val MAX_COLUMNS = 3

@FormFactorPreviews
@Composable
private fun InsightsContentPreview() {
  ISAEyeTheme { InsightsContent(uiState = PreviewInsights, onEditGoal = {}) }
}

private val PreviewInsights =
  InsightsUiState(
    ruleset = "2026/2027",
    daysRemaining = 169,
    stage = TaxYearStage.Mid,
    currentYearTotalPence = 165_000,
    accountShares =
      listOf(
        AccountShare("a1", "Cash ISA", "#006A4D", 40_000),
        AccountShare("a2", "Stocks & Shares ISA", "#8A1E2D", 125_000),
      ),
    typeShares =
      listOf(
        TypeShare("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0", 125_000),
        TypeShare("CASH", "Cash ISA", "#2E7D32", 40_000),
      ),
    yearShares =
      listOf(
        YearShare(
          rulesetId = "2025/2026",
          label = "25/26",
          totalPence = 50_000,
          byType = listOf(TypeShare("CASH", "Cash ISA", "#2E7D32", 50_000)),
        ),
        YearShare(
          rulesetId = "2026/2027",
          label = "26/27",
          totalPence = 165_000,
          byType =
            listOf(
              TypeShare("STOCKS_AND_SHARES", "Stocks & Shares ISA", "#1565C0", 125_000),
              TypeShare("CASH", "Cash ISA", "#2E7D32", 40_000),
            ),
        ),
      ),
    goal =
      GoalProgress(
        targetPence = 2_000_000,
        contributedPence = 165_000,
        monthsRemaining = 6,
        monthlySuggestionPence = 305_900,
      ),
  )
