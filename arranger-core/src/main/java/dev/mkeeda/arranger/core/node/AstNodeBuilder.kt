package dev.mkeeda.arranger.core.node

internal fun AstNodeElement.toNodeWithChildrenOf(vararg childrenElements: AstNodeElement): AstNode {
    val parentNode = AstNode(element = this)
    for (childElement in childrenElements) {
        val childNode = AstNode(element = childElement)
        parentNode.appendChild(childNode)
    }
    return parentNode
}
