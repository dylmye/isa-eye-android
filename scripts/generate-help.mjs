// Generates app/src/main/java/me/dylmye/isa/ui/help/HelpContent.kt from the help articles in
// app/src/main/assets/help/.
//
// Run with Node:
//   node scripts/generate-help.mjs
//
// One article per topic, named after the topic id. An article's `# ` heading becomes the topic
// title, which is what the card and the page app bar show, so the card list is derived from the
// articles rather than maintained alongside them. ORDER fixes the card order, and the script fails
// if the articles and ORDER disagree in either direction, so adding an article is a deliberate
// change in two places rather than a silent one.
//
// Supported markdown: `#`/`##`/`###` headings, paragraphs, `-`/`*` bullets, `**bold**`,
// `[text](url)` links, and `>` blockquotes (rendered as callout cards). Anything else survives as
// plain text rather than being dropped.

import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const articlesDirectory = fileURLToPath(new URL("../app/src/main/assets/help", import.meta.url));
const output = fileURLToPath(
  new URL("../app/src/main/java/me/dylmye/isa/ui/help/HelpContent.kt", import.meta.url),
);

// The order the topics are listed in, which is not the order they sort in on disk.
const ORDER = [
  "what-is-an-isa",
  "what-can-i-put-in-an-isa",
  "maximising-isa-allowance",
  "glossary",
];

const MAX_LINE = 100;

// Inline tokens, tried in order: bold first so `**` is not read as the start of a link.
const INLINE = /(\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g;

const spansOf = (text) => {
  const spans = [];
  let consumed = 0;
  for (const match of text.matchAll(INLINE)) {
    if (match.index > consumed) {
      spans.push({ kind: "text", value: text.slice(consumed, match.index) });
    }
    const token = match[0];
    if (token.startsWith("**")) {
      spans.push({ kind: "bold", value: token.slice(2, -2) });
    } else {
      const link = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
      spans.push({ kind: "link", value: link[1], url: link[2] });
    }
    consumed = match.index + token.length;
  }
  if (consumed < text.length) {
    spans.push({ kind: "text", value: text.slice(consumed) });
  }
  return spans.length > 0 ? spans : [{ kind: "text", value: text }];
};

const isBullet = (line) => /^[-*] /.test(line);
const isHeading = (line) => /^#{1,6} /.test(line);
const isCallout = (line) => line.startsWith("> ");

/** Parses one article into its title and its blocks, in document order. */
const parseArticle = (source) => {
  const lines = source.split(/\r?\n/);
  const blocks = [];
  let title = null;
  let index = 0;

  while (index < lines.length) {
    const line = lines[index].trim();
    if (line === "") {
      index += 1;
    } else if (line.startsWith("### ")) {
      blocks.push({ kind: "heading", level: 3, spans: spansOf(line.slice(4).trim()) });
      index += 1;
    } else if (line.startsWith("## ")) {
      blocks.push({ kind: "heading", level: 2, spans: spansOf(line.slice(3).trim()) });
      index += 1;
    } else if (line.startsWith("# ")) {
      title = line.slice(2).trim();
      index += 1;
    } else if (isCallout(line)) {
      const quoted = [];
      while (index < lines.length && isCallout(lines[index].trim())) {
        quoted.push(lines[index].trim().slice(2).trim());
        index += 1;
      }
      blocks.push({ kind: "callout", spans: spansOf(quoted.join(" ")) });
    } else if (isBullet(line)) {
      const items = [];
      while (index < lines.length && isBullet(lines[index].trim())) {
        items.push(spansOf(lines[index].trim().slice(2).trim()));
        index += 1;
      }
      blocks.push({ kind: "bullets", items });
    } else {
      // A paragraph runs until a blank line or the start of another block, so hard-wrapped prose
      // still reads as one paragraph.
      const prose = [];
      while (index < lines.length) {
        const next = lines[index].trim();
        if (next === "" || isHeading(next) || isCallout(next) || isBullet(next)) break;
        prose.push(next);
        index += 1;
      }
      blocks.push({ kind: "paragraph", spans: spansOf(prose.join(" ")) });
    }
  }

  if (title === null) {
    throw new Error("Article has no `# ` heading to use as its title");
  }
  return { title, blocks };
};

const escape = (value) =>
  value.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\$/g, "\\$");

const indentOf = (width) => " ".repeat(width);

/**
 * Renders `value` as one or more Kotlin string literals, broken at word boundaries so no line
 * exceeds the column limit.
 *
 * The parts are slices of the original rather than words rejoined, so the spaces either side of a
 * break survive and the parts concatenate back to exactly the input. That matters because a link is
 * its own span: dropping the space before it renders "you payCapital Gains Tax".
 */
