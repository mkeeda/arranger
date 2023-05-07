package dev.mkeeda.arranger.core.node

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AstNodeTest {
    @Test
    fun appendChild() {
        val parent = AstNode(
            links = AstNodeLinks(),
            element = TestElement("parent")
        )
        val nodeA = AstNode(
            links = AstNodeLinks(),
            element = TestElement("a")
        )
        val nodeB = AstNode(
            links = AstNodeLinks(),
            element = TestElement("b")
        )
        val nodeC = AstNode(
            links = AstNodeLinks(),
            element = TestElement("c")
        )

        parent.appendChild(nodeA)
        parent.appendChild(nodeB)
        parent.appendChild(nodeC)

        assertThat(parent.links).isEqualTo(
            AstNodeLinks(
                parent = null,
                firstChild = nodeA,
                lastChild = nodeC
            )
        )
        assertThat(nodeA.links).isEqualTo(
            AstNodeLinks(
                parent = parent,
                prevSibling = null,
                nextSibling = nodeB
            )
        )
        assertThat(nodeB.links).isEqualTo(
            AstNodeLinks(
                parent = parent,
                prevSibling = nodeA,
                nextSibling = nodeC
            )
        )
        assertThat(nodeC.links).isEqualTo(
            AstNodeLinks(
                parent = parent,
                prevSibling = nodeB,
                nextSibling = null
            )
        )
    }
}

private data class TestElement(override val name: String) : AstNodeElement
