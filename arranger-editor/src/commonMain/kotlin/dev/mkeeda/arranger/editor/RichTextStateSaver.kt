package dev.mkeeda.arranger.editor

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey

internal fun createRichTextStateSaver(
    customSerializers: List<AttributeSerializer<*>> = emptyList(),
): Saver<RichTextState, Any> {
    val customSerializersMap = customSerializers.associateBy { it.key.name }

    return Saver(
        save = { state ->
            val serializedSpans =
                serializeSpans(
                    scope = this,
                    spans = state.richString.spans,
                    customSerializers = customSerializersMap,
                )

            val serializedTypingAttributes =
                state.typingAttributes?.let {
                    serializeAttributeContainer(
                        scope = this,
                        container = it,
                        customSerializers = customSerializersMap,
                    )
                }

            val serializedRemovedTypingAttributes = state.removedTypingAttributes?.map { it.name }

            listOf(
                state.textFieldState.text.toString(),
                state.selection.start,
                state.selection.end,
                serializedSpans,
                serializedTypingAttributes,
                serializedRemovedTypingAttributes,
            )
        },
        restore = { saved ->
            val list = saved as? List<*> ?: return@Saver null
            if (list.size < 6) return@Saver null

            val text = list[0] as? String ?: return@Saver null
            val rawSelectionStart = (list[1] as? Number)?.toInt() ?: 0
            val rawSelectionEnd = (list[2] as? Number)?.toInt() ?: 0
            val selectionStart = rawSelectionStart.coerceIn(0, text.length)
            val selectionEnd = rawSelectionEnd.coerceIn(0, text.length)

            val rawSpans = list[3] as? List<*> ?: emptyList<Any?>()
            val restoredSpans =
                deserializeSpans(
                    serializedSpans = rawSpans,
                    customSerializers = customSerializersMap,
                )

            val richString =
                RichString(
                    text = text,
                    spans = restoredSpans,
                )

            val restoredState =
                RichTextState(
                    initialText = richString,
                    initialSelection = TextRange(start = selectionStart, end = selectionEnd),
                )

            val rawTyping = list[4] as? List<*>
            val restoredTypingAttributes =
                rawTyping?.let {
                    deserializeAttributeContainer(
                        serializedList = it,
                        customSerializers = customSerializersMap,
                    )
                }

            val rawRemoved = list[5] as? List<*>
            val restoredRemovedTypingAttributes =
                rawRemoved?.mapNotNull { item ->
                    val name = item as? String ?: return@mapNotNull null
                    findAttributeKey(name = name, customSerializers = customSerializersMap)
                }?.toSet()

            restoredState.restoreTypingAttributes(
                typing = restoredTypingAttributes,
                removed = restoredRemovedTypingAttributes,
            )

            restoredState
        },
    )
}

private val builtInKeysByName: Map<String, AttributeKey<*>> =
    listOf(
        BoldKey,
        ItalicKey,
        UnderlineKey,
        StrikethroughKey,
        InlineCodeKey,
        TextColorKey,
        BackgroundColorKey,
        FontSizeKey,
        HeadingKey,
        TextAlignmentKey,
        BlockquoteKey,
        BulletListKey,
        OrderedListKey,
        LinkKey,
    ).associateBy { it.name }

private fun findAttributeKey(
    name: String,
    customSerializers: Map<String, AttributeSerializer<*>>,
): AttributeKey<*>? {
    return builtInKeysByName[name] ?: customSerializers[name]?.key
}

private fun serializeAttribute(
    scope: SaverScope,
    key: AttributeKey<*>,
    value: Any?,
    customSerializers: Map<String, AttributeSerializer<*>>,
): Pair<String, Any?>? {
    val keyName = key.name
    return when (key) {
        BoldKey, ItalicKey, UnderlineKey, StrikethroughKey, InlineCodeKey, BlockquoteKey -> {
            keyName to null
        }

        CodeBlockKey -> {
            keyName to (value as? String)
        }

        TextColorKey, BackgroundColorKey -> {
            val color = value as? RgbaColor ?: return null
            keyName to color.value
        }

        FontSizeKey -> {
            val textSize = value as? TextSize ?: return null
            keyName to textSize.sp
        }

        HeadingKey -> {
            val heading = value as? HeadingLevel ?: return null
            keyName to heading.name
        }

        TextAlignmentKey -> {
            val alignment = value as? TextAlignment ?: return null
            keyName to alignment.name
        }

        BulletListKey, OrderedListKey -> {
            val indent = value as? ListIndentLevel ?: return null
            keyName to indent.name
        }

        LinkKey -> {
            val link = value as? String ?: return null
            keyName to link
        }

        else -> {
            val serializer = customSerializers[keyName] ?: return null

            @Suppress("UNCHECKED_CAST")
            val typedSerializer = serializer as AttributeSerializer<Any>
            val serializedValue = with(typedSerializer.saver) { scope.save(value ?: return null) }
            keyName to serializedValue
        }
    }
}

