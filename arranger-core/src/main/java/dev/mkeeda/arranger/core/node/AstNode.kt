package dev.mkeeda.arranger.core.node

internal data class AstNode(
    val element: AstNodeElement,
    val links: AstNodeLinks = AstNodeLinks(),
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
}

internal interface AstNodeElement {
    val name: String
}

internal fun AstNode.appendChildren(vararg children: AstNode) {
    for (child in children) {
        this.appendChild(child)
    }
}
