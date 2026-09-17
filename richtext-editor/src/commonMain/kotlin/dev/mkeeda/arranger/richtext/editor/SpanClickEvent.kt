package dev.mkeeda.arranger.richtext.editor

import dev.mkeeda.arranger.richtext.RichSpan

/**
 * Represents a click or tap event on a [RichSpan] within the rich text editor.
 *
 * @property span The [RichSpan] at the clicked position, containing its character range and attributes.
 */
public class SpanClickEvent(
    public val span: RichSpan,
) {
    /**
     * Returns true if this event has been consumed by a handler.
     */
    public var isConsumed: Boolean = false
        private set

    /**
     * Marks this event as consumed, indicating that the span click has been handled
     * and subsequent default actions (such as cursor placement) should be suppressed.
     */
    public fun consume() {
        isConsumed = true
    }
}
