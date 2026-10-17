package me.dylmye.isa.ui.help

/**
 * A help topic the Help tab lists as a card. The [id] is what the page is navigated by, so it stays
 * stable if a [title] is reworded.
 */
enum class HelpTopic(val id: String, val title: String) {
  WhatIsAnIsa("what-is-an-isa", "What is an ISA?"),
  WhatCanISaveWithAnIsa("what-can-i-save-with-an-isa", "What can I save with an ISA?"),
  AreIsasRightForMe("are-isas-right-for-me", "Are ISAs right for me?"),
  FindingTheBestAccounts(
    "tips-for-finding-the-best-accounts",
    "Tips for finding the best accounts",
  ),
  MaximisingTheAllowance("maximising-your-isa-allowance", "Maximising your ISA allowance"),
  ;

  companion object {
    /** The topic an [id] names, or null when nothing matches. */
    fun fromId(id: String): HelpTopic? = entries.firstOrNull { it.id == id }
  }
}
