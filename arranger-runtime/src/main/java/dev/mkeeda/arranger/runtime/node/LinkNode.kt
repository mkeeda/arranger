package dev.mkeeda.arranger.runtime.node

class LinkNode : MarkupTextNode() {
    var url: String = ""
    var text: String = ""

    override fun toSemanticText(): String = text

    override fun equals(other: Any?): Boolean {
        return if (other is LinkNode) {
            url == other.url && text == other.text
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    override fun toString(): String {
        return "LinkNode (url = $url, text = $text)"
    }
}

fun LinkNode(url: String, text: String): LinkNode = LinkNode().apply {
    this.url = url
    this.text = text
}