const kotlinString = (value, indent) => {
  if (value === "") return `${indentOf(indent)}""`;

  // A continuation line is the tightest fit: indent + 2, the quotes, and ` +` or `,`.
  const limit = MAX_LINE - indent - 6;
  const parts = [];
  let rest = value;

  while (rest !== "") {
    if (escape(rest).length <= limit) {
      parts.push(rest);
      break;
    }
    // Break after the last space that still fits. A single word longer than the limit is emitted
    // whole rather than split.
    let cut = 0;
    for (let at = rest.indexOf(" "); at !== -1; at = rest.indexOf(" ", at + 1)) {
      if (escape(rest.slice(0, at + 1)).length > limit) break;
      cut = at + 1;
    }
    if (cut === 0) {
      parts.push(rest);
      break;
    }
    parts.push(rest.slice(0, cut));
    rest = rest.slice(cut);
  }

  return parts
    .map((part, position) => {
      const isLast = position === parts.length - 1;
      const prefix = indentOf(position === 0 ? indent : indent + 2);
      return `${prefix}"${escape(part)}"${isLast ? "," : " +"}`;
    })
    .join("\n");
};

const flatSpan = (span) =>
  span.kind === "bold"
    ? `helpBold("${escape(span.value)}")`
    : span.kind === "link"
      ? `helpLink("${escape(span.value)}", "${escape(span.url)}")`
      : `helpText("${escape(span.value)}")`;

const flatSpans = (spans) => spans.map(flatSpan).join(", ");

const renderSpan = (span, indent) => {
  const pad = indentOf(indent);
  const flat = flatSpan(span);
  if (indent + flat.length <= MAX_LINE) return `${pad}${flat},`;

  if (span.kind === "bold") {
    return [`${pad}helpBold(`, kotlinString(span.value, indent + 2), `${pad}),`].join("\n");
  }
  if (span.kind === "link") {
    return [
      `${pad}helpLink(`,
      kotlinString(span.value, indent + 2),
      kotlinString(span.url, indent + 2),
      `${pad}),`,
    ].join("\n");
  }
  return [`${pad}helpText(`, kotlinString(span.value, indent + 2), `${pad}),`].join("\n");
};

const renderSpans = (spans, indent) =>
  spans.map((span) => renderSpan(span, indent)).join("\n");

const renderItem = (item, indent) => {
  const pad = indentOf(indent);
  const flat = `helpItem(${flatSpans(item)})`;
  if (indent + flat.length <= MAX_LINE) return `${pad}${flat},`;
  return [`${pad}helpItem(`, renderSpans(item, indent + 2), `${pad}),`].join("\n");
};

const renderBlock = (block, indent) => {
  const pad = indentOf(indent);

  if (block.kind === "bullets") {
    const flat = `helpBullets(${block.items
      .map((item) => `helpItem(${flatSpans(item)})`)
      .join(", ")})`;
    if (indent + flat.length <= MAX_LINE) return `${pad}${flat},`;
    return [
      `${pad}helpBullets(`,
      block.items.map((item) => renderItem(item, indent + 2)).join("\n"),
      `${pad}),`,
    ].join("\n");
  }

  if (block.kind === "heading") {
    const flat = `helpHeading(${block.level}, ${flatSpans(block.spans)})`;
    if (indent + flat.length <= MAX_LINE) return `${pad}${flat},`;
    return [
      `${pad}helpHeading(`,
      `${pad}  ${block.level},`,
      renderSpans(block.spans, indent + 2),
      `${pad}),`,
    ].join("\n");
  }

  const name = block.kind === "paragraph" ? "helpParagraph" : "helpCallout";
  const flat = `${name}(${flatSpans(block.spans)})`;
  if (indent + flat.length <= MAX_LINE) return `${pad}${flat},`;
  return [`${pad}${name}(`, renderSpans(block.spans, indent + 2), `${pad}),`].join("\n");
};

const files = readdirSync(articlesDirectory)
  .filter((name) => name.endsWith(".md"))
  .map((name) => name.slice(0, -3));

const missing = ORDER.filter((id) => !files.includes(id));
const unlisted = files.filter((id) => !ORDER.includes(id));
if (missing.length > 0 || unlisted.length > 0) {
  throw new Error(
    [
      missing.length > 0 ? `ORDER lists articles that do not exist: ${missing.join(", ")}` : null,
      unlisted.length > 0 ? `Articles missing from ORDER: ${unlisted.join(", ")}` : null,
    ]
      .filter(Boolean)
      .join("\n"),
  );
}

const articles = ORDER.map((id) => ({
  id,
  ...parseArticle(readFileSync(`${articlesDirectory}/${id}.md`, "utf8")),
}));

const topicEntries = articles
  .map(({ id, title }) =>
    [
      "    HelpTopic(",
      `      id = ${JSON.stringify(id)},`,
      `      title = ${JSON.stringify(title)},`,
      "    ),",
    ].join("\n"),
  )
  .join("\n");

const contentEntries = articles
  .map(({ id, blocks }) =>
    [
      `    ${JSON.stringify(id)} to`,
      "      listOf(",
      blocks.map((block) => renderBlock(block, 8)).join("\n"),
      "      ),",
    ].join("\n"),
  )
  .join("\n");

const file = `// Generated by scripts/generate-help.mjs from app/src/main/assets/help. Do not edit by hand.
package me.dylmye.isa.ui.help

/** The help topics, in the order they are listed. */
internal val HelpTopics: List<HelpTopic> =
  listOf(
${topicEntries}
  )

/** The parsed copy for each topic, keyed by [HelpTopic.id]. */
internal val HelpContent: Map<String, List<HelpBlock>> =
  mapOf(
${contentEntries}
  )
`;

writeFileSync(output, file);
console.log(`Wrote ${articles.length} topics to ${output}`);
