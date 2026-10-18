package me.dylmye.isa.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews

/** Stateful entry point: owns the insights ViewModel, which reports today's tax year. */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
  val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.factory())
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  InsightsContent(uiState = uiState, modifier = modifier)
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
 * Only the countdown is unconditional: both donuts describe this year and need a contribution in it,
 * while the bar chart covers every year and needs just one of them.
 */
@Composable
fun InsightsContent(uiState: InsightsUiState, modifier: Modifier = Modifier) {
  Scaffold(modifier = modifier, contentWindowInsets = WindowInsets.statusBars) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      val columns = (maxWidth / MIN_CARD_WIDTH).toInt().coerceIn(1, MAX_COLUMNS)
      val gridWidth =
        if (columns == 1) {
          maxWidth
        } else {
          MIN_CARD_WIDTH * columns + CARD_GAP * (columns - 1) + PAGE_PADDING * 2
        }
      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.width(gridWidth).fillMaxHeight().align(Alignment.TopCenter),
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
        item(key = "countdown", span = { GridItemSpan(maxLineSpan) }) { CountdownCard(uiState) }
        if (uiState.accountShares.isNotEmpty()) {
          item(key = "accounts", span = { GridItemSpan(1) }) { AccountDonutCard(uiState) }
          item(key = "types", span = { GridItemSpan(1) }) { ProductTypeDonutCard(uiState) }
        }
        if (uiState.yearShares.isNotEmpty()) {
          item(key = "years", span = { GridItemSpan(maxLineSpan) }) { YearBarCard(uiState) }
        }
      }
    }
  }
}

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
  ISAEyeTheme { InsightsContent(uiState = PreviewInsights) }
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
  )
