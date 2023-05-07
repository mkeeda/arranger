package dev.mkeeda.arranger.core

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import dev.mkeeda.arranger.core.node.AstNode
import dev.mkeeda.arranger.core.node.RootElement

@Stable
class EditorState internal constructor(
    internal val rootNode: AstNode = AstNode(element = RootElement())
) {
    val richText: AnnotatedString
        get() = AnnotatedString("")

    fun setText(value: TextFieldValue) {
    }
}
