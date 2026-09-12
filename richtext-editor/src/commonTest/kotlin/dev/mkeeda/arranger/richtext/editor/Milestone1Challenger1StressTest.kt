package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.mkeeda.arranger.richtext.AttributeContainer
import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.HeadingKey
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.InlineCodeKey
import dev.mkeeda.arranger.richtext.ItalicKey
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.StrikethroughKey
import dev.mkeeda.arranger.richtext.UnderlineKey
import dev.mkeeda.arranger.richtext.attributeContainerOf
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.clearBold
import dev.mkeeda.arranger.richtext.clearInlineCode
import dev.mkeeda.arranger.richtext.clearItalic
import dev.mkeeda.arranger.richtext.clearLink
import dev.mkeeda.arranger.richtext.clearStrikethrough
import dev.mkeeda.arranger.richtext.clearUnderline
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.inlineCode
import dev.mkeeda.arranger.richtext.italic
import dev.mkeeda.arranger.richtext.link
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.richtext.strikethrough
import dev.mkeeda.arranger.richtext.underline
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

/**
 * Adversarial stress and boundary condition test suite for Milestone 1 (Core Inline Code Attribute).
 *
 * Authored by: Challenger 1 (m1_challenger_1)
 */
class Milestone1Challenger1StressTest {
    // =========================================================================
    // 1. Extreme Combinations: InlineCodeKey with All Inline & Paragraph Attributes
    // =========================================================================

    @Test
    fun `InlineCodeKey resolves orthogonally when combined with all standard inline attributes`() {
        val testUrl = "https://arranger.dev"
        val container =
            AttributeContainer.empty() +
                (InlineCodeKey to Unit) +
                (BoldKey to Unit) +
                (ItalicKey to Unit) +
                (StrikethroughKey to Unit) +
                (UnderlineKey to Unit) +
                (LinkKey to testUrl)

        val resolved = DefaultAttributeStyleResolver.resolve(container)

        // Monospace and background from InlineCodeKey
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace
        resolved.spanStyle?.background shouldBe Color(0x14000000)

        // Weight from BoldKey
        resolved.spanStyle?.fontWeight shouldBe FontWeight.Bold

        // Style from ItalicKey
        resolved.spanStyle?.fontStyle shouldBe FontStyle.Italic

        // Link color
        resolved.spanStyle?.color shouldBe Color(0xFF1E88E5)

        // TextDecoration is Underline (from LinkKey / UnderlineKey via Compose SpanStyle.merge)
        resolved.spanStyle?.textDecoration shouldBe TextDecoration.Underline

        // No paragraph style leaked
        resolved.paragraphStyle.shouldBeNull()
    }

