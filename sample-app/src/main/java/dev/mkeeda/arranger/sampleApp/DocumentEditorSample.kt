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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextFormatController
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.material3.rememberMaterial3AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.rememberRichTextFormatController
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

@Composable
fun DocumentEditorSample(modifier: Modifier = Modifier) {
    val state = remember { RichTextState(initialText = RichString("")) }

    DocumentEditorBox(
        state = state,
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
    )
}

@Composable
private fun DocumentEditorBox(state: RichTextState, modifier: Modifier = Modifier) {
    val formatController = rememberRichTextFormatController(state)

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
        DocumentFormattingToolbar(
            state = state,
            formatController = formatController,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DocumentEditorField(state = state, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DocumentFormattingToolbar(
    state: RichTextState,
    formatController: RichTextFormatController,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        // Undo / Redo Buttons
        IconButton(
            onClick = { state.undoState.undo() },
            enabled = state.undoState.canUndo,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.undo),
                contentDescription = "Undo",
            )
        }
        IconButton(
            onClick = { state.undoState.redo() },
            enabled = state.undoState.canRedo,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.redo),
                contentDescription = "Redo",
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        val formatActions =
            listOf(
                FormatAction(
                    iconRes = R.drawable.format_bold,
                    contentDescription = "Bold",
                    isActive = formatController.isActive(BoldKey),
                    onClick = { formatController.toggle(BoldKey) },
                ),
                FormatAction(
                    iconRes = R.drawable.format_italic,
                    contentDescription = "Italic",
                    isActive = formatController.isActive(ItalicKey),
                    onClick = { formatController.toggle(ItalicKey) },
                ),
                FormatAction(
                    iconRes = R.drawable.format_underlined,
                    contentDescription = "Underline",
                    isActive = formatController.isActive(UnderlineKey),
                    onClick = { formatController.toggle(UnderlineKey) },
                ),
                FormatAction(
                    iconRes = R.drawable.format_strikethrough,
                    contentDescription = "Strikethrough",
                    isActive = formatController.isActive(StrikethroughKey),
                    onClick = { formatController.toggle(StrikethroughKey) },
                ),
                FormatAction(
                    iconRes = R.drawable.format_color_text,
                    contentDescription = "Text Color Red",
                    isActive = formatController.getCurrentValue(TextColorKey) == RgbaColor(0xFFFF0000),
                    onClick = {
                        if (formatController.getCurrentValue(TextColorKey) == RgbaColor(0xFFFF0000)) {
                            formatController.remove(TextColorKey)
                        } else {
                            formatController.apply(TextColorKey, RgbaColor(0xFFFF0000))
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_color_fill,
                    contentDescription = "Background Color Yellow",
                    isActive = formatController.getCurrentValue(BackgroundColorKey) == RgbaColor(0xFFFFFF00),
                    onClick = {
                        if (formatController.getCurrentValue(BackgroundColorKey) == RgbaColor(0xFFFFFF00)) {
                            formatController.remove(BackgroundColorKey)
                        } else {
                            formatController.apply(BackgroundColorKey, RgbaColor(0xFFFFFF00))
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_size,
                    contentDescription = "Large Font Size",
                    isActive = formatController.getCurrentValue(FontSizeKey) == TextSize(24f),
                    onClick = {
                        if (formatController.getCurrentValue(FontSizeKey) == TextSize(24f)) {
                            formatController.remove(FontSizeKey)
                        } else {
                            formatController.apply(FontSizeKey, TextSize(24f))
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_h1,
                    contentDescription = "Heading 1",
                    isActive = formatController.getCurrentValue(HeadingKey) == HeadingLevel.H1,
                    onClick = {
                        if (formatController.getCurrentValue(HeadingKey) == HeadingLevel.H1) {
                            formatController.remove(HeadingKey)
                        } else {
                            formatController.apply(HeadingKey, HeadingLevel.H1)
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_align_center,
                    contentDescription = "Align Center",
                    isActive = formatController.getCurrentValue(TextAlignmentKey) == TextAlignment.Center,
                    onClick = {
                        if (formatController.getCurrentValue(TextAlignmentKey) == TextAlignment.Center) {
                            formatController.remove(TextAlignmentKey)
                        } else {
                            formatController.apply(TextAlignmentKey, TextAlignment.Center)
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_quote,
                    contentDescription = "Blockquote",
                    isActive = formatController.isActive(BlockquoteKey),
                    onClick = { formatController.toggle(BlockquoteKey) },
                ),
                FormatAction(
                    iconRes = R.drawable.format_list_bulleted,
                    contentDescription = "Bullet List",
                    isActive = formatController.isActive(BulletListKey),
                    onClick = {
                        if (formatController.isActive(BulletListKey)) {
                            formatController.remove(BulletListKey)
                        } else {
                            formatController.apply(BulletListKey, ListIndentLevel.Level1)
                        }
                    },
                ),
                FormatAction(
                    iconRes = R.drawable.format_list_numbered,
                    contentDescription = "Ordered List",
                    isActive = formatController.isActive(OrderedListKey),
                    onClick = {
                        if (formatController.isActive(OrderedListKey)) {
                            formatController.remove(OrderedListKey)
                        } else {
                            formatController.apply(OrderedListKey, ListIndentLevel.Level1)
                        }
                    },
                ),
            )

        formatActions.forEach { action ->
            IconToggleButton(
                checked = action.isActive,
                onCheckedChange = { action.onClick() },
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

        IndentOutdentButtons(state = state, formatController = formatController)

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { formatController.clearAll() },
            enabled = true,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.format_clear),
                contentDescription = "Clear Formatting",
            )
        }
    }
}

private class FormatAction(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val isActive: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun DocumentEditorField(state: RichTextState, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp),
    ) {
        if (state.richString.text.isEmpty()) {
            Text(
                text = "Type your document...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        RichTextEditor(
            state = state,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("DocumentEditor"),
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            styleResolver = rememberMaterial3AttributeStyleResolver(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DocumentEditorSamplePreview() {
    ArrangerTheme {
        DocumentEditorSample()
    }
}

@Composable
private fun IndentOutdentButtons(
    state: RichTextState,
    formatController: RichTextFormatController,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = {
            val currentLevel =
                formatController.getCurrentValue(BulletListKey)
                    ?: formatController.getCurrentValue(OrderedListKey)
            if (currentLevel != null && currentLevel.ordinal > 0) {
                val prevLevel = ListIndentLevel.entries[currentLevel.ordinal - 1]
                if (formatController.isActive(BulletListKey)) {
                    formatController.apply(BulletListKey, prevLevel)
                } else {
                    formatController.apply(OrderedListKey, prevLevel)
                }
            } else if (currentLevel != null) {
                formatController.remove(BulletListKey)
                formatController.remove(OrderedListKey)
            }
        },
        enabled = formatController.isActive(BulletListKey) || formatController.isActive(OrderedListKey),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.format_indent_decrease),
            contentDescription = "Outdent",
        )
    }

    IconButton(
        onClick = {
            val currentLevel =
                formatController.getCurrentValue(BulletListKey)
                    ?: formatController.getCurrentValue(OrderedListKey)
            if (currentLevel != null && currentLevel.ordinal < ListIndentLevel.Level6.ordinal) {
                val nextLevel = ListIndentLevel.entries[currentLevel.ordinal + 1]
                if (formatController.isActive(BulletListKey)) {
                    formatController.apply(BulletListKey, nextLevel)
                } else {
                    formatController.apply(OrderedListKey, nextLevel)
                }
            }
        },
        enabled = formatController.isActive(BulletListKey) || formatController.isActive(OrderedListKey),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.format_indent_increase),
            contentDescription = "Indent",
        )
    }
}
