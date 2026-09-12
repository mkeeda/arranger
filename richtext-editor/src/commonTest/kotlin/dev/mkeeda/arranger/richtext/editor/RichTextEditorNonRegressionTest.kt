package dev.mkeeda.arranger.richtext.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.BulletListKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.OrderedListKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.editor.wysiwyg.createWysiwygHarness
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RichTextEditorNonRegressionTest {
    @Test
    fun `RichTextEditor composable contract compiles and binds properly`() {
        val composableRef: @Composable () -> Unit = {
            val state = RichTextState(initialText = RichString(text = "Hello"))
            RichTextEditor(
                state = state,
                readOnly = false,
                textStyle = TextStyle.Default,
                styleResolver = DefaultAttributeStyleResolver,
            )
            RichTextEditor(
                state = state,
                readOnly = false,
                textStyle = TextStyle.Default,
                attributeStyleResolver = DefaultAttributeStyleResolver,
                onLinkClick = { _ -> },
            )
        }
        composableRef shouldBe composableRef
    }

    @Test
    fun `RichTextInputTransformation does not trigger block heading 1 auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "# Heading")

        state.richString.text shouldBe "# Heading"
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger block heading 2 auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "## Heading 2")

        state.richString.text shouldBe "## Heading 2"
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger block heading 3 auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "### Heading 3")

        state.richString.text shouldBe "### Heading 3"
        state.richString.spans.filter { it.attributes.containsKey(HeadingKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger bullet list dash auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "- Item")

        state.richString.text shouldBe "- Item"
        state.richString.spans.filter { it.attributes.containsKey(BulletListKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger bullet list asterisk auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "* Item")

        state.richString.text shouldBe "* Item"
        state.richString.spans.filter { it.attributes.containsKey(BulletListKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger ordered list auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "1. Item")

        state.richString.text shouldBe "1. Item"
        state.richString.spans.filter { it.attributes.containsKey(OrderedListKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger blockquote auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "> Quote")

        state.richString.text shouldBe "> Quote"
        state.richString.spans.filter { it.attributes.containsKey(BlockquoteKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger bold auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "**bold text**")

        state.richString.text shouldBe "**bold text**"
        state.richString.spans.filter { it.attributes.containsKey(BoldKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger italic asterisk auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "*italic text*")

        state.richString.text shouldBe "*italic text*"
        state.richString.spans.filter { it.attributes.containsKey(ItalicKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger italic underscore auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "_italic text_")

        state.richString.text shouldBe "_italic text_"
        state.richString.spans.filter { it.attributes.containsKey(ItalicKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger inline code auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "`code text`")

        state.richString.text shouldBe "`code text`"
        state.richString.spans.filter { it.attributes.containsKey(InlineCodeKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextInputTransformation does not trigger strikethrough auto-formatting`() {
        val state = RichTextState()
        val transformation = RichTextInputTransformation(state)

        typeTextSimulated(state, transformation, "~strike text~")

        state.richString.text shouldBe "~strike text~"
        state.richString.spans.filter { it.attributes.containsKey(StrikethroughKey) }.shouldBeEmpty()
    }

    @Test
    fun `RichTextEditor harness preserves raw markdown text without auto-formatting across multiple lines`() {
        val harness = createWysiwygHarness(isWysiwygEnabled = false)
        harness.typeText("# Line 1\n## Line 2\n- Bullet\n`Code`\n**Bold**")

        harness.assertText("# Line 1\n## Line 2\n- Bullet\n`Code`\n**Bold**")
        harness.assertNotHeading()
        harness.assertNotBulletList()
        harness.assertNotInlineCode()
        harness.assertNotBold()
    }

    private fun typeTextSimulated(
        state: RichTextState,
        transformation: RichTextInputTransformation,
        text: String,
    ) {
        for (char in text) {
            val oldLength = state.textFieldState.text.length
            val insertIndex = state.selection.start
            state.textFieldState.edit {
                replace(insertIndex, insertIndex, char.toString())
                selection = TextRange(insertIndex + 1)
                val buffer = this
                with(transformation) {
                    buffer.transformInput()
                }
            }
        }
    }
}
