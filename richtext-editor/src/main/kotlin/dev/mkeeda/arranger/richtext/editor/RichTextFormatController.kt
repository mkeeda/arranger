package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.ParagraphAttributeKey
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.editor.RichTextState

public class RichTextFormatController(private val state: RichTextState) {
    public fun isActive(key: AttributeKey<*>): Boolean {
        return state.currentAttributes.containsKey(key)
    }

    public fun <T : Any> getCurrentValue(key: AttributeKey<T>): T? {
        return state.currentAttributes[key]
    }

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

    public fun <T : Any> apply(key: ParagraphAttributeKey<T>, value: T) {
        state.edit {
            editAttributes(state.selection) {
                setParagraphAttribute(key, value)
            }
        }
    }

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

@Composable
public fun rememberRichTextFormatController(state: RichTextState): RichTextFormatController {
    return remember(state) {
        RichTextFormatController(state)
    }
}
