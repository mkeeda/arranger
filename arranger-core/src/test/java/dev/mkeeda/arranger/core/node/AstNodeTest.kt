package dev.mkeeda.arranger.core.node

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class AstNodeTest {
    private lateinit var testNodeMapStore: TestAstNodeMapStore

    @Before
    fun setup() {
        testNodeMapStore = TestAstNodeMapStore()
    }

    private fun testAstNodeLinks() = AstNodeLinks(nodeMapStore = testNodeMapStore)

    @Test
    fun appendChild() {
        val parentKey = AstNodeKey(uuid = "parent")
        val aKey = AstNodeKey(uuid = "a")
        val bKey = AstNodeKey(uuid = "b")
        val cKey = AstNodeKey(uuid = "c")

        val parent = AstNode(
            key = parentKey,
            element = TestElement("parent"),
            links = testAstNodeLinks(),
        )
        val nodeA = AstNode(
            key = aKey,
            element = TestElement("a"),
            links = testAstNodeLinks(),
        )
        val nodeB = AstNode(
            key = bKey,
            element = TestElement("b"),
            links = testAstNodeLinks(),
        )
        val nodeC = AstNode(
            key = cKey,
            element = TestElement("c"),
            links = testAstNodeLinks(),
        )

        parent.appendChild(nodeA)
        parent.appendChild(nodeB)
        parent.appendChild(nodeC)

        assertThat(parent.links).isEqualTo(
            AstNodeLinks(
                parentKey = null,
                firstChildKey = nodeA.key,
                lastChildKey = nodeC.key,
                nodeMapStore = testNodeMapStore,
            )
        )
        assertThat(nodeA.links).isEqualTo(
            AstNodeLinks(
                parentKey = parent.key,
                prevSiblingKey = null,
                nextSiblingKey = nodeB.key,
                nodeMapStore = testNodeMapStore,
            )
        )
        assertThat(nodeB.links).isEqualTo(
            AstNodeLinks(
                parentKey = parent.key,
                prevSiblingKey = nodeA.key,
                nextSiblingKey = nodeC.key,
                nodeMapStore = testNodeMapStore,
            )
        )
        assertThat(nodeC.links).isEqualTo(
            AstNodeLinks(
                parentKey = parent.key,
                prevSiblingKey = nodeB.key,
                nextSiblingKey = null,
                nodeMapStore = testNodeMapStore,
            )
        )

        assertThat(testNodeMapStore.nodeMap).isEqualTo(
            mapOf(
                parentKey to parent,
                aKey to nodeA,
                bKey to nodeB,
                cKey to nodeC
            )
        )
    }
}

private class TestAstNodeMapStore(
    override val nodeMap: MutableMap<AstNodeKey, AstNode> = mutableMapOf()
) : AstNodeMapStore
