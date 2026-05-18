package dev.mkeeda.arranger.richtext.editor

import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey

/**
 * Toggles a [SpanAttributeKey] that does not require a value (i.e. its value is [Unit]).
 *
 * If text is selected, the format is applied to the selection.
 * If no text is selected (cursor is collapsed), the format is applied to the next typed characters.
 * In either case, if the attribute is currently active, it is removed. Otherwise, it is applied.
 */
public fun RichTextState.toggleFormat(key: SpanAttributeKey<Unit>) {
    val isActive = currentAttributes.containsKey(key)
    if (selection.collapsed) {
        if (isActive) {
            removeTypingAttribute(key)
        } else {
            setTypingAttribute(key, Unit)
        }
    } else {
        edit {
            editAttributes(selection) {
                if (isActive) {
                    setSpanAttribute(key, null)
                } else {
                    setSpanAttribute(key, Unit)
                }
            }
        }
    }
}

/**
 * Toggles a [ParagraphAttributeKey] that does not require a value (i.e. its value is [Unit]).
 *
 * The format is applied to the paragraph(s) overlapping the current cursor position or selection.
 * If the attribute is currently active, it is removed. Otherwise, it is applied.
 */
public fun RichTextState.toggleFormat(key: ParagraphAttributeKey<Unit>) {
    val isActive = currentAttributes.containsKey(key)
    edit {
        editAttributes(selection) {
            if (isActive) {
                setParagraphAttribute(key, null)
            } else {
                setParagraphAttribute(key, Unit)
            }
        }
    }
}

/**
 * Applies the given [value] for a [SpanAttributeKey].
 *
 * If text is selected, the format is applied to the selection.
 * If no text is selected (cursor is collapsed), the format is applied to the next typed characters.
 */
public fun <T : Any> RichTextState.applyFormat(key: SpanAttributeKey<T>, value: T) {
    if (selection.collapsed) {
        setTypingAttribute(key, value)
    } else {
        edit {
            editAttributes(selection) {
                setSpanAttribute(key, value)
            }
        }
    }
}

/**
 * Applies the given [value] for a [ParagraphAttributeKey].
 *
 * The format is applied to the paragraph(s) overlapping the current cursor position or selection.
 */
public fun <T : Any> RichTextState.applyFormat(key: ParagraphAttributeKey<T>, value: T) {
    edit {
        editAttributes(selection) {
            setParagraphAttribute(key, value)
        }
    }
}

/**
 * Removes the format associated with the given [key].
 *
 * - If [key] is a [SpanAttributeKey] and the selection is collapsed, the format is removed
 *   from the typing attributes for the next input.
 * - Otherwise (text is selected, or [key] is a [ParagraphAttributeKey]), the format is
 *   removed from the overlapping selection or paragraph(s).
 */
public fun RichTextState.removeFormat(key: AttributeKey<*>) {
    if (selection.collapsed && key is SpanAttributeKey<*>) {
        removeTypingAttribute(key)
    } else {
        edit {
            editAttributes(selection) {
                when (key) {
                    is SpanAttributeKey<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        setSpanAttribute(key as SpanAttributeKey<Any>, null)
                    }

                    is ParagraphAttributeKey<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        setParagraphAttribute(key as ParagraphAttributeKey<Any>, null)
                    }
                }
            }
        }
    }
}

/**
 * Clears all currently active formats.
 *
 * If text is selected, all formats are removed from the selection.
 * If no text is selected (cursor is collapsed), all typing attributes are cleared.
 */
public fun RichTextState.clearFormats() {
    if (selection.collapsed) {
        clearTypingAttributes()
    } else {
        edit {
            editAttributes(selection) {
                clearAll()
            }
        }
    }
}
