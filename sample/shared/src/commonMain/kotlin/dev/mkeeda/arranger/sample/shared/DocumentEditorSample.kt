package dev.mkeeda.arranger.sample.shared

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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import arranger.sample.shared.generated.resources.Res
import arranger.sample.shared.generated.resources.format_align_center
import arranger.sample.shared.generated.resources.format_bold
import arranger.sample.shared.generated.resources.format_clear
import arranger.sample.shared.generated.resources.format_color_fill
import arranger.sample.shared.generated.resources.format_color_text
import arranger.sample.shared.generated.resources.format_h1
import arranger.sample.shared.generated.resources.format_indent_decrease
import arranger.sample.shared.generated.resources.format_indent_increase
import arranger.sample.shared.generated.resources.format_italic
import arranger.sample.shared.generated.resources.format_list_bulleted
import arranger.sample.shared.generated.resources.format_list_numbered
import arranger.sample.shared.generated.resources.format_quote
import arranger.sample.shared.generated.resources.format_size
import arranger.sample.shared.generated.resources.format_strikethrough
import arranger.sample.shared.generated.resources.format_underlined
import arranger.sample.shared.generated.resources.redo
import arranger.sample.shared.generated.resources.undo
import dev.mkeeda.arranger.richtext.BackgroundColorKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.FontSizeKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RgbaColor
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.TextAlignmentKey
import dev.mkeeda.arranger.richtext.TextColorKey
import dev.mkeeda.arranger.richtext.TextSize
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.applyFormat
import dev.mkeeda.arranger.richtext.editor.clearFormats
import dev.mkeeda.arranger.richtext.editor.material3.rememberMaterial3AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.removeFormat
import dev.mkeeda.arranger.richtext.editor.toggleFormat
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun DocumentEditorSample(modifier: Modifier = Modifier) {
    val state = remember { RichTextState() }

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
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DocumentEditorField(state = state, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DocumentFormattingToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier,
) {
    val unfocusableModifier = Modifier.focusProperties { canFocus = false }
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
            modifier = unfocusableModifier,
        ) {
            Icon(
                painter = painterResource(Res.drawable.undo),
                contentDescription = "Undo",
            )
        }
        IconButton(
            onClick = { state.undoState.redo() },
            enabled = state.undoState.canRedo,
            modifier = unfocusableModifier,
        ) {
            Icon(
                painter = painterResource(Res.drawable.redo),
                contentDescription = "Redo",
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        FormatToggleButton(
            iconRes = Res.drawable.format_bold,
            contentDescription = "Bold",
            isActive = state.currentAttributes.containsKey(BoldKey),
            onClick = { state.toggleFormat(BoldKey) },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_italic,
            contentDescription = "Italic",
            isActive = state.currentAttributes.containsKey(ItalicKey),
            onClick = { state.toggleFormat(ItalicKey) },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_underlined,
            contentDescription = "Underline",
            isActive = state.currentAttributes.containsKey(UnderlineKey),
            onClick = { state.toggleFormat(UnderlineKey) },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_strikethrough,
            contentDescription = "Strikethrough",
            isActive = state.currentAttributes.containsKey(StrikethroughKey),
            onClick = { state.toggleFormat(StrikethroughKey) },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_underlined,
            contentDescription = "Hyperlink",
            isActive = state.currentAttributes.containsKey(LinkKey),
            onClick = {
                if (state.currentAttributes.containsKey(LinkKey)) {
                    state.removeFormat(LinkKey)
                } else {
                    state.applyFormat(LinkKey, "https://example.com")
                }
            },
            modifier = unfocusableModifier,
        )

        FormatToggleButton(
            iconRes = Res.drawable.format_color_text,
            contentDescription = "Text Color Red",
            isActive = state.currentAttributes[TextColorKey] == RgbaColor(0xFFFF0000),
            onClick = {
                if (state.currentAttributes[TextColorKey] == RgbaColor(0xFFFF0000)) {
                    state.removeFormat(TextColorKey)
                } else {
                    state.applyFormat(TextColorKey, RgbaColor(0xFFFF0000))
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_color_fill,
            contentDescription = "Background Color Yellow",
            isActive = state.currentAttributes[BackgroundColorKey] == RgbaColor(0xFFFFFF00),
            onClick = {
                if (state.currentAttributes[BackgroundColorKey] == RgbaColor(0xFFFFFF00)) {
                    state.removeFormat(BackgroundColorKey)
                } else {
                    state.applyFormat(BackgroundColorKey, RgbaColor(0xFFFFFF00))
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_size,
            contentDescription = "Large Font Size",
            isActive = state.currentAttributes[FontSizeKey] == TextSize(24f),
            onClick = {
                if (state.currentAttributes[FontSizeKey] == TextSize(24f)) {
                    state.removeFormat(FontSizeKey)
                } else {
                    state.applyFormat(FontSizeKey, TextSize(24f))
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_h1,
            contentDescription = "Heading 1",
            isActive = state.currentAttributes[HeadingKey] == HeadingLevel.H1,
            onClick = {
                if (state.currentAttributes[HeadingKey] == HeadingLevel.H1) {
                    state.removeFormat(HeadingKey)
                } else {
                    state.applyFormat(HeadingKey, HeadingLevel.H1)
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_align_center,
            contentDescription = "Align Center",
            isActive = state.currentAttributes[TextAlignmentKey] == TextAlignment.Center,
            onClick = {
                if (state.currentAttributes[TextAlignmentKey] == TextAlignment.Center) {
                    state.removeFormat(TextAlignmentKey)
                } else {
                    state.applyFormat(TextAlignmentKey, TextAlignment.Center)
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_quote,
            contentDescription = "Blockquote",
            isActive = state.currentAttributes.containsKey(BlockquoteKey),
            onClick = { state.toggleFormat(BlockquoteKey) },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_list_bulleted,
            contentDescription = "Bullet List",
            isActive = state.currentAttributes.containsKey(BulletListKey),
            onClick = {
                if (state.currentAttributes.containsKey(BulletListKey)) {
                    state.removeFormat(BulletListKey)
                } else {
                    state.applyFormat(BulletListKey, ListIndentLevel.Level1)
                }
            },
            modifier = unfocusableModifier,
        )
        FormatToggleButton(
            iconRes = Res.drawable.format_list_numbered,
            contentDescription = "Ordered List",
            isActive = state.currentAttributes.containsKey(OrderedListKey),
            onClick = {
                if (state.currentAttributes.containsKey(OrderedListKey)) {
                    state.removeFormat(OrderedListKey)
                } else {
                    state.applyFormat(OrderedListKey, ListIndentLevel.Level1)
                }
            },
            modifier = unfocusableModifier,
        )

        OutdentButton(state = state, modifier = unfocusableModifier)
        IndentButton(state = state, modifier = unfocusableModifier)

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { state.clearFormats() },
            enabled = true,
            modifier = unfocusableModifier,
        ) {
            Icon(
                painter = painterResource(Res.drawable.format_clear),
                contentDescription = "Clear Formatting",
            )
        }
    }
}

@Composable
private fun FormatToggleButton(
    iconRes: DrawableResource,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    IconToggleButton(
        checked = isActive,
        onCheckedChange = { onClick() },
        enabled = true,
        colors = colors,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
        )
    }
}

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

@Composable
private fun OutdentButton(
    state: RichTextState,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = {
            val currentLevel =
                state.currentAttributes[BulletListKey]
                    ?: state.currentAttributes[OrderedListKey]
            if (currentLevel != null && currentLevel.ordinal > 0) {
                val prevLevel = ListIndentLevel.entries[currentLevel.ordinal - 1]
                if (state.currentAttributes.containsKey(BulletListKey)) {
                    state.applyFormat(BulletListKey, prevLevel)
                } else {
                    state.applyFormat(OrderedListKey, prevLevel)
                }
            } else if (currentLevel != null) {
                state.removeFormat(BulletListKey)
                state.removeFormat(OrderedListKey)
            }
        },
        enabled = state.currentAttributes.containsAny(BulletListKey, OrderedListKey),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(Res.drawable.format_indent_decrease),
            contentDescription = "Outdent",
        )
    }
}

@Composable
private fun IndentButton(
    state: RichTextState,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = {
            val currentLevel =
                state.currentAttributes[BulletListKey]
                    ?: state.currentAttributes[OrderedListKey]
            if (currentLevel != null && currentLevel.ordinal < ListIndentLevel.Level6.ordinal) {
                val nextLevel = ListIndentLevel.entries[currentLevel.ordinal + 1]
                if (state.currentAttributes.containsKey(BulletListKey)) {
                    state.applyFormat(BulletListKey, nextLevel)
                } else {
                    state.applyFormat(OrderedListKey, nextLevel)
                }
            }
        },
        enabled = state.currentAttributes.containsAny(BulletListKey, OrderedListKey),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(Res.drawable.format_indent_increase),
            contentDescription = "Indent",
        )
    }
}
