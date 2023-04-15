package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.AbstractApplier
import dev.mkeeda.arranger.runtime.node.MarkupTextNode

internal class MarkupTextNodeApplier(root: MarkupTextNode) : AbstractApplier<MarkupTextNode>(root) {
    override fun insertBottomUp(index: Int, instance: MarkupTextNode) {
        current._children.add(index, instance)
    }

    override fun insertTopDown(index: Int, instance: MarkupTextNode) {
        // Ignored as the tree is built bottom-up.
    }

    override fun move(from: Int, to: Int, count: Int) {
        current._children.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        current._children.remove(index, count)
    }

    override fun onClear() {
        current._children.clear()
    }
}
