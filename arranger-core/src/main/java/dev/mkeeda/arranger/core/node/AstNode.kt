package dev.mkeeda.arranger.core.node

internal data class AstNode(
    val links: AstNodeLinks,
    val element: AstNodeElement
)

internal data class AstNodeLinks(
    var parent: AstNode? = null,
    var prevSibling: AstNode? = null,
    var nextSibling: AstNode? = null,
    var firstChild: AstNode? = null,
    var lastChild: AstNode? = null
) {
    /**
     * If all pointers are checked, happens infinite loop.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is AstNodeLinks) {
            return false
        }
        return nextSibling == other.nextSibling && firstChild == other.firstChild
    }

    override fun hashCode(): Int {
        var result = nextSibling?.hashCode() ?: 0
        result = 31 * result + (firstChild?.hashCode() ?: 0)
        return result
    }
}

internal interface AstNodeElement {
    val name: String
}
