package dev.mkeeda.arranger.sampleApp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.backgroundColor
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.clearBackgroundColor
import dev.mkeeda.arranger.richtext.clearBlockquote
import dev.mkeeda.arranger.richtext.clearBold
import dev.mkeeda.arranger.richtext.clearFontSize
import dev.mkeeda.arranger.richtext.clearHeadingLevel
import dev.mkeeda.arranger.richtext.clearItalic
import dev.mkeeda.arranger.richtext.clearStrikethrough
import dev.mkeeda.arranger.richtext.clearTextAlignment
import dev.mkeeda.arranger.richtext.clearTextColor
import dev.mkeeda.arranger.richtext.clearUnderline
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.editAttributes
import dev.mkeeda.arranger.richtext.fontSize
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.italic
import dev.mkeeda.arranger.richtext.strikethrough
import dev.mkeeda.arranger.richtext.textAlignment
import dev.mkeeda.arranger.richtext.textColor
import dev.mkeeda.arranger.richtext.underline
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun ChatInputSample(modifier: Modifier = Modifier) {
    val state = remember { RichTextState(initialText = RichString("")) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .padding(16.dp),
    ) {
        Text(
            text = "Chat UI Sample",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select text and use the toolbar to format it. Real-time cursor state reflection is not supported in this phase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        ChatInputBox(state = state)
    }
}

@Composable
private fun ChatInputBox(state: RichTextState, modifier: Modifier = Modifier) {
    // Toolbar buttons are enabled only when text is selected (selection length > 0)
    val hasSelection = !state.selection.collapsed

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        ChatInputField(state = state)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ChatFormattingToolbar(
            state = state,
            hasSelection = hasSelection,
        )
    }
}

@Composable
private fun ChatFormattingToolbar(
    state: RichTextState,
    hasSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { bold() } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_bold),
                contentDescription = "Bold",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { italic() } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_italic),
                contentDescription = "Italic",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { underline() } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_underlined),
                contentDescription = "Underline",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { strikethrough() } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_strikethrough),
                contentDescription = "Strikethrough",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { textColor(RgbaColor(0xFFFF0000.toLong())) } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_color_text),
                contentDescription = "Text Color Red",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { backgroundColor(RgbaColor(0xFFFFFF00.toLong())) } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_color_fill),
                contentDescription = "Background Color Yellow",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { fontSize(TextSize(24f)) } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_size),
                contentDescription = "Large Font Size",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { headingLevel(HeadingLevel.H1) } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_h1),
                contentDescription = "Heading 1",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { textAlignment(TextAlignment.Center) } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_align_center),
                contentDescription = "Align Center",
            )
        }
        IconButton(
            onClick = {
                state.edit { editAttributes(state.selection) { blockquote() } }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_quote),
                contentDescription = "Blockquote",
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                state.edit {
                    editAttributes(state.selection) {
                        clearBold()
                        clearItalic()
                        clearStrikethrough()
                        clearUnderline()
                        clearTextColor()
                        clearBackgroundColor()
                        clearFontSize()
                        clearHeadingLevel()
                        clearTextAlignment()
                        clearBlockquote()
                    }
                }
            },
            enabled = hasSelection,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_clear),
                contentDescription = "Clear Formatting",
            )
        }
    }
}

@Composable
private fun ChatInputField(state: RichTextState, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp),
    ) {
        if (state.richString.text.isEmpty()) {
            Text(
                text = "Type a message...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        RichTextEditor(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("ChatInputEditor"),
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 5),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatInputSamplePreview() {
    ArrangerTheme {
        ChatInputSample()
    }
}
