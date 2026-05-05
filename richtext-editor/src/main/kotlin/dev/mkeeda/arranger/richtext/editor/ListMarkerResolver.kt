package dev.mkeeda.arranger.richtext.editor

import dev.mkeeda.arranger.richtext.BulletListItem
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.ListItem
import dev.mkeeda.arranger.richtext.OrderedListItem

/**
 * Resolves a [ListItem] to a marker text string for display.
 *
 * Example: Alphabetical markers
 * ```
 * val alphabeticalResolver = ListMarkerResolver { item ->
 *     when (item) {
 *         is BulletListItem -> "•"
 *         is OrderedListItem -> {
 *             // Char arithmetic: 'a'(0-based) + index(1-based) - 1
 *             val letter = 'a' + item.index - 1
 *             "$letter."
 *         }
 *     }
 * }
 * ```
 *
 * Example: Parenthesized numbers
 * ```
 * val parenthesizedResolver = ListMarkerResolver { item ->
 *     when (item) {
 *         is BulletListItem -> "•"
 *         is OrderedListItem -> "(${item.index})"
 *     }
 * }
 * ```
 */
public fun interface ListMarkerResolver {
    /**
     * Resolves the marker text for the given [item].
     */
    public fun resolve(item: ListItem): String
}

/**
 * The default [ListMarkerResolver] used by [RichTextEditor].
 *
 * Provides standard bullet point symbols based on the indentation level:
 * - Level 1: "・"
 * - Level 2: "○"
 * - Level 3 and deeper: "▪"
 *
 * For ordered lists, it provides standard numbers followed by a period (e.g., "1.", "2.").
 */
public val DefaultListMarkerResolver: ListMarkerResolver =
    ListMarkerResolver { item ->
        when (item) {
            is BulletListItem -> {
                when (item.indentLevel) {
                    ListIndentLevel.Level1 -> "・"
                    ListIndentLevel.Level2 -> "○"
                    else -> "▪"
                }
            }

            is OrderedListItem -> {
                "${item.index}."
            }
        }
    }
