package me.dylmye.isa.ui.accounts

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import me.dylmye.isa.theme.ISAEyeTheme
import me.dylmye.isa.ui.FormFactorPreviews
import me.dylmye.isa.ui.components.LoadingIndicator
import me.dylmye.isa.ui.components.ProviderAvatar
import me.dylmye.isa.ui.components.containerTint
import me.dylmye.isa.ui.components.opaqueContainerTint
import me.dylmye.isa.ui.formatGbp
import me.dylmye.isa.ui.toTint

/** Stateful entry point: owns the ViewModel and hoists its state down into [AccountsContent]. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AccountsScreen(onAccountClick: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var notesVisible by rememberSaveable { mutableStateOf(false) }
  val isCompact =
    !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
      WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
  AccountsContent(
    uiState = uiState,
    onAccountClick = onAccountClick,
    onReorderAccounts = viewModel::onReorderAccounts,
    onPreviousRuleset = viewModel::onPreviousRuleset,
    onNextRuleset = viewModel::onNextRuleset,
    onResetRuleset = viewModel::onResetRuleset,
    onShowNotes = { notesVisible = true },
    modifier = modifier,
  )
  // The year's notes follow the selection, so they stay live while the surface is open.
  if (notesVisible) {
    RulesetNotesSurface(
      notes = uiState.notes,
      isCompact = isCompact,
      onDismiss = { notesVisible = false },
    )
  }
}

/**
 * Stateless accounts list. Renders [uiState] and reports events upwards; it holds no state of its
 * own, so it can be previewed and tested with any UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsContent(
  uiState: AccountsUiState,
  onAccountClick: (String) -> Unit,
  onReorderAccounts: (List<String>) -> Unit,
  onPreviousRuleset: () -> Unit,
  onNextRuleset: () -> Unit,
  onResetRuleset: () -> Unit,
  onShowNotes: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier =
      modifier.semantics {
        paneTitle = "Accounts"
      },
    topBar = {
      Column {
        AccountsTopBar(
          remainingPence = uiState.remainingPence,
          heroIsJunior = uiState.heroIsJunior,
          juniorAllowances = uiState.juniorAllowances,
          scrollBehavior = scrollBehavior,
        )
        if (uiState.ruleset.current != null) {
          RulesetHeader(
            state = uiState.ruleset,
            onPrevious = onPreviousRuleset,
            onNext = onNextRuleset,
            onReset = onResetRuleset,
            onShowNotes = onShowNotes,
          )
        }
      }
    },
    contentWindowInsets = WindowInsets(0.dp),
  ) { innerPadding ->
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding =
      PaddingValues(
        start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
        top = innerPadding.calculateTopPadding(),
        end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
        bottom = innerPadding.calculateBottomPadding() + 88.dp,
      )
    when {
      uiState.isLoading -> {
        AccountsLoading(contentPadding)
      }

      uiState.accounts.isEmpty() -> {
        AccountsEmpty(
          hasAccounts = uiState.hasAccounts,
          ruleset = uiState.ruleset.current,
          modifier = Modifier.fillMaxSize().padding(contentPadding),
        )
      }

      else -> {
        AccountsList(
          accounts = uiState.accounts,
          onAccountClick = onAccountClick,
          onReorderAccounts = onReorderAccounts,
          scrollBehavior = scrollBehavior,
          contentPadding = contentPadding,
        )
      }
    }
  }
}

/** Hero app bar: the year's remaining allowance while expanded, the screen name once collapsed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsTopBar(
  remainingPence: Long?,
  heroIsJunior: Boolean,
  juniorAllowances: List<JuniorAllowanceUiState>,
  scrollBehavior: TopAppBarScrollBehavior,
) {
  val collapsed = scrollBehavior.state.collapsedFraction > 0.5f
  val hero =
    if (collapsed) {
      null
    } else {
      allowanceHeadline(remainingPence)?.let { headline ->
        HeroText(headline = headline, caption = juniorCaption(heroIsJunior, juniorAllowances))
      }
    }
  MediumTopAppBar(
    title = {
      Crossfade(targetState = hero, label = "accountsTitle") { text ->
        if (text == null) {
          Text("Accounts", modifier = Modifier.semantics { heading() })
        } else {
          Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            // Fixed size on purpose: sizing to the text made short figures balloon and long ones
            // shrink, so the header changed size whenever the amount did.
            Text(
              text = text.headline,
              maxLines = 1,
              style = MaterialTheme.typography.displaySmall.copy(lineHeight = TextUnit.Unspecified),
            )
            text.caption?.let { caption ->
              Text(
                text = caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    scrollBehavior = scrollBehavior,
  )
}

/** What the hero says while the bar is expanded: the big figure and, optionally, a line under it. */
private data class HeroText(val headline: String, val caption: String?)

/**
 * The Junior allowance line under the hero. When the hero is already the Junior figure the line
 * merely names it; otherwise it totals what the year's Junior accounts have left between them.
 */
