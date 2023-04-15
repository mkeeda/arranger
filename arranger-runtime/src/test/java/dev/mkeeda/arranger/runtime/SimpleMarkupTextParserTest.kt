package dev.mkeeda.arranger.runtime

import com.google.common.truth.Truth.assertThat
import dev.mkeeda.arranger.runtime.node.HeadingLevel
import dev.mkeeda.arranger.runtime.node.HeadingNode
import dev.mkeeda.arranger.runtime.node.LinkNode
import dev.mkeeda.arranger.runtime.node.MarkupTextNode
import dev.mkeeda.arranger.runtime.node.ParagraphNode
import dev.mkeeda.arranger.runtime.node.RootNode
import dev.mkeeda.arranger.runtime.node.TextNode
import org.junit.Test

class SimpleMarkupTextParserTest {
    private fun doTest(markupText: String, expectedNode: MarkupTextNode) {
        val parser = SimpleMarkupTextParser()
        val actualNode = parser.parse(markupText)
        assertThat(actualNode).isEqualTo(expectedNode)
    }

    @Test
    fun oneText() {
        doTest(
            markupText = "This is a pen.",
            expectedNode = RootNode(
                listOf(
                    ParagraphNode(
                        listOf(
                            TextNode("This is a pen."),
                        )
                    ),
                )
            )
        )
    }


    @Test
    fun twoParagraph() {
        doTest(
            markupText = """
                This is a pen.
                That is a cup.
            """.trimIndent(),
            expectedNode = RootNode(
                listOf(
                    ParagraphNode(
                        listOf(
                            TextNode("This is a pen."),
                        )
                    ),
                    ParagraphNode(
                        listOf(
                            TextNode("That is a cup."),
                        )
                    ),
                )
            )
        )
    }


    @Test
    fun link() {
        doTest(
            markupText = """
                before text [link](https://example.com) after text
            """.trimIndent(),
            expectedNode = RootNode(
                listOf(
                    ParagraphNode(
                        listOf(
                            TextNode("before text "),
                            LinkNode(url = "https://example.com", text = "link"),
                            TextNode(" after text"),
                        )
                    ),
                )
            )
        )
    }

    @Test
    fun heading() {
        doTest(
            markupText = """
                # h1
                ## h2
                ### h3
            """.trimIndent(),
            expectedNode = RootNode(
                listOf(
                    HeadingNode(title = "h1", level = HeadingLevel.H1),
                    HeadingNode(title = "h2", level = HeadingLevel.H2),
                    HeadingNode(title = "h3", level = HeadingLevel.H3),
                )
            )
        )
    }

    @Test
    fun mixSyntax() {
        doTest(
            markupText = """
                # My profile
                Hello world. I am a arranger. My profile is this [link](https://example.com).
            """.trimIndent(),
            expectedNode = RootNode(
                listOf(
                    HeadingNode(title = "My profile", level = HeadingLevel.H1),
                    ParagraphNode(
                        listOf(
                            TextNode("Hello world. I am a arranger. My profile is this "),
                            LinkNode(url = "https://example.com", text = "link"),
                            TextNode("."),
                        )
                    )
                )
            )
        )
    }
}
