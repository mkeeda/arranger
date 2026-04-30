package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.buildRichString
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.rememberRichTextState
import dev.mkeeda.arranger.richtext.orderedList

@Composable
fun ListFormattingSample(modifier: Modifier = Modifier) {
    val initialText =
        remember {
            buildRichString {
                append("List Formatting Features\n\n")

                // Bullet lists
                val bulletStart = length
                append("Bullet Item 1\n")
                append("Bullet Item 2\n")
                append("Bullet Item 3\n")
                editAttributes(range = bulletStart until length) {
                    bulletList(ListIndentLevel.Level1)
                }

                append("\n")

                // Ordered lists
                val orderedStart = length
                append("First step\n")
                append("Second step\n")
                append("Third step\n")
                editAttributes(range = orderedStart until length) {
                    orderedList(ListIndentLevel.Level1)
                }

                append("\nNested Lists\n")
                val nestedStart = length
                append("Parent item\n")
                editAttributes(range = nestedStart until length) {
                    bulletList(ListIndentLevel.Level1)
                }

                val child1Start = length
                append("Child item 1\n")
                editAttributes(range = child1Start until length) {
                    bulletList(ListIndentLevel.Level2)
                }

                val child2Start = length
                append("Child item 2\n")
                editAttributes(range = child2Start until length) {
                    bulletList(ListIndentLevel.Level2)
                }
            }
        }

    val state = rememberRichTextState(initialText)

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        scrollState = rememberScrollState(),
    )
}