private fun juniorCaption(
  heroIsJunior: Boolean,
  allowances: List<JuniorAllowanceUiState>,
): String? {
  if (allowances.isEmpty()) return null
  val label = allowances.first().typeName.ifBlank { "Junior ISA" }
  val plural = if (allowances.size == 1) "" else "s"
  return if (heroIsJunior) {
    "$label allowance$plural"
  } else {
    "${formatGbp(allowances.sumOf { it.remainingPence })} left in $label allowance$plural"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsList(
  accounts: List<AccountListItemUiState>,
  onAccountClick: (String) -> Unit,
  onReorderAccounts: (List<String>) -> Unit,
  scrollBehavior: TopAppBarScrollBehavior,
  contentPadding: PaddingValues,
) {
  val listState = rememberLazyListState()
  val latestOnReorder by rememberUpdatedState(onReorderAccounts)
  val reorder =
    remember(listState) {
      AccountReorderState(listState, accounts) { order -> latestOnReorder(order) }
    }
  // Follow the saved order, unless the user is in the middle of rearranging it.
  LaunchedEffect(accounts) { reorder.updateItems(accounts) }
  LazyColumn(
    state = listState,
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    itemsIndexed(reorder.items, key = { _, account -> account.productId }) { index, account ->
      val dragging = reorder.isDragging(account.productId)
      // The lifted row is drawn at its real place through the offset, so animating its placement
      // as well would move it twice.
      val placement = if (dragging) Modifier else Modifier.animateItem()
      // Build the TalkBack actions here, never inside the semantics block below: reading the
      // reorder state in there registers the row's semantics for invalidation on every list
      // change, and recalculating them while the row is leaving the list can crash with
      // "visitAncestors called on an unattached node".
      val actions = moveActions(index, reorder.items.size, reorder::move)
      val position = "${index + 1} of ${reorder.items.size}"
      AccountRow(
        account = account,
        dragging = dragging,
        onClick = { onAccountClick(account.productId) },
        modifier =
          placement.semantics {
            contentDescription = "${account.name}, ${account.providerName}, " +
              "${formatGbp(account.contributionsPence)} contributed"
            customActions = actions
            stateDescription = position
          },
        dragModifier = reorder.dragModifier(account.productId),
      )
    }
  }
}

/** TalkBack's stand-in for dragging: a row can be moved one place at a time. */
private fun moveActions(
  index: Int,
  count: Int,
  move: (from: Int, to: Int) -> Unit,
): List<CustomAccessibilityAction> = buildList {
  if (index > 0) {
    add(
      CustomAccessibilityAction("Move up") {
        move(index, index - 1)
        true
      },
    )
  }
  if (index < count - 1) {
    add(
      CustomAccessibilityAction("Move down") {
        move(index, index + 1)
        true
      },
    )
  }
}

@Composable
private fun AccountsLoading(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize().padding(contentPadding),
    contentAlignment = Alignment.Center,
  ) {
    LoadingIndicator(label = "Loading accounts")
  }
}

@Composable
private fun AccountRow(
  account: AccountListItemUiState,
  dragging: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  dragModifier: Modifier = Modifier,
) {
  val tint = account.providerColour.toTint()
  // A lifted card turns opaque and casts a shadow, so the cards it passes over cannot show through
  // it and mix their colours with its own.
  val container =
    if (dragging) {
      tint.opaqueContainerTint(MaterialTheme.colorScheme.background)
    } else {
      tint.containerTint()
    }
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = container,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shadowElevation = if (dragging) 6.dp else 0.dp,
    modifier = modifier.fillMaxWidth().then(dragModifier),
  ) {
    ListItem(
      // The row's own semantics above carry its name, provider, amount and reorder actions, so
      // the visible text underneath is hidden to avoid repeating it.
      modifier = Modifier.clearAndSetSemantics {},
      headlineContent = { Text(account.name) },
      supportingContent = { Text(account.providerName) },
      leadingContent = {
        ProviderAvatar(
          provider = account.providerName,
          tint = tint,
          iconUrl = account.providerIconUrl,
          // The provider is already named beside the avatar, so it is decorative here.
          modifier = Modifier.clearAndSetSemantics {},
        )
      },
      trailingContent = {
        Text(
          text = formatGbp(account.contributionsPence),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
        )
      },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }
}

@Composable
private fun AccountsEmpty(hasAccounts: Boolean, ruleset: String?, modifier: Modifier = Modifier) {
  val message =
    if (hasAccounts) {
      "No accounts were open in ${ruleset.orEmpty()}."
    } else {
      "Add an account to get started!"
    }
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        "Nothing to see here",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@FormFactorPreviews
@Composable
private fun AccountsContentPreview() {
  ISAEyeTheme {
    AccountsContent(
      uiState = PreviewAccountsUiState,
      onAccountClick = {},
      onReorderAccounts = {},
      onPreviousRuleset = {},
      onNextRuleset = {},
      onResetRuleset = {},
      onShowNotes = {},
    )
  }
}

@FormFactorPreviews
@Composable
private fun AccountsEmptyPreview() {
  ISAEyeTheme {
    AccountsContent(
      uiState = AccountsUiState(ruleset = RulesetState(current = "2026/2027", canGoBack = true)),
      onAccountClick = {},
      onReorderAccounts = {},
      onPreviousRuleset = {},
      onNextRuleset = {},
      onResetRuleset = {},
      onShowNotes = {},
    )
  }
}

private val PreviewAccountsUiState =
  AccountsUiState(
    accounts =
      listOf(
        AccountListItemUiState("1", "Cash ISA 2025/26", "Lloyds Bank", "#006A4D", 125_000),
        AccountListItemUiState("2", "Stocks & Shares ISA", "Vanguard", "#8A1E2D", 432_050),
        AccountListItemUiState("3", "Help to Buy ISA", "Nationwide", "#004B87", 0),
        AccountListItemUiState("4", "Junior ISA", "Hargreaves Lansdown", "#003A70", 0),
      ),
    ruleset = RulesetState(current = "2026/2027", canGoBack = true),
    remainingPence = 1_250_000,
  )
