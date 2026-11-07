package me.dylmye.isa.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import me.dylmye.isa.IsaEyeApplication
import me.dylmye.isa.data.db.dao.AccountSummary
import me.dylmye.isa.data.db.entity.AnnualBalanceEntity
import me.dylmye.isa.data.db.entity.ProductTypeEntity
import me.dylmye.isa.data.db.entity.RulesetEntity
import me.dylmye.isa.data.db.entity.RulesetExceptionEntity

/**
 * Observes the database for a widget. Glance keeps the composition alive while a widget session is
 * running and only re-runs [loadWidgetState] from `provideGlance` when the session is recreated, so
 * the content must follow the application state itself instead of a one-shot read.
 */
internal fun observeWidgetState(context: Context): Flow<WidgetState> {
  val application =
    context.applicationContext as? IsaEyeApplication ?: return flowOf(WidgetState())
  val database = application.database
  return combine(
    combine(
      database.productDao().observeSummaries(),
      database.productTypeDao().observeAll(),
      database.annualBalanceDao().observeAll(),
      ::WidgetSources,
    ),
    combine(
      database.rulesetDao().observeAll(),
      database.rulesetExceptionDao().observeAll(),
      ::rulesetSources,
    ),
    database.goalDao().observeAll(),
  ) { sources, rulesetSources, goals ->
    resolveWidgetState(
      accounts = sources.accounts,
      productTypes = sources.productTypes,
      balances = sources.balances,
      rulesets = rulesetSources.rulesets,
      exceptions = rulesetSources.exceptions,
      goals = goals,
      nowMillis = System.currentTimeMillis(),
    )
  }
    .distinctUntilChanged()
}

/** The widget's state, re-collected from [observeWidgetState] whenever the database changes. */
@Composable
internal fun rememberWidgetState(initial: WidgetState): WidgetState {
  val context = LocalContext.current
  val flow = remember(context) { observeWidgetState(context) }
  return flow.collectAsState(initial).value
}

private data class WidgetSources(
  val accounts: List<AccountSummary>,
  val productTypes: List<ProductTypeEntity>,
  val balances: List<AnnualBalanceEntity>,
)

private data class RulesetSources(
  val rulesets: List<RulesetEntity>,
  val exceptions: List<RulesetExceptionEntity>,
)

private fun rulesetSources(
  rulesets: List<RulesetEntity>,
  exceptions: List<RulesetExceptionEntity>,
): RulesetSources = RulesetSources(rulesets, exceptions)

/**
 * Reads the database for a widget. Widgets are refreshed by the app, not the other way around, so a
 * one-shot read is enough; [provideGlance][androidx.glance.appwidget.GlanceAppWidget.provideGlance]
 * runs it off the main thread like any other database call.
 */
internal suspend fun loadWidgetState(
  context: Context,
  nowMillis: Long = System.currentTimeMillis(),
): WidgetState = withContext(Dispatchers.IO) {
  val application =
    context.applicationContext as? IsaEyeApplication ?: return@withContext WidgetState()
  val database = application.database
  resolveWidgetState(
    accounts = database.productDao().getSummaries(),
    productTypes = database.productTypeDao().getAll(),
    balances = database.annualBalanceDao().getAll(),
    rulesets = database.rulesetDao().getAll(),
    exceptions = database.rulesetExceptionDao().getAll(),
    goals = database.goalDao().getAll(),
    nowMillis = nowMillis,
  )
}
