package dev.mkeeda.arranger.sampleApp

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
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
import dev.mkeeda.arranger.richtext.AttributeEditScope
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.backgroundColor
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.bulletList
import dev.mkeeda.arranger.richtext.clearBackgroundColor
import dev.mkeeda.arranger.richtext.clearBlockquote
import dev.mkeeda.arranger.richtext.clearBold
import dev.mkeeda.arranger.richtext.clearBulletList
import dev.mkeeda.arranger.richtext.clearFontSize
import dev.mkeeda.arranger.richtext.clearHeadingLevel
import dev.mkeeda.arranger.richtext.clearItalic
import dev.mkeeda.arranger.richtext.clearOrderedList
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
import dev.mkeeda.arranger.richtext.orderedList
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
                .imePadding(),
    ) {
        Text(
            text = "Select text and use the toolbar to format it, or use the toolbar before typing.",
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
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        val formatActions =
            listOf(
                FormatAction(R.drawable.format_bold, "Bold", BoldKey, Unit) { bold() },
                FormatAction(R.drawable.format_italic, "Italic", ItalicKey, Unit) { italic() },
                FormatAction(R.drawable.format_underlined, "Underline", UnderlineKey, Unit) { underline() },
                FormatAction(R.drawable.format_strikethrough, "Strikethrough", StrikethroughKey, Unit) { strikethrough() },
                FormatAction(R.drawable.format_color_text, "Text Color Red", TextColorKey, RgbaColor(0xFFFF0000.toLong())) {
                    textColor(RgbaColor(0xFFFF0000.toLong()))
                },
                FormatAction(R.drawable.format_color_fill, "Background Color Yellow", BackgroundColorKey, RgbaColor(0xFFFFFF00.toLong())) {
                    backgroundColor(RgbaColor(0xFFFFFF00.toLong()))
                },
                FormatAction(R.drawable.format_size, "Large Font Size", FontSizeKey, TextSize(24f)) { fontSize(TextSize(24f)) },
                FormatAction(R.drawable.format_h1, "Heading 1", HeadingKey, HeadingLevel.H1) { headingLevel(HeadingLevel.H1) },
                FormatAction(R.drawable.format_align_center, "Align Center", TextAlignmentKey, TextAlignment.Center) {
                    textAlignment(TextAlignment.Center)
                },
                FormatAction(R.drawable.format_quote, "Blockquote", BlockquoteKey, Unit) { blockquote() },
                FormatAction(
                    R.drawable.format_list_bulleted,
                    "Bullet List",
                    BulletListKey,
                    ListIndentLevel.Level1,
                ) { bulletList(ListIndentLevel.Level1) },
                FormatAction(R.drawable.format_list_numbered, "Ordered List", OrderedListKey, ListIndentLevel.Level1) {
                    orderedList(ListIndentLevel.Level1)
                },
            )

        formatActions.forEach { action ->
            val isActive = state.currentAttributes.containsKey(action.key)
            IconToggleButton(
                checked = isActive,
                onCheckedChange = {
                    if (hasSelection) {
                        state.edit { editAttributes(state.selection) { action.applySelection(this) } }
                    } else {
                        toggleTypingAttribute(state, action, isActive)
                    }
                },
                enabled = true,
                colors =
                    IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = action.iconRes),
                    contentDescription = action.contentDescription,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                if (hasSelection) {
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
                            clearBulletList()
                            clearOrderedList()
                        }
                    }
                } else {
                    state.clearTypingAttributes()
                }
            },
            enabled = true,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_clear),
                contentDescription = "Clear Formatting",
            )
        }
    }
}

private fun <T : Any> toggleTypingAttribute(
    state: RichTextState,
    action: FormatAction<T>,
    isActive: Boolean,
) {
    if (isActive) {
        state.removeTypingAttribute(action.key)
    } else {
        state.setTypingAttribute(action.key, action.value)
    }
}

private data class FormatAction<T : Any>(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val key: AttributeKey<T>,
    val value: T,
    val applySelection: AttributeEditScope.() -> Unit,
)

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
