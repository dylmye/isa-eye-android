package me.dylmye.isa.widgets

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.dylmye.isa.IsaEyeApplication

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
