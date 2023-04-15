package dev.mkeeda.arranger.runtime.node

class TextNode : MarkupTextNode() {
    var text: String = ""

    override fun toSemanticText(): String = text

    override fun equals(other: Any?): Boolean {
        return if (other is TextNode) {
            text == other.text
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    override fun toString(): String {
        return "TextNode (text = $text)"
    }
}

fun TextNode(text: String): TextNode = TextNode().apply {
    this.text = text
}

