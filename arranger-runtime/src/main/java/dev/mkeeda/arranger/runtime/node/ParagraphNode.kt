package dev.mkeeda.arranger.runtime.node

class ParagraphNode : DocumentNode() {
    override fun toSemanticText(): String =
        children.joinToString(separator = "") { it.toSemanticText() }
}

class TextNode : DocumentNode() {
    var text: String = ""

    override fun toSemanticText(): String = text
}

class LinkNode : DocumentNode() {
    var url: String = ""
    var text: String = ""

    override fun toSemanticText(): String = text
}
