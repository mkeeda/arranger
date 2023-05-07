package dev.mkeeda.arranger.core.node

internal data class AstNode(
    val links: AstNodeLinks,
    val element: AstNodeElement
) {
    fun appendChild(newNode: AstNode) {
        newNode.links.parent = this

        if (links.hasChildren) {
            val prevLastChild = links.lastChild!!
            prevLastChild.links.nextSibling = newNode
            newNode.links.prevSibling = prevLastChild
            links.lastChild = newNode
        } else {
            links.firstChild = newNode
            links.lastChild = newNode
        }
    }

    override fun toString(): String {
        return element.name
    }
}

internal data class AstNodeLinks(
    var parent: AstNode? = null,
    var prevSibling: AstNode? = null,
    var nextSibling: AstNode? = null,
    var firstChild: AstNode? = null,
    var lastChild: AstNode? = null
) {
    val hasChildren: Boolean
        get() = firstChild != null && lastChild != null

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
