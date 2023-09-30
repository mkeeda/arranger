package dev.mkeeda.arranger.core.node

internal fun AstNodeElement.node(): AstNode {
    return AstNode(element = this)
}

internal fun AstNodeElement.nodeWithChildrenOf(vararg children: AstNode): AstNode {
    val parentNode = AstNode(element = this)
    for (child in children) {
        parentNode.appendChild(child)
    }
    return parentNode
}