    @Test
    fun `RichString applies all 6 inline attributes and clearCode isolates InlineCodeKey removal`() {
        val original = RichString("All inline styles test")
        val styled =
            original.edit {
                editAttributes {
                    bold()
                    italic()
                    strikethrough()
                    underline()
                    link("https://arranger.dev")
                    inlineCode()
                }
            }

        styled.spans shouldHaveSize 1
        val attrs = styled.spans[0].attributes
        attrs[InlineCodeKey] shouldBe Unit
        attrs[BoldKey] shouldBe Unit
        attrs[ItalicKey] shouldBe Unit
        attrs[StrikethroughKey] shouldBe Unit
        attrs[UnderlineKey] shouldBe Unit
        attrs[LinkKey] shouldBe "https://arranger.dev"

        // Remove only inlineCode()
        val afterClearCode =
            styled.edit {
                editAttributes {
                    clearInlineCode()
                }
            }

        afterClearCode.spans shouldHaveSize 1
        val clearedAttrs = afterClearCode.spans[0].attributes
        clearedAttrs[InlineCodeKey].shouldBeNull()
        clearedAttrs[BoldKey] shouldBe Unit
        clearedAttrs[ItalicKey] shouldBe Unit
        clearedAttrs[StrikethroughKey] shouldBe Unit
        clearedAttrs[UnderlineKey] shouldBe Unit
        clearedAttrs[LinkKey] shouldBe "https://arranger.dev"

        // Clear other attributes one by one, ensuring code remains intact when re-added
        val reAddedCode =
            afterClearCode.edit {
                editAttributes { inlineCode() }
            }
        reAddedCode.spans[0].attributes[InlineCodeKey] shouldBe Unit

        val clearedBold = reAddedCode.edit { editAttributes { clearBold() } }
        clearedBold.spans[0].attributes[InlineCodeKey] shouldBe Unit
        clearedBold.spans[0].attributes[BoldKey].shouldBeNull()

        val clearedItalic = clearedBold.edit { editAttributes { clearItalic() } }
        clearedItalic.spans[0].attributes[InlineCodeKey] shouldBe Unit
        clearedItalic.spans[0].attributes[ItalicKey].shouldBeNull()

        val clearedStrikethrough = clearedItalic.edit { editAttributes { clearStrikethrough() } }
        clearedStrikethrough.spans[0].attributes[InlineCodeKey] shouldBe Unit
        clearedStrikethrough.spans[0].attributes[StrikethroughKey].shouldBeNull()

        val clearedUnderline = clearedStrikethrough.edit { editAttributes { clearUnderline() } }
        clearedUnderline.spans[0].attributes[InlineCodeKey] shouldBe Unit
        clearedUnderline.spans[0].attributes[UnderlineKey].shouldBeNull()

        val clearedLink = clearedUnderline.edit { editAttributes { clearLink() } }
        clearedLink.spans[0].attributes[InlineCodeKey] shouldBe Unit
        clearedLink.spans[0].attributes[LinkKey].shouldBeNull()

        // At this point only code remains
        clearedLink.spans[0].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)
    }

    // =========================================================================
    // 2. String Length Extremities: Empty, 1-Char, and Huge (50,000 Chars)
    // =========================================================================

    @Test
    fun `InlineCodeKey on empty string does not crash and yields empty spans`() {
        val emptyRichString = RichString("")
        val edited =
            emptyRichString.edit {
                editAttributes(0 until 0) {
                    inlineCode()
                }
            }
        edited.text shouldBe ""
        edited.spans.shouldBeEmpty()

        // RichTextState on empty string
        val state = RichTextState(initialText = RichString(""))
        state.edit {
            editAttributes(0 until 0) {
                inlineCode()
            }
        }
        state.richString.spans.shouldBeEmpty()
    }

    @Test
    fun `InlineCodeKey on single character behaves correctly across lifecycle`() {
        val oneChar =
            RichString("x").edit {
                editAttributes(0..0) {
                    inlineCode()
                }
            }
        oneChar.spans shouldHaveSize 1
        oneChar.spans[0].range shouldBe 0..0
        oneChar.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // Clear code on 1-char
        val cleared =
            oneChar.edit {
                editAttributes(0..0) {
                    clearInlineCode()
                }
            }
        cleared.spans.shouldBeEmpty()

        // RichTextState deletion of single styled character
        val state = RichTextState(initialText = oneChar)
        state.edit {
            delete(0..0)
        }
        state.richString.text shouldBe ""
        state.richString.spans.shouldBeEmpty()
    }

    @Test
    fun `InlineCodeKey handles huge strings 50000 characters efficiently without overflow`() {
        val size = 50_000
        val hugeText = "a".repeat(size)
        val initial = RichString(hugeText)

        // Apply code to range 10,000..40,000
        val styled =
            initial.edit {
                editAttributes(10_000..40_000) {
                    inlineCode()
                }
            }
        styled.spans shouldHaveSize 1
        styled.spans[0].range shouldBe 10_000..40_000
        styled.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // Verify resolver on this container
        val resolved = DefaultAttributeStyleResolver.resolve(styled.spans[0].attributes)
        resolved.spanStyle?.fontFamily shouldBe FontFamily.Monospace

        // Shift spans via RichTextState
        val state = RichTextState(initialText = styled)

        // Insert 100 characters before the code span
        state.edit {
            insert(5_000, "b".repeat(100))
        }
        state.richString.text.length shouldBe size + 100
        state.richString.spans shouldHaveSize 1
        state.richString.spans[0].range shouldBe 10_100..40_100
        state.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // Delete 500 characters inside the code span
        state.edit {
            delete(20_000..20_499)
        }
        state.richString.text.length shouldBe size + 100 - 500
        state.richString.spans shouldHaveSize 1
        state.richString.spans[0].range shouldBe 10_100..(40_100 - 500)
        state.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit
    }

    // =========================================================================
    // 3. Span Shifting, clearInlineCode(), clearAll(), and Partial Deletions
    // =========================================================================

    @Test
    fun `clearCode in the middle splits a code span into two distinct spans`() {
        val text = "0123456789"
        val styled =
            RichString(text).edit {
                editAttributes(0..9) {
                    inlineCode()
                }
            }
        styled.spans shouldHaveSize 1

        // Clear code from 3..6 -> should leave 0..2 and 7..9
        val split =
            styled.edit {
                editAttributes(3..6) {
                    clearInlineCode()
                }
            }

        split.spans shouldHaveSize 2
        split.spans[0].range shouldBe 0..2
        split.spans[0].attributes[InlineCodeKey] shouldBe Unit

        split.spans[1].range shouldBe 7..9
        split.spans[1].attributes[InlineCodeKey] shouldBe Unit
    }

    @Test
    fun `clearAll removes InlineCodeKey along with paragraph attributes`() {
        val text = "val x = 10\nval y = 20"
        // Paragraph 1 is 0..10 (inclusive of \n).
        // Let's apply code, bold, and heading to the entire paragraph 1 (0..10)
        val styled =
            RichString(text).edit {
                editAttributes(0..10) {
                    inlineCode()
                    bold()
                    headingLevel(HeadingLevel.H1)
                }
            }
        styled.spans shouldHaveSize 1
        styled.spans[0].range shouldBe 0..10
        styled.spans[0].attributes[InlineCodeKey] shouldBe Unit
        styled.spans[0].attributes[BoldKey] shouldBe Unit
        styled.spans[0].attributes[HeadingKey] shouldBe HeadingLevel.H1

        // Clear all on paragraph 1
        val cleared =
            styled.edit {
                editAttributes(0..10) {
                    clearAll()
                }
            }
        cleared.spans.shouldBeEmpty()
    }

    @Test
    fun `span shifting handles deletions at boundary inside and complete wipe`() {
        val initialText = "prefix [code] suffix"
        val codeRange = initialText.rangeOf("[code]")
        val richString =
            RichString(initialText).edit {
                editAttributes(codeRange) {
                    inlineCode()
                }
            }

        // 1. Delete text before span
        val state1 = RichTextState(richString)
        state1.edit {
            delete(0 until 7) // deletes "prefix "
        }
        state1.richString.text shouldBe "[code] suffix"
        state1.richString.spans shouldHaveSize 1
        state1.richString.spans[0].range shouldBe 0..5
        state1.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // 2. Partial delete overlapping start of span
        val state2 = RichTextState(richString)
        state2.edit {
            delete(5..9) // deletes "x [co"
        }
        state2.richString.text shouldBe "prefide] suffix"
        state2.richString.spans shouldHaveSize 1
        // original was 7..12. 5..9 deleted (5 chars).
        // Overlap: 7..9 deleted. Remaining span is original 10..12 shifted left by 5 -> 5..7 ("de]")
        state2.richString.spans[0].range shouldBe 5..7
        state2.richString.text.substring(5..7) shouldBe "de]"
        state2.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // 3. Partial delete inside span
        val state3 = RichTextState(richString)
        state3.edit {
            // In "prefix [code] suffix":
            // index 7='[', 8='c', 9='o', 10='d', 11='e', 12=']'
            // To delete "ode", range is 9..11!
            delete(9..11)
        }
        state3.richString.text shouldBe "prefix [c] suffix"
        state3.richString.spans shouldHaveSize 1
        state3.richString.spans[0].range shouldBe 7..9 // "[c]"
        state3.richString.text.substring(7..9) shouldBe "[c]"
        state3.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // 4. Complete deletion of span
        val state4 = RichTextState(richString)
        state4.edit {
            delete(initialText.rangeOf("[code]"))
        }
        state4.richString.text shouldBe "prefix  suffix"
        state4.richString.spans.shouldBeEmpty()
    }

    // =========================================================================
    // 4. Multi-range Complex Interleaving & Chunks
    // =========================================================================

    @Test
    fun `overlapping InlineCodeKey and BoldKey properly tessellate and clearCode preserves BoldKey`() {
        // Text: 01234567890123456789 (20 chars)
        val text = "ABCDEFGHIJKLMNOPQRST"
        val styled =
            RichString(text).edit {
                editAttributes(0..14) { bold() }
                editAttributes(5..19) { inlineCode() }
            }

        // Expected chunks:
        // 0..4: bold only
        // 5..14: bold + code
        // 15..19: code only
        styled.spans shouldHaveSize 3

        styled.spans[0].range shouldBe 0..4
        styled.spans[0].attributes[BoldKey] shouldBe Unit
        styled.spans[0].attributes[InlineCodeKey].shouldBeNull()

        styled.spans[1].range shouldBe 5..14
        styled.spans[1].attributes[BoldKey] shouldBe Unit
        styled.spans[1].attributes[InlineCodeKey] shouldBe Unit

        styled.spans[2].range shouldBe 15..19
        styled.spans[2].attributes[BoldKey].shouldBeNull()
        styled.spans[2].attributes[InlineCodeKey] shouldBe Unit

        // Clear code across 10..17
        val cleared =
            styled.edit {
                editAttributes(10..17) {
                    clearInlineCode()
                }
            }

        // Chunks after clearCode on 10..17:
        // 0..4: bold
        // 5..9: bold + code
        // 10..14: bold (code removed)
        // 15..17: empty (omitted)
        // 18..19: code
        // Note: 0..4 (bold) and 10..14 (bold) cannot coalesce because 5..9 is between them.
        cleared.spans shouldHaveSize 4
        cleared.spans[0].range shouldBe 0..4
        cleared.spans[0].attributes shouldBe attributeContainerOf(BoldKey to Unit)

        cleared.spans[1].range shouldBe 5..9
        cleared.spans[1].attributes shouldBe (attributeContainerOf(BoldKey to Unit) + (InlineCodeKey to Unit))

        cleared.spans[2].range shouldBe 10..14
        cleared.spans[2].attributes shouldBe attributeContainerOf(BoldKey to Unit)

        cleared.spans[3].range shouldBe 18..19
        cleared.spans[3].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)
    }

    // =========================================================================
    // 5. RichTextState Typing Attribute Workflow with InlineCodeKey
    // =========================================================================

    @Test
    fun `toggleFormat and typingAttributes apply InlineCodeKey to typed characters`() {
        val state = RichTextState(RichString("Hello "))

        // Place cursor at end (collapsed)
        state.textFieldState.edit { selection = TextRange(6) }

        // Toggle code on
        state.toggleFormat(InlineCodeKey)
        state.typingAttributes.shouldNotBeNull()
        state.typingAttributes?.get(InlineCodeKey) shouldBe Unit

        // Type characters via edit or insert
        state.edit {
            insert(6, "code") {
                inlineCode()
            }
        }

        state.richString.text shouldBe "Hello code"
        state.richString.spans shouldHaveSize 1
        state.richString.spans[0].range shouldBe 6..9
        state.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // Clear formats
        state.textFieldState.edit { selection = TextRange(10) }
        state.clearFormats()
        state.typingAttributes.shouldBeNull()
    }

    // =========================================================================
    // 6. Subrange clearAll with InlineCodeKey Slicing
    // =========================================================================

    @Test
    fun `clearAll on subrange slices InlineCodeKey cleanly into two separate spans`() {
        val text = "0123456789ABCDEF"
        val styled =
            RichString(text).edit {
                editAttributes(0..15) {
                    inlineCode()
                }
            }
        styled.spans shouldHaveSize 1

        // clearAll on 5..10
        val sliced =
            styled.edit {
                editAttributes(5..10) {
                    clearAll()
                }
            }

        sliced.spans shouldHaveSize 2
        sliced.spans[0].range shouldBe 0..4
        sliced.spans[0].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)

        sliced.spans[1].range shouldBe 11..15
        sliced.spans[1].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)
    }

    // =========================================================================
    // 7. Contiguous & Redundant InlineCodeKey Applications Merge Cleanly
    // =========================================================================

    @Test
    fun `contiguous code spans coalesce into a single span`() {
        val text = "0123456789"
        val styled =
            RichString(text).edit {
                editAttributes(0..4) { inlineCode() }
                editAttributes(5..9) { inlineCode() }
            }

        styled.spans shouldHaveSize 1
        styled.spans[0].range shouldBe 0..9
        styled.spans[0].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)

        // Redundant application on middle subrange 2..7
        val reApplied =
            styled.edit {
                editAttributes(2..7) { inlineCode() }
            }
        reApplied.spans shouldHaveSize 1
        reApplied.spans[0].range shouldBe 0..9
        reApplied.spans[0].attributes shouldBe attributeContainerOf(InlineCodeKey to Unit)
    }

    // =========================================================================
    // 8. Paragraph Attributes Independence with InlineCodeKey
    // =========================================================================

    @Test
    fun `paragraph attributes do not expand InlineCodeKey and snap independently`() {
        val text = "First line with `code` here\nSecond line"
        val codeRange = text.rangeOf("`code`")
        val styled =
            RichString(text).edit {
                editAttributes(codeRange) {
                    inlineCode()
                }
                editAttributes(0..5) {
                    headingLevel(HeadingLevel.H1)
                }
            }

        // Heading snaps to 0..27 (entire first line including \n)
        // Code is only on codeRange
        // Result should have:
        // 0..(codeRange.first - 1): Heading only
        // codeRange: Heading + Code
        // (codeRange.last + 1)..27: Heading only
        styled.spans shouldHaveSize 3

        val codeSpan = styled.spans.first { it.attributes.containsKey(InlineCodeKey) }
        codeSpan.range shouldBe codeRange
        codeSpan.attributes[InlineCodeKey] shouldBe Unit
        codeSpan.attributes[HeadingKey] shouldBe HeadingLevel.H1

        // Other spans must NOT have InlineCodeKey
        styled.spans.filter { !it.attributes.containsKey(InlineCodeKey) }.forEach { nonCodeSpan ->
            nonCodeSpan.attributes[InlineCodeKey].shouldBeNull()
            nonCodeSpan.attributes[HeadingKey] shouldBe HeadingLevel.H1
        }
    }

    // =========================================================================
    // 9. Multiple Independent Code Spans Shifting
    // =========================================================================

    @Test
    fun `multiple separated code spans shift correctly when intermediate text is modified`() {
        val text = "codeA middle_text codeB tail"
        val state =
            RichTextState(
                RichString(text).edit {
                    editAttributes(text.rangeOf("codeA")) { inlineCode() }
                    editAttributes(text.rangeOf("codeB")) { inlineCode() }
                },
            )

        state.richString.spans shouldHaveSize 2
        val origRangeA = state.richString.text.rangeOf("codeA")
        val origRangeB = state.richString.text.rangeOf("codeB")
        state.richString.spans[0].range shouldBe origRangeA
        state.richString.spans[1].range shouldBe origRangeB

        // Replace "middle_text" with "very_long_middle_content_inserted"
        val middleRange = state.richString.text.rangeOf("middle_text")
        val replacement = "very_long_middle_content_inserted"
        state.edit {
            replace(middleRange, replacement)
        }

        state.richString.spans shouldHaveSize 2
        // Span A before middle_text is unchanged
        state.richString.spans[0].range shouldBe origRangeA
        state.richString.spans[0].attributes[InlineCodeKey] shouldBe Unit

        // Span B shifted by diff in length
        val shiftDiff = replacement.length - "middle_text".length
        val expectedRangeB = (origRangeB.first + shiftDiff)..(origRangeB.last + shiftDiff)
        state.richString.spans[1].range shouldBe expectedRangeB
        state.richString.spans[1].attributes[InlineCodeKey] shouldBe Unit
        state.richString.text.substring(expectedRangeB) shouldBe "codeB"
    }

    // =========================================================================
    // 10. Invariant Fuzz Test: 1,000 Edits on InlineCodeKey and Attributes
    // =========================================================================

    @Test
    fun `invariant fuzz test with 1000 iterations maintains span integrity`() {
        var str = RichString("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
        val random = Random(42)

        repeat(1000) { iter ->
            val len = str.text.length
            val start = random.nextInt(len)
            val end = random.nextInt(start, len)
            val range = start..end

            str =
                str.edit {
                    when (iter % 4) {
                        0 -> editAttributes(range) { inlineCode() }
                        1 -> editAttributes(range) { bold() }
                        2 -> editAttributes(range) { clearInlineCode() }
                        3 -> editAttributes(range) { clearAll() }
                    }
                }

            // Invariants check:
            // 1. Spans must not be empty range
            // 2. Spans must be strictly within 0 until str.text.length
            // 3. Consecutive spans must not have identical attributes (merger invariant)
            // 4. No spans with empty attributes
            var prevEnd = -1
            var prevAttrs: AttributeContainer? = null
            for (span in str.spans) {
                (span.range.first >= 0) shouldBe true
                (span.range.last < str.text.length) shouldBe true
                (span.range.first <= span.range.last) shouldBe true
                (span.attributes.isNotEmpty()) shouldBe true

                if (prevEnd != -1) {
                    // Check non-overlapping chunks
                    (span.range.first > prevEnd) shouldBe true
                    // If adjacent, attributes cannot be identical
                    if (span.range.first == prevEnd + 1 && prevAttrs != null) {
                        (span.attributes != prevAttrs) shouldBe true
                    }
                }
                prevEnd = span.range.last
                prevAttrs = span.attributes
            }
        }
    }
}
