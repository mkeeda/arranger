package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.orderedList

@Composable
fun ListFormattingSample(modifier: Modifier = Modifier) {
    val initialText =
        remember {
            val plainText =
                "List Formatting Features\n\n" +
                    "Bullet Item 1\n" +
                    "Bullet Item 2\n" +
                    "Bullet Item 3\n\n" +
                    "First step\n" +
                    "Second step\n" +
                    "Third step\n\n" +
                    "Nested Lists\n" +
                    "Parent item\n" +
                    "Child item 1\n" +
                    "Child item 2\n"

            RichString(plainText).edit {
                val b1 = plainText.indexOf("Bullet Item 1")
                val bEnd = plainText.indexOf("First step") - 1
                editAttributes(b1 until bEnd) {
                    bulletList(ListIndentLevel.Level1)
                }

                val o1 = plainText.indexOf("First step")
                val oEnd = plainText.indexOf("Nested Lists") - 1
                editAttributes(o1 until oEnd) {
                    orderedList(ListIndentLevel.Level1)
                }

                val p1 = plainText.indexOf("Parent item")
                val pEnd = plainText.indexOf("Child item 1")
                editAttributes(p1 until pEnd) {
                    bulletList(ListIndentLevel.Level1)
                }

                val c1 = plainText.indexOf("Child item 1")
                val c2End = plainText.length
                editAttributes(c1 until c2End) {
                    bulletList(ListIndentLevel.Level2)
                }
            }
        }

    val state = remember { RichTextState(initialText) }

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
