package dev.mkeeda.arranger.runtime.node

abstract class MarkupTextNode {
    val children: List<MarkupTextNode>
        get() = _children.toList()

    internal val _children: MutableList<MarkupTextNode> = mutableListOf()

    /**
     * @return A string that convey the meaning of this document itself.
     */
    abstract fun toSemanticText(): String

    override fun equals(other: Any?): Boolean {
        return if (other is MarkupTextNode) {
            children == other.children
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return _children.hashCode()
    }

    override fun toString(): String {
        return "MarkupTextNode ($_children)"
    }
}

class RootNode : MarkupTextNode() {
    override fun toSemanticText(): String =
        children.joinToString(separator = "\n") { it.toSemanticText() }

    override fun toString(): String {
        return "RootNode ($_children)"
    }
}

fun RootNode(children: List<MarkupTextNode>): RootNode = RootNode().apply {
    _children.addAll(children)
}
