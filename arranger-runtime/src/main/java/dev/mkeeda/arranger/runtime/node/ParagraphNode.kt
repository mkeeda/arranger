package dev.mkeeda.arranger.runtime.node

class ParagraphNode : MarkupTextNode() {
    override fun toSemanticText(): String =
        children.joinToString(separator = "") { it.toSemanticText() }

    override fun toString(): String {
        return "ParagraphNode($_children)"
    }
}

fun ParagraphNode(children: List<MarkupTextNode>): ParagraphNode = ParagraphNode().apply {
    _children.addAll(children)
}
