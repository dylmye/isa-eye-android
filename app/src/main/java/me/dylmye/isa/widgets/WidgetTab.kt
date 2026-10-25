package me.dylmye.isa.widgets

/** Where a widget tap lands, written onto the activity intent as [EXTRA_WIDGET_TAB]. */
internal enum class WidgetTab(val extra: String) {
  Accounts("accounts"),
  Insights("insights"),
}

/** The extra on a widget's intent that says which tab opening it should land on. */
internal const val EXTRA_WIDGET_TAB = "me.dylmye.isa.extra.WIDGET_TAB"
