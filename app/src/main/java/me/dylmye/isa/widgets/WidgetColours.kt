package me.dylmye.isa.widgets

import me.dylmye.isa.R
import me.dylmye.isa.ui.insights.TaxYearStage

/**
 * The traffic-light scale for a tax year, with container tints for the widget surface. Each value
 * has a night variant in `values-night`, so the widget reads in either theme.
 */
internal object WidgetColours {
  fun stageResource(stage: TaxYearStage): Int = when (stage) {
    TaxYearStage.Early -> R.color.widget_stage_early
    TaxYearStage.Mid -> R.color.widget_stage_mid
    TaxYearStage.Late -> R.color.widget_stage_late
  }
}
