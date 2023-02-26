package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.AbstractApplier
import dev.mkeeda.arranger.runtime.node.DocumentNode

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
