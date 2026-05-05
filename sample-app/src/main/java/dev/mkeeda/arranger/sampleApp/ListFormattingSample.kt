package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.BulletListItem
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListItem
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.editor.ListMarkerResolver
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.orderedList

@Composable
fun ListFormattingSample(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text("List Formatting Sample", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Bullet List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        BulletListSample()

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Ordered List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OrderedListSample()

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Custom List Marker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        CustomListMarkerSample()
    }
}

@Composable
fun BulletListSample(modifier: Modifier = Modifier) {
    val initialText =
        "Bullet Items:\n" +
            "First item\n" +
            "Second item\n" +
            "Third item\n" +
            "Nested item 1\n" +
            "Nested item 2"

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        val itemsStart = initialText.indexOf("First item")
                        val itemsEnd = initialText.indexOf("Nested item 1") - 1
                        editAttributes(itemsStart until itemsEnd) {
                            bulletList(ListIndentLevel.Level1)
                        }

                        val nestedStart = initialText.indexOf("Nested item 1")
                        val nestedEnd = initialText.length
                        editAttributes(nestedStart until nestedEnd) {
                            bulletList(ListIndentLevel.Level2)
                        }
                    },
            )
        }

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun OrderedListSample(modifier: Modifier = Modifier) {
    val initialText =
        "Steps to follow:\n" +
            "Prepare ingredients\n" +
            "Cook the meal\n" +
            "Serve on plates"

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        val start = initialText.indexOf("Prepare ingredients")
                        val end = initialText.length
                        editAttributes(start until end) {
                            orderedList(ListIndentLevel.Level1)
                        }
                    },
            )
        }

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun CustomListMarkerSample(modifier: Modifier = Modifier) {
    val initialText =
        "Checklist:\n" +
            "Review code\n" +
            "Run tests\n" +
            "Deploy\n" +
            "Priorities:\n" +
            "Critical bugs\n" +
            "New features\n" +
            "Refactoring"

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        val start = initialText.indexOf("Review code")
                        val end = initialText.indexOf("Priorities:") - 1
                        editAttributes(start until end) {
                            bulletList(ListIndentLevel.Level1)
                        }

                        val orderedStart = initialText.indexOf("Critical bugs")
                        val orderedEnd = initialText.length
                        editAttributes(orderedStart until orderedEnd) {
                            orderedList(ListIndentLevel.Level1)
                        }
                    },
            )
        }

    val customMarkerResolver =
        remember {
            ListMarkerResolver { item ->
                when (item) {
                    is BulletListItem -> "✔️ "
                    is OrderedListItem -> "${('a' + item.index - 1)}) "
                }
            }
        }

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
        listMarkerResolver = customMarkerResolver,
    )
}

@Preview(showBackground = true)
@Composable
private fun BulletListSamplePreview() {
    BulletListSample()
}

@Preview(showBackground = true)
@Composable
private fun OrderedListSamplePreview() {
    OrderedListSample()
}

@Preview(showBackground = true)
@Composable
private fun CustomListMarkerSamplePreview() {
    CustomListMarkerSample()
}
