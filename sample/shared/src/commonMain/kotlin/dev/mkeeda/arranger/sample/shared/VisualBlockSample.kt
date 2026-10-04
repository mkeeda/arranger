package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.clearCodeBlock
import dev.mkeeda.arranger.richtext.codeBlock
import dev.mkeeda.arranger.richtext.editor.DefaultBlockDecorator
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.material3.rememberMaterial3AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.material3.rememberMaterial3BlockDecorator
import dev.mkeeda.arranger.richtext.editor.toggleFormat
import dev.mkeeda.arranger.richtext.rangeOf

private enum class DecoratorStyle(val label: String) {
    Material3("Material 3"),
    Default("Default"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun VisualBlockSample(modifier: Modifier = Modifier) {
    val initialText =
        "Visual Block Decorations & Code Blocks\n" +
            "Simplicity is prerequisite for reliability.\n" +
            "- Edsger W. Dijkstra\n" +
            "fun greet(name: String) {\n" +
            "    println(\"Hello, \$name!\")\n" +
            "}\n" +
            "./gradlew allTests\n" +
            "Try adding or editing blocks below."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(initialText).edit {
                        val quoteRange = initialText.rangeOf("Simplicity is prerequisite for reliability.\n- Edsger W. Dijkstra")
                        editAttributes(quoteRange) {
                            blockquote()
                        }

                        val kotlinCodeRange = initialText.rangeOf("fun greet(name: String) {\n    println(\"Hello, \$name!\")\n}")
                        editAttributes(kotlinCodeRange) {
                            codeBlock(language = "kotlin")
                        }

                        val bashCodeRange = initialText.rangeOf("./gradlew allTests")
                        editAttributes(bashCodeRange) {
                            codeBlock(language = "bash")
                        }
                    },
            )
        }

    var selectedStyle by remember { mutableStateOf(DecoratorStyle.Material3) }

    val m3Decorator = rememberMaterial3BlockDecorator()
    val activeDecorator =
        when (selectedStyle) {
            DecoratorStyle.Material3 -> m3Decorator
            DecoratorStyle.Default -> DefaultBlockDecorator
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Block Decorator Style",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DecoratorStyle.entries.forEach { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { selectedStyle = style },
                    label = { Text(style.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { state.toggleBlockquote() },
            ) {
                Text("Toggle Quote")
            }
            OutlinedButton(
                onClick = { state.toggleCodeBlock(language = "kotlin") },
            ) {
                Text("Code (Kotlin)")
            }
            OutlinedButton(
                onClick = { state.toggleCodeBlock(language = null) },
            ) {
                Text("Code (Plain)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Editor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        RichTextEditor(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            styleResolver = rememberMaterial3AttributeStyleResolver(),
            blockDecorator = activeDecorator,
        )
    }
}

private fun RichTextState.toggleBlockquote() {
    toggleFormat(BlockquoteKey)
}

private fun RichTextState.toggleCodeBlock(language: String?) {
    val hasCode = currentAttributes.containsKey(CodeBlockKey)
    edit {
        editAttributes(selection.min until selection.max) {
            if (hasCode) {
                clearCodeBlock()
            } else {
                codeBlock(language = language)
            }
        }
    }
}
