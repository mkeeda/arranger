package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.mkeeda.arranger.runtime.node.HeadingLevel
import dev.mkeeda.arranger.runtime.node.HeadingNode
import dev.mkeeda.arranger.runtime.node.LinkNode
import dev.mkeeda.arranger.runtime.node.ParagraphNode
import dev.mkeeda.arranger.runtime.node.TextNode

interface DocumentScope

@Composable
fun DocumentScope.Heading(level: HeadingLevel, title: String) {
    ComposeNode<HeadingNode, DocumentNodeApplier>(factory = ::HeadingNode) {
        set(level) {
            this.level = it
        }
        set(title) {
            this.title = it
        }
    }
}

@Composable
fun DocumentScope.Paragraph(content: @Composable () -> Unit) {
    ComposeNode<ParagraphNode, DocumentNodeApplier>(
        factory = ::ParagraphNode,
        update = {},
        content = content
    )
}

@Composable
fun DocumentScope.Text(text: String) {
    ComposeNode<TextNode, DocumentNodeApplier>(factory = ::TextNode) {
        set(text) {
            this.text = it
        }
    }
}

@Composable
fun DocumentScope.Link(text: String, url: String) {
    ComposeNode<LinkNode, DocumentNodeApplier>(factory = ::LinkNode) {
        set(url) {
            this.url = url
        }
        set(text) {
            this.text = it
        }
    }
}
