package dev.mkeeda.arranger.core.node

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AstNodeBuilderTest {
    @Test
    fun makeChildrenNode() {
        val parentElement = TestElement("parent")
        val elementA = TestElement("a")
        val elementB = TestElement("b")
        val elementC = TestElement("c")

        val parentNode = parentElement.nodeWithChildrenOf(
            elementA.node(),
            elementB.node(),
            elementC.node(),
        )
        val nodeA = parentNode.links.firstChild
        val nodeB = nodeA?.links?.nextSibling
        val nodeC = nodeB?.links?.nextSibling

        assertThat(parentNode.links).isEqualTo(
            AstNodeLinks(
                parent = null,
                firstChild = nodeA,
                lastChild = nodeC
            )
        )
        assertThat(nodeA?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = null,
                nextSibling = nodeB
            )
        )
        assertThat(nodeB?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = nodeA,
                nextSibling = nodeC
            )
        )
        assertThat(nodeC?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = nodeB,
                nextSibling = null
            )
        )
    }
}
