package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.richtext.textAlignment
import dev.mkeeda.arranger.richtext.underline
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun AdvancedFormattingSample(modifier: Modifier = Modifier) {
    val initialText =
        "Advanced Formatting Options\n" +
            "You can easily apply various text and paragraph styles.\n\n" +
            "Paragraph Styling\n" +
            "This paragraph is explicitly centered, overriding the default alignment.\n" +
            "> Blockquotes are perfect for highlighting external quotes or important notes."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.rangeOf("Advanced Formatting Options")) {
                            headingLevel(HeadingLevel.H1)
                        }
                        editAttributes(range = initialText.rangeOf("Paragraph Styling")) {
                            headingLevel(HeadingLevel.H3)
                        }
                        editAttributes(
                            range = initialText.rangeOf("This paragraph is explicitly centered, overriding the default alignment."),
                        ) {
                            textAlignment(TextAlignment.Center)
                        }
                        editAttributes(
                            range = initialText.rangeOf("> Blockquotes are perfect for highlighting external quotes or important notes."),
                        ) {
                            blockquote()
                        }
                        editAttributes(range = initialText.rangeOf("various text and paragraph styles")) {
                            textColor(Color(0xFFE91E63)) // Pink
                            bold()
                            underline()
                        }
                    },
            )
        }

    RichTextEditor(
        state = state,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
fun AdvancedFormattingSamplePreview() {
    ArrangerTheme {
        AdvancedFormattingSample()
    }
}
