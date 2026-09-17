package dev.mkeeda.arranger.richtext.editor

import dev.mkeeda.arranger.richtext.BoldKey
import dev.mkeeda.arranger.richtext.RichSpan
import dev.mkeeda.arranger.richtext.attributeContainerOf
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SpanClickEventTest {
    @Test
    fun `SpanClickEvent holds rich span and is not consumed by default`() {
        val span = RichSpan(range = 0..5, attributes = attributeContainerOf(BoldKey to Unit))
        val event = SpanClickEvent(span = span)

        event.span shouldBe span
        event.isConsumed shouldBe false
    }

    @Test
    fun `consume sets isConsumed to true`() {
        val span = RichSpan(range = 0..5, attributes = attributeContainerOf(BoldKey to Unit))
        val event = SpanClickEvent(span = span)

        event.consume()

        event.isConsumed shouldBe true
    }

    @Test
    fun `multiple consume calls keep isConsumed true`() {
        val span = RichSpan(range = 0..5, attributes = attributeContainerOf(BoldKey to Unit))
        val event = SpanClickEvent(span = span)

        event.consume()
        event.consume()

        event.isConsumed shouldBe true
    }
}
