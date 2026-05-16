package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.editor.RichTextState

/**
 * A controller that simplifies formatting operations on a [RichTextState].
 *
 * It provides a unified API for applying, toggling, removing, and clearing rich text attributes.
 * It automatically handles the difference between applying attributes to an active selection
 * versus updating typing attributes when there is no active selection.
 *
 * @property state The [RichTextState] that this controller manipulates.
 */
public class RichTextFormatController(private val state: RichTextState) {
    /**
     * Checks whether the given [key] is currently active at the cursor position or within the selection.
     */
    public fun isActive(key: AttributeKey<*>): Boolean {
        return state.currentAttributes.containsKey(key)
    }

    /**
     * Gets the current value for the given [key] at the cursor position or within the selection.
     */
    public fun <T : Any> getCurrentValue(key: AttributeKey<T>): T? {
        return state.currentAttributes[key]
    }

    /**
     * Toggles a [SpanAttributeKey] that does not require a value (i.e. its value is [Unit]).
     * If the attribute is currently active, it is removed. Otherwise, it is applied.
     */
    public fun toggle(key: SpanAttributeKey<Unit>) {
        val isActive = isActive(key)
        if (state.selection.collapsed) {
            if (isActive) {
                state.removeTypingAttribute(key)
            } else {
                state.setTypingAttribute(key, Unit)
            }
        } else {
            state.edit {
                editAttributes(state.selection) {
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
     * If the attribute is currently active, it is removed. Otherwise, it is applied.
     */
    public fun toggle(key: ParagraphAttributeKey<Unit>) {
        val isActive = isActive(key)
        state.edit {
            editAttributes(state.selection) {
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
     * If text is selected, the attribute is applied to the selection.
     * Otherwise, it is added to the typing attributes.
     */
    public fun <T : Any> apply(key: SpanAttributeKey<T>, value: T) {
        if (state.selection.collapsed) {
            state.setTypingAttribute(key, value)
        } else {
            state.edit {
                editAttributes(state.selection) {
                    setSpanAttribute(key, value)
                }
            }
        }
    }

    /**
     * Applies the given [value] for a [ParagraphAttributeKey].
     * The attribute is applied to the paragraph(s) overlapping the current selection.
     */
    public fun <T : Any> apply(key: ParagraphAttributeKey<T>, value: T) {
        state.edit {
            editAttributes(state.selection) {
                setParagraphAttribute(key, value)
            }
        }
    }

    /**
     * Removes the attribute associated with the given [key].
     * If text is selected, it is removed from the selection.
     * Otherwise, it is removed from the typing attributes.
     */
    public fun remove(key: AttributeKey<*>) {
        if (state.selection.collapsed && key is SpanAttributeKey<*>) {
            state.removeTypingAttribute(key)
        } else {
            state.edit {
                editAttributes(state.selection) {
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
     * Clears all currently active formatting attributes.
     * If text is selected, it removes all attributes in the selection.
     * Otherwise, it clears all typing attributes.
     */
    public fun clearAll() {
        if (state.selection.collapsed) {
            state.clearTypingAttributes()
        } else {
            state.edit {
                editAttributes(state.selection) {
                    val keysToRemove = state.currentAttributes.keys
                    keysToRemove.forEach { key ->
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
    }
}

/**
 * Creates and remembers a [RichTextFormatController] tied to the given [state].
 */
@Composable
public fun rememberRichTextFormatController(state: RichTextState): RichTextFormatController {
    return remember(state) {
        RichTextFormatController(state)
    }
}
