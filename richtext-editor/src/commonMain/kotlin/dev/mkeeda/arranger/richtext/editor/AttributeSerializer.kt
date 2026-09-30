package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import dev.mkeeda.arranger.richtext.AttributeKey

/**
 * Defines how to serialize and deserialize custom [AttributeKey] values
 * for [RichTextState.Saver].
 *
 * @param T The type of the attribute value.
 * @property key The [AttributeKey] to be serialized.
 * @property saver The Compose [Saver] used to save and restore values of type [T].
 */
public class AttributeSerializer<T>(
    public val key: AttributeKey<T>,
    public val saver: Saver<T, Any>,
)

/**
 * Creates an [AttributeSerializer] using functional save and restore lambdas.
 *
 * @param key The [AttributeKey] to serialize and deserialize.
 * @param save Lambda describing how to serialize an instance of [T].
 * @param restore Lambda describing how to restore an instance of [T] from the saved representation.
 */
public fun <T> attributeSerializer(
    key: AttributeKey<T>,
    save: SaverScope.(value: T) -> Any?,
    restore: (value: Any) -> T?,
): AttributeSerializer<T> =
    AttributeSerializer(
        key = key,
        saver = Saver(save = save, restore = restore),
    )
