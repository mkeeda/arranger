package dev.mkeeda.arranger.richtext

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

/**
 * Adversarial stress test suite for [SpanMerger] (transformSpans, mergeSpan),
 * [RichStringScope], and [resnapParagraphSpans].
 *
 * Focuses on:
 * - Overlapping, adjacent, and deeply nested spans within list items
 * - Multiple inline decorations (Bold, Italic, Code, Strikethrough, Url/TextColor)
 * - Boundary edge cases (empty text, single char, adjacent touches, complete overlaps)
 * - Fuzzing / property-based interval chunking stress test
 */
class SpanMergerAdversarialStressTest {
    @Test
    fun `list item containing multiple overlapping adjacent and nested inline spans preserves all attributes`() {
        // Text: 0..49 (50 chars)
        // Bullet list paragraph: 0..49
        // Bold: 5..15
        // Italic: 10..20 (partially overlaps Bold at 10..15)
        // Code: 21..28 (adjacent to Italic at 20 + 1 == 21)
        // Strikethrough: 2..35 (encompasses Bold, Italic, Code)
        // TextColor: 36..45
        val text = "01234567890123456789012345678901234567890123456789"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..49)
                setSpanAttribute(StrikethroughKey, Unit, 2..35)
                setSpanAttribute(BoldKey, Unit, 5..15)
                setSpanAttribute(ItalicKey, Unit, 10..20)
                setSpanAttribute(InlineCodeKey, Unit, 21..28)
                setSpanAttribute(TextColorKey, RgbaColor(0xFF00FF00), 36..45)
            }

        // 1. Verify runs for each attribute reconstruct the original ranges
        val bulletRuns = richString.runs(BulletListKey).toList()
        bulletRuns shouldHaveSize 1
        bulletRuns[0].range shouldBe 0..49
        bulletRuns[0].value shouldBe ListIndentLevel.Level1

        val strikeRuns = richString.runs(StrikethroughKey).toList()
        strikeRuns shouldHaveSize 1
        strikeRuns[0].range shouldBe 2..35

        val boldRuns = richString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 5..15

        val italicRuns = richString.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 10..20

        val codeRuns = richString.runs(InlineCodeKey).toList()
        codeRuns shouldHaveSize 1
        codeRuns[0].range shouldBe 21..28

        val colorRuns = richString.runs(TextColorKey).toList()
        colorRuns shouldHaveSize 1
        colorRuns[0].range shouldBe 36..45

        // 2. Verify intervals are disjoint, tessellated, and contain expected combined attributes
        // Expected chunks:
        // 0..1: BulletList
        // 2..4: BulletList, Strikethrough
        // 5..9: BulletList, Strikethrough, Bold
        // 10..15: BulletList, Strikethrough, Bold, Italic
        // 16..20: BulletList, Strikethrough, Italic
        // 21..28: BulletList, Strikethrough, Code
        // 29..35: BulletList, Strikethrough
        // 36..45: BulletList, TextColor
        // 46..50: BulletList
        val spans = richString.spans
        spans shouldHaveSize 9

        spans[0].range shouldBe 0..1
        spans[0].attributes.keys shouldBe setOf(BulletListKey)

        spans[1].range shouldBe 2..4
        spans[1].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey)

        spans[2].range shouldBe 5..9
        spans[2].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey, BoldKey)

        spans[3].range shouldBe 10..15
        spans[3].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey, BoldKey, ItalicKey)

        spans[4].range shouldBe 16..20
        spans[4].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey, ItalicKey)

        spans[5].range shouldBe 21..28
        spans[5].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey, InlineCodeKey)

        spans[6].range shouldBe 29..35
        spans[6].attributes.keys shouldBe setOf(BulletListKey, StrikethroughKey)

        spans[7].range shouldBe 36..45
        spans[7].attributes.keys shouldBe setOf(BulletListKey, TextColorKey)

        spans[8].range shouldBe 46..49
        spans[8].attributes.keys shouldBe setOf(BulletListKey)
    }

    @Test
    fun `removing an attribute from an overlapping chunk splits and coalesces neighboring chunks`() {
        val text = "01234567890123456789"
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..19)
                setSpanAttribute(BoldKey, Unit, 5..15)
                setSpanAttribute(ItalicKey, Unit, 10..15)
            }

        // Remove Bold from 8..12
        val modified =
            richString.edit {
                removeSpanAttribute(BoldKey, 8..12)
            }

        // Original:
        // 0..4: Bullet
        // 5..9: Bullet + Bold
        // 10..15: Bullet + Bold + Italic
        // 16..20: Bullet
        // After removing Bold from 8..12:
        // 0..4: Bullet
        // 5..7: Bullet + Bold
        // 8..9: Bullet
        // 10..12: Bullet + Italic
        // 13..15: Bullet + Bold + Italic
        // 16..20: Bullet
        val spans = modified.spans
        spans shouldHaveSize 6

        spans[0].range shouldBe 0..4
        spans[0].attributes.keys shouldBe setOf(BulletListKey)

        spans[1].range shouldBe 5..7
        spans[1].attributes.keys shouldBe setOf(BulletListKey, BoldKey)

        spans[2].range shouldBe 8..9
        spans[2].attributes.keys shouldBe setOf(BulletListKey)

        spans[3].range shouldBe 10..12
        spans[3].attributes.keys shouldBe setOf(BulletListKey, ItalicKey)

        spans[4].range shouldBe 13..15
        spans[4].attributes.keys shouldBe setOf(BulletListKey, BoldKey, ItalicKey)

        spans[5].range shouldBe 16..19
        spans[5].attributes.keys shouldBe setOf(BulletListKey)

        // Bold runs should now be two separate runs: 5..7 and 13..15
        val boldRuns = modified.runs(BoldKey).toList()
        boldRuns shouldHaveSize 2
        boldRuns[0].range shouldBe 5..7
        boldRuns[1].range shouldBe 13..15

        // Italic run should be intact: 10..15
        val italicRuns = modified.runs(ItalicKey).toList()
        italicRuns shouldHaveSize 1
        italicRuns[0].range shouldBe 10..15
    }

    @Test
    fun `resnapParagraphSpans on multi-line text with inline formatting across boundaries`() {
        val text = "Line 1 with bold\nLine 2 with code\nLine 3 plain"
        // Paragraph 1: 0..16 ("Line 1 with bold\n")
        // Paragraph 2: 17..33 ("Line 2 with code\n")
        // Paragraph 3: 34..45 ("Line 3 plain")
        val richString =
            RichString(text).edit {
                setParagraphAttribute(BulletListKey, ListIndentLevel.Level1, 0..16)
                setParagraphAttribute(OrderedListKey, ListIndentLevel.Level1, 17..33)
                setSpanAttribute(BoldKey, Unit, 12..15) // "bold"
                setSpanAttribute(InlineCodeKey, Unit, 29..32) // "code"
            }

        // Test resnapParagraphSpans preserves all inline and paragraph spans
        val resnapped = richString.spans.resnapParagraphSpans(text)
        val resnappedRichString = RichString(text, resnapped)

        val bulletRuns = resnappedRichString.runs(BulletListKey).toList()
        bulletRuns shouldHaveSize 1
        bulletRuns[0].range shouldBe 0..16

        val orderedRuns = resnappedRichString.runs(OrderedListKey).toList()
        orderedRuns shouldHaveSize 1
        orderedRuns[0].range shouldBe 17..33

        val boldRuns = resnappedRichString.runs(BoldKey).toList()
        boldRuns shouldHaveSize 1
        boldRuns[0].range shouldBe 12..15

        val codeRuns = resnappedRichString.runs(InlineCodeKey).toList()
        codeRuns shouldHaveSize 1
        codeRuns[0].range shouldBe 29..32
    }

    @Test
    fun `adjacent spans with same attributes merge automatically`() {
        val spans =
            listOf(
                RichSpan(0..4, attributeContainerOf(BoldKey to Unit)),
                RichSpan(5..9, attributeContainerOf(BoldKey to Unit)),
            )

        val merged = spans.mergeSpan(RichSpan(10..14, attributeContainerOf(BoldKey to Unit)))
        merged shouldHaveSize 1
        merged[0].range shouldBe 0..14
        merged[0].attributes.keys shouldBe setOf(BoldKey)
    }

    @Test
    fun `adjacent spans with different attributes do not merge`() {
        val spans =
            listOf(
                RichSpan(0..4, attributeContainerOf(BoldKey to Unit)),
                RichSpan(5..9, attributeContainerOf(ItalicKey to Unit)),
            )

        val merged = spans.mergeSpan(RichSpan(10..14, attributeContainerOf(InlineCodeKey to Unit)))
        merged shouldHaveSize 3
        merged[0].range shouldBe 0..4
        merged[1].range shouldBe 5..9
        merged[2].range shouldBe 10..14
    }

    @Test
    fun `completely identical duplicate span application is idempotent`() {
        val initial = listOf(RichSpan(0..10, attributeContainerOf(BoldKey to Unit)))
        val mergedOnce = initial.mergeSpan(RichSpan(0..10, attributeContainerOf(BoldKey to Unit)))
        val mergedTwice = mergedOnce.mergeSpan(RichSpan(0..10, attributeContainerOf(BoldKey to Unit)))

        mergedOnce shouldBe initial
        mergedTwice shouldBe initial
    }

    @Test
    fun `fuzz testing interval chunking with random overlapping ranges`() {
        // Generate a random sequence of intervals and attribute applications
        // Verify that:
        // 1. Resulting spans are strictly non-overlapping and sorted
        // 2. For every character position, the set of attributes matches an independent naive oracle
        val length = 100
        val rng = Random(42)
        val numOperations = 200

        data class Op(val key: SpanAttributeKey<Unit>, val range: IntRange)
        val keys = listOf<SpanAttributeKey<Unit>>(BoldKey, ItalicKey, InlineCodeKey, StrikethroughKey)
        val ops = mutableListOf<Op>()

        repeat(numOperations) {
            val key = keys[rng.nextInt(keys.size)]
            val a = rng.nextInt(length)
            val b = rng.nextInt(length)
            val range = minOf(a, b)..maxOf(a, b)
            ops.add(Op(key, range))
        }

        // 1. Naive character-by-character oracle
        val oracle = Array(length) { mutableSetOf<SpanAttributeKey<Unit>>() }
        for (op in ops) {
            for (idx in op.range) {
                oracle[idx].add(op.key)
            }
        }

        // 2. TransformSpans / mergeSpan execution
        var currentSpans = emptyList<RichSpan>()
        for (op in ops) {
            currentSpans =
                currentSpans.mergeSpan(
                    RichSpan(
                        range = op.range,
                        attributes = attributeContainerOf(op.key to Unit),
                    ),
                )
        }

        // 3. Invariants check:
        // - strictly non-overlapping and strictly increasing
        for (i in 0 until currentSpans.size - 1) {
            val curr = currentSpans[i]
            val next = currentSpans[i + 1]
            (curr.range.last < next.range.first) shouldBe true
            (curr.range.first <= curr.range.last) shouldBe true
            // Adjacent chunks should never have identical attributes (coalescing invariant)
            if (curr.range.last + 1 == next.range.first) {
                (curr.attributes == next.attributes) shouldBe false
            }
        }

        // 4. Compare with oracle at every index
        for (i in 0 until length) {
            val spanAtI = currentSpans.firstOrNull { i in it.range }
            val expectedAttrs = oracle[i]
            val actualAttrs = spanAtI?.attributes?.keys ?: emptySet()
            actualAttrs shouldBe expectedAttrs
        }
    }
}
