package dev.mkeeda.arranger.richtext.editor

import dev.mkeeda.arranger.richtext.BulletListItem
import dev.mkeeda.arranger.richtext.ListIndentLevel
import dev.mkeeda.arranger.richtext.OrderedListItem
import dev.mkeeda.arranger.richtext.RgbaColor
import io.kotest.matchers.shouldBe
import org.junit.Test

class ListMarkerResolverTest {
    @Test
    fun `DefaultListMarkerResolver returns correct marker for BulletListItem`() {
        val level1Item = BulletListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified)
        val level2Item = BulletListItem(textIndex = 0, indentLevel = ListIndentLevel.Level2, color = RgbaColor.Unspecified)
        val level3Item = BulletListItem(textIndex = 0, indentLevel = ListIndentLevel.Level3, color = RgbaColor.Unspecified)
        val level4Item = BulletListItem(textIndex = 0, indentLevel = ListIndentLevel.Level4, color = RgbaColor.Unspecified)

        DefaultListMarkerResolver.resolve(level1Item) shouldBe "・"
        DefaultListMarkerResolver.resolve(level2Item) shouldBe "○"
        DefaultListMarkerResolver.resolve(level3Item) shouldBe "▪"
        DefaultListMarkerResolver.resolve(level4Item) shouldBe "▪"
    }

    @Test
    fun `DefaultListMarkerResolver returns correct marker for OrderedListItem`() {
        val index1Item = OrderedListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified, index = 1)
        val index5Item = OrderedListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified, index = 5)
        val index100Item = OrderedListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified, index = 100)

        DefaultListMarkerResolver.resolve(index1Item) shouldBe "1."
        DefaultListMarkerResolver.resolve(index5Item) shouldBe "5."
        DefaultListMarkerResolver.resolve(index100Item) shouldBe "100."
    }

    @Test
    fun `Custom ListMarkerResolver returns custom markers`() {
        val customResolver =
            ListMarkerResolver { item ->
                when (item) {
                    is BulletListItem -> "★"
                    is OrderedListItem -> "(${item.index})"
                }
            }

        val bulletItem = BulletListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified)
        val orderedItem = OrderedListItem(textIndex = 0, indentLevel = ListIndentLevel.Level1, color = RgbaColor.Unspecified, index = 2)

        customResolver.resolve(bulletItem) shouldBe "★"
        customResolver.resolve(orderedItem) shouldBe "(2)"
    }
}
