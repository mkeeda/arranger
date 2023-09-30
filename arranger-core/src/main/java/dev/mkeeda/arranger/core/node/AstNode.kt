package dev.mkeeda.arranger.core.node

import java.util.UUID

internal data class AstNode(
    val key: AstNodeKey = AstNodeKey.generate(),
    val element: AstNodeElement,
    val links: AstNodeLinks = AstNodeLinks(),
) {
    init {
        links.nodeMapStore.nodeMap[key] = this
    }

    fun appendChild(newNode: AstNode) {
        newNode.links.parentKey = this.key

        if (links.hasChildren) {
            val prevLastChild = links.getLastChildNode()
            prevLastChild.links.nextSiblingKey = newNode.key
            newNode.links.prevSiblingKey = prevLastChild.key
            links.lastChildKey = newNode.key
        } else {
            links.firstChildKey = newNode.key
            links.lastChildKey = newNode.key
        }

        links.nodeMapStore.nodeMap[newNode.key] = newNode
    }

    override fun toString(): String {
        return element.name
    }
}

internal data class AstNodeLinks(
    var parentKey: AstNodeKey? = null,
    var prevSiblingKey: AstNodeKey? = null,
    var nextSiblingKey: AstNodeKey? = null,
    var firstChildKey: AstNodeKey? = null,
    var lastChildKey: AstNodeKey? = null,
    val nodeMapStore: AstNodeMapStore = GlobalAstNodeMapStore
) {
    val hasChildren: Boolean
        get() = firstChildKey != null && lastChildKey != null

    fun getParentNode(): AstNode {
        return checkNotNull(nodeMapStore.nodeMap[parentKey])
    }

    fun getPrevSiblingNode(): AstNode {
        return checkNotNull(nodeMapStore.nodeMap[prevSiblingKey])
    }

    fun getNextSiblingNode(): AstNode {
        return checkNotNull(nodeMapStore.nodeMap[nextSiblingKey])
    }

    fun getFirstChildNode(): AstNode {
        return checkNotNull(nodeMapStore.nodeMap[firstChildKey])
    }

    fun getLastChildNode(): AstNode {
        return checkNotNull(nodeMapStore.nodeMap[lastChildKey])
    }
}

internal interface AstNodeElement {
    val name: String
}

@JvmInline
internal value class AstNodeKey(val uuid: String) {
    companion object {
        fun generate(): AstNodeKey = AstNodeKey(uuid = UUID.randomUUID().toString())
    }
}

internal fun AstNode.appendChildren(vararg children: AstNode) {
    for (child in children) {
        this.appendChild(child)
    }
}

internal interface AstNodeMapStore {
    val nodeMap: MutableMap<AstNodeKey, AstNode>
}

internal object GlobalAstNodeMapStore : AstNodeMapStore {
    override val nodeMap = mutableMapOf<AstNodeKey, AstNode>()
}
