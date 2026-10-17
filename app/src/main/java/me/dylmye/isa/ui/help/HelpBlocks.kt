package me.dylmye.isa.ui.help

/** A run of text within a block, carrying any emphasis or link the markdown gave it. */
sealed interface HelpSpan {
  data class Text(val text: String) : HelpSpan

  data class Bold(val text: String) : HelpSpan

  data class Link(val text: String, val url: String) : HelpSpan
}

/** A block of help copy. */
sealed interface HelpBlock {
  /** [level] is the markdown heading depth, so 2 for `##` and 3 for `###`. */
  data class Heading(val level: Int, val spans: List<HelpSpan>) : HelpBlock

  data class Paragraph(val spans: List<HelpSpan>) : HelpBlock

  data class Bullets(val items: List<List<HelpSpan>>) : HelpBlock

  /** A blockquote, rendered as a callout card rather than as quoted text. */
  data class Callout(val spans: List<HelpSpan>) : HelpBlock
}

/**
 * The vocabulary [HelpContent] is generated in. These exist so the generated file reads as content
 * rather than as nested constructors, and so a regenerated file produces a clean diff.
 */
internal fun helpText(value: String): HelpSpan = HelpSpan.Text(value)

internal fun helpBold(value: String): HelpSpan = HelpSpan.Bold(value)

internal fun helpLink(text: String, url: String): HelpSpan = HelpSpan.Link(text, url)

internal fun helpHeading(level: Int, vararg spans: HelpSpan): HelpBlock =
  HelpBlock.Heading(level, spans.toList())

internal fun helpParagraph(vararg spans: HelpSpan): HelpBlock = HelpBlock.Paragraph(spans.toList())

internal fun helpItem(vararg spans: HelpSpan): List<HelpSpan> = spans.toList()

internal fun helpBullets(vararg items: List<HelpSpan>): HelpBlock =
  HelpBlock.Bullets(items.toList())

internal fun helpCallout(vararg spans: HelpSpan): HelpBlock = HelpBlock.Callout(spans.toList())
