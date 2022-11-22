package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.AbstractApplier

abstract class DocumentNode {
    val children = mutableListOf<DocumentNode>()

    abstract fun render(): String
}

class DocumentNodeApplier(root: DocumentNode) : AbstractApplier<DocumentNode>(root) {
    override fun insertBottomUp(index: Int, instance: DocumentNode) {
        current.children.add(index, instance)
    }

    override fun insertTopDown(index: Int, instance: DocumentNode) {
        // Ignored as the tree is built bottom-up.
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

    override fun onClear() {
        current.children.clear()
    }
}

class TextNode : DocumentNode() {
    var text: String = ""
    override fun render(): String = text
}

class HeadingNode : DocumentNode() {
    var level: HeadingLevel = HeadingLevel.H1
    var title: String = ""
    override fun render(): String = title
}

class GroupNode : DocumentNode() {
    override fun render(): String {
        return children.joinToString(separator = "\n") { it.render() }
    }
}
