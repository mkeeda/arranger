package dev.mkeeda.arranger.sample.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

interface DocumentScope

@Composable
fun DocumentScope.Text(text: String) {
    ComposeNode<TextNode, DocumentNodeApplier>(factory = ::TextNode) {
        set(text) {
            this.text = it
        }
    }
}


enum class HeadingLevel {
    H1, H2, H3;
}

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
