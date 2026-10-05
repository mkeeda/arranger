package dev.mkeeda.arranger.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import dev.mkeeda.arranger.richtext.RichString

/**
 * Creates and remembers a [RichTextState] across configuration changes and process death
 * using [RichTextState.Saver].
 *
 * For large document editing where Bundle size limits (TransactionTooLargeException) are
 * a concern, or where undo/redo history must survive configuration changes, hoist [RichTextState]
 * directly in a ViewModel instead of using [rememberRichTextState].
 *
 * @param initialText The initial [RichString] to populate the editor state with. Defaults to empty.
 */
@Composable
public fun rememberRichTextState(
    initialText: RichString = RichString(""),
): RichTextState =
    rememberSaveable(saver = RichTextState.Saver) {
        RichTextState(initialText = initialText)
    }

/**
 * Creates and remembers a [RichTextState] with a custom [saver] (e.g. supporting custom attribute keys).
 *
 * @param initialText The initial [RichString] to populate the editor state with. Defaults to empty.
 * @param saver The custom [Saver] to use for saving and restoring the [RichTextState].
 */
@Composable
public fun rememberRichTextState(
    initialText: RichString = RichString(""),
    saver: Saver<RichTextState, *>,
): RichTextState =
    rememberSaveable(saver = saver) {
        RichTextState(initialText = initialText)
    }
