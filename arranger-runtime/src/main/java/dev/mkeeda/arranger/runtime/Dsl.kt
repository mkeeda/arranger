package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import dev.mkeeda.arranger.runtime.node.HeadingLevel
import dev.mkeeda.arranger.runtime.node.HeadingNode
import dev.mkeeda.arranger.runtime.node.LinkNode
import dev.mkeeda.arranger.runtime.node.MarkupTextNode
import dev.mkeeda.arranger.runtime.node.ParagraphNode
import dev.mkeeda.arranger.runtime.node.TextNode

@DslMarker
annotation class MarkupTextScopeMarker

@MarkupTextScopeMarker
class MarkupTextScope {
    @Composable
    fun Heading(level: HeadingLevel, title: String) {
        ComposeNode<HeadingNode, MarkupTextNodeApplier>(factory = ::HeadingNode) {
            set(level) {
                this.level = it
            }
            set(title) {
                this.title = it
            }
        }
    }

    @Composable
    fun Paragraph(content: @Composable () -> Unit) {
        ComposeNode<ParagraphNode, MarkupTextNodeApplier>(
            factory = ::ParagraphNode,
            update = {},
            content = content
        )
    }

    @Composable
    fun Text(text: String) {
        ComposeNode<TextNode, MarkupTextNodeApplier>(factory = ::TextNode) {
            set(text) {
                this.text = it
            }
        }
    }

    @Composable
    fun Link(text: String, url: String) {
        ComposeNode<LinkNode, MarkupTextNodeApplier>(factory = ::LinkNode) {
            set(url) {
                this.url = url
            }
            set(text) {
                this.text = it
            }
        }
    }
}

internal fun MarkupTextNode.setContent(
    parent: CompositionContext,
    content: @Composable () -> Unit
): Composition {
    return Composition(MarkupTextNodeApplier(this), parent).apply {
        setContent(content)
    }
}

