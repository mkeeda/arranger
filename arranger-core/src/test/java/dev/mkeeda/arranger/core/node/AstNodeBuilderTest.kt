package dev.mkeeda.arranger.core.node

import com.google.common.truth.Truth
import org.junit.Test

class AstNodeBuilderTest {
    @Test
    fun makeChildrenNode() {
        val parentElement = TestElement("parent")
        val elementA = TestElement("a")
        val elementB = TestElement("b")
        val elementC = TestElement("c")

        val parentNode = parentElement.toNodeWithChildrenOf(elementA, elementB, elementC)
        val nodeA = parentNode.links.firstChild
        val nodeB = nodeA?.links?.nextSibling
        val nodeC = nodeB?.links?.nextSibling

        Truth.assertThat(parentNode.links).isEqualTo(
            AstNodeLinks(
                parent = null,
                firstChild = nodeA,
                lastChild = nodeC
            )
        )
        Truth.assertThat(nodeA?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = null,
                nextSibling = nodeB
            )
        )
        Truth.assertThat(nodeB?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = nodeA,
                nextSibling = nodeC
            )
        )
        Truth.assertThat(nodeC?.links).isEqualTo(
            AstNodeLinks(
                parent = parentNode,
                prevSibling = nodeB,
                nextSibling = null
            )
        )
    }
}
