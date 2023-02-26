package dev.mkeeda.arranger.runtime.node

abstract class DocumentNode {
    val children : MutableList<DocumentNode> = mutableListOf()

    /**
     * @return A string that convey the meaning of this document itself.
     */
    abstract fun toSemanticText(): String
}

class RootNode : DocumentNode() {
    override fun toSemanticText(): String =
        children.joinToString(separator = "\n") { it.toSemanticText() }
}