private fun deserializeAttribute(
    keyName: String,
    serializedValue: Any?,
    customSerializers: Map<String, AttributeSerializer<*>>,
): Pair<AttributeKey<*>, Any?>? {
    return when (keyName) {
        BoldKey.name -> {
            BoldKey to Unit
        }

        ItalicKey.name -> {
            ItalicKey to Unit
        }

        UnderlineKey.name -> {
            UnderlineKey to Unit
        }

        StrikethroughKey.name -> {
            StrikethroughKey to Unit
        }

        InlineCodeKey.name -> {
            InlineCodeKey to Unit
        }

        BlockquoteKey.name -> {
            BlockquoteKey to Unit
        }

        CodeBlockKey.name -> {
            CodeBlockKey to (serializedValue as? String)
        }

        TextColorKey.name -> {
            val colorValue = (serializedValue as? Long) ?: (serializedValue as? Number)?.toLong() ?: return null
            TextColorKey to RgbaColor(value = colorValue)
        }

        BackgroundColorKey.name -> {
            val colorValue = (serializedValue as? Long) ?: (serializedValue as? Number)?.toLong() ?: return null
            BackgroundColorKey to RgbaColor(value = colorValue)
        }

        FontSizeKey.name -> {
            val spValue = (serializedValue as? Float) ?: (serializedValue as? Number)?.toFloat() ?: return null
            FontSizeKey to TextSize(sp = spValue)
        }

        HeadingKey.name -> {
            val name = serializedValue as? String ?: return null
            val level = runCatching { HeadingLevel.valueOf(name) }.getOrDefault(HeadingLevel.Unspecified)
            HeadingKey to level
        }

        TextAlignmentKey.name -> {
            val name = serializedValue as? String ?: return null
            val alignment = runCatching { TextAlignment.valueOf(name) }.getOrDefault(TextAlignment.Unspecified)
            TextAlignmentKey to alignment
        }

        BulletListKey.name -> {
            val name = serializedValue as? String ?: return null
            val indent = runCatching { ListIndentLevel.valueOf(name) }.getOrDefault(ListIndentLevel.Unspecified)
            BulletListKey to indent
        }

        OrderedListKey.name -> {
            val name = serializedValue as? String ?: return null
            val indent = runCatching { ListIndentLevel.valueOf(name) }.getOrDefault(ListIndentLevel.Unspecified)
            OrderedListKey to indent
        }

        LinkKey.name -> {
            val url = serializedValue as? String ?: return null
            LinkKey to url
        }

        else -> {
            val serializer = customSerializers[keyName] ?: return null
            val restored = serializer.saver.restore(serializedValue ?: return null) ?: return null
            serializer.key to restored
        }
    }
}

private fun serializeAttributeContainer(
    scope: SaverScope,
    container: AttributeContainer,
    customSerializers: Map<String, AttributeSerializer<*>>,
): List<List<Any?>> {
    val result = ArrayList<List<Any?>>(container.size)
    for (key in container.keys) {
        val value = container.getOrDefault(key)
        val serialized =
            serializeAttribute(
                scope = scope,
                key = key,
                value = value,
                customSerializers = customSerializers,
            )
        if (serialized != null) {
            result.add(listOf(serialized.first, serialized.second))
        }
    }
    return result
}

private fun deserializeAttributeContainer(
    serializedList: List<*>,
    customSerializers: Map<String, AttributeSerializer<*>>,
): AttributeContainer {
    var container = AttributeContainer.empty()
    for (item in serializedList) {
        val entry = item as? List<*> ?: continue
        if (entry.size < 2) continue
        val keyName = entry[0] as? String ?: continue
        val serializedValue = entry[1]
        val deserialized =
            deserializeAttribute(
                keyName = keyName,
                serializedValue = serializedValue,
                customSerializers = customSerializers,
            )
        if (deserialized != null) {
            @Suppress("UNCHECKED_CAST")
            container += (deserialized.first as AttributeKey<Any?>) to deserialized.second
        }
    }
    return container
}

private fun serializeSpans(
    scope: SaverScope,
    spans: List<RichSpan>,
    customSerializers: Map<String, AttributeSerializer<*>>,
): List<List<Any?>> {
    val result = ArrayList<List<Any?>>(spans.size)
    for (span in spans) {
        val serializedAttributes =
            serializeAttributeContainer(
                scope = scope,
                container = span.attributes,
                customSerializers = customSerializers,
            )
        result.add(listOf(span.range.first, span.range.last, serializedAttributes))
    }
    return result
}

private fun deserializeSpans(
    serializedSpans: List<*>,
    customSerializers: Map<String, AttributeSerializer<*>>,
): List<RichSpan> {
    val result = ArrayList<RichSpan>(serializedSpans.size)
    for (item in serializedSpans) {
        val entry = item as? List<*> ?: continue
        if (entry.size < 3) continue
        val start = (entry[0] as? Number)?.toInt() ?: continue
        val end = (entry[1] as? Number)?.toInt() ?: continue
        if (start < 0 || end < 0 || start > end) continue
        val rawAttrs = entry[2] as? List<*> ?: continue
        val attributes =
            deserializeAttributeContainer(
                serializedList = rawAttrs,
                customSerializers = customSerializers,
            )
        if (attributes.isEmpty()) continue
        result.add(RichSpan(range = start..end, attributes = attributes))
    }
    return result
}
