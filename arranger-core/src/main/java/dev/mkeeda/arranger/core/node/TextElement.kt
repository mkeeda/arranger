package dev.mkeeda.arranger.core.node

import dev.mkeeda.arranger.core.EditorStyle

internal data class TextElement(
    val text: String,
    val editorStyle: EditorStyle?
) : AstNodeElement {
    override val name: String = "Text"
}
