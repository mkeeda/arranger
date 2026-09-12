package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.wysiwyg.WysiwygEditor
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.inlineCode
import dev.mkeeda.arranger.richtext.italic
import dev.mkeeda.arranger.richtext.rangeOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun WysiwygEditorSample(modifier: Modifier = Modifier) {
    val initialContent =
        "Welcome to WYSIWYG Editor\n" +
            "Type markdown shortcuts to format text dynamically:\n" +
            "- Bullet list item with bold and inline code\n" +
            "> Blockquotes are styled in real time\n" +
            "Press Backspace right after auto-formatting to revert."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(initialContent).edit {
                        editAttributes(range = initialContent.rangeOf("Welcome to WYSIWYG Editor")) {
                            headingLevel(HeadingLevel.H1)
                        }
                        editAttributes(
                            range = initialContent.rangeOf("- Bullet list item with bold and inline code"),
                        ) {
                            bulletList(ListIndentLevel.Level1)
                        }
                        editAttributes(range = initialContent.rangeOf("bold")) {
                            bold()
                        }
                        editAttributes(range = initialContent.rangeOf("inline code")) {
                            inlineCode()
                        }
                        editAttributes(
                            range = initialContent.rangeOf("> Blockquotes are styled in real time"),
                        ) {
                            blockquote()
                        }
                        editAttributes(range = initialContent.rangeOf("revert")) {
                            italic()
                        }
                    },
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Supported Markdown Shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SuggestionChip(onClick = {}, label = { Text("# H1  ## H2  ### H3") })
                    SuggestionChip(onClick = {}, label = { Text("- or * Bullet list") })
                    SuggestionChip(onClick = {}, label = { Text("1. Ordered list") })
                    SuggestionChip(onClick = {}, label = { Text("> Blockquote") })
                    SuggestionChip(onClick = {}, label = { Text("**bold**") })
                    SuggestionChip(onClick = {}, label = { Text("*italic* or _italic_") })
                    SuggestionChip(onClick = {}, label = { Text("`code`") })
                    SuggestionChip(onClick = {}, label = { Text("~strike~") })
                    SuggestionChip(onClick = {}, label = { Text("Backspace to revert") })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                WysiwygEditor(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
