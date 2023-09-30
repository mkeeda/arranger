package dev.mkeeda.arranger.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.google.common.truth.Truth.assertThat
import dev.mkeeda.arranger.core.node.ParagraphElement
import dev.mkeeda.arranger.core.node.RootElement
import dev.mkeeda.arranger.core.node.TextElement
import dev.mkeeda.arranger.core.node.node
import dev.mkeeda.arranger.core.node.nodeWithChildrenOf
import org.junit.Test

class EditorStateTest {
    @Test
    fun `Type characters consecutively`() {
        val state = EditorState()

        state.setText(
            currentValue = TextFieldValue(
                text = "h",
                selection = TextRange(1)
            ),
            editorStyle = null
        )
        assertThat(state.richText).isEqualTo(
            buildAnnotatedString {
                append("h")
            }
        )
        /**
         * Root
         *   Paragraph
         *     Text("h")
         */
        assertThat(state.rootNode).isEqualTo(
            RootElement().nodeWithChildrenOf(
                ParagraphElement().nodeWithChildrenOf(
                    TextElement(text = "h", editorStyle = null).node()
                )
            )
        )

        state.setText(
            currentValue = TextFieldValue(
                text = "he",
                selection = TextRange(2)
            ),
            editorStyle = ColorStyle(Color.Red)
        )
        assertThat(state.richText).isEqualTo(
            buildAnnotatedString {
                append("h")
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append("e")
                }
            }
        )
        /**
         * Root
         *   Paragraph
         *     Text("h")
         *     Text("e", red)
         */
        assertThat(state.rootNode).isEqualTo(
            RootElement().nodeWithChildrenOf(
                ParagraphElement().nodeWithChildrenOf(
                    TextElement(text = "h", editorStyle = null).node(),
                    TextElement(text = "e", editorStyle = ColorStyle(Color.Red)).node(),
                )
            )
        )
    }
}
