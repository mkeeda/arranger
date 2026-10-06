package dev.mkeeda.arranger.editor

import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.CodeBlockKey
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BlockDecoratorTest {
    private enum class CalloutType {
        Info,
        Warning,
    }

    private object CalloutKey : BlockTypeAttributeKey<CalloutType> {
        override val name: String = "callout"
        override val defaultValue: CalloutType = CalloutType.Info
    }

    @Test
    fun `supportedKeys contains all registered keys without base decorator`() {
        val decorator =
            BlockDecorator {
                on(CalloutKey) { _, _ -> }
            }

        decorator.supportedKeys shouldBe setOf(CalloutKey)
    }

    @Test
    fun `supportedKeys merges base supportedKeys with custom keys`() {
        val base =
            BlockDecorator {
                on(BlockquoteKey) { _, _ -> }
            }
        val custom =
            BlockDecorator(base = base) {
                on(CalloutKey) { _, _ -> }
            }

        custom.supportedKeys shouldBe setOf(BlockquoteKey, CalloutKey)
    }

    @Test
    fun `supportedKeys preserves existing keys when base has multiple keys`() {
        val custom =
            BlockDecorator(base = DefaultBlockDecorator) {
                on(CalloutKey) { _, _ -> }
            }

        custom.supportedKeys shouldBe setOf(BlockquoteKey, CodeBlockKey, CalloutKey)
    }

    @Test
    fun `supportedKeys handles key override without duplicates`() {
        val custom =
            BlockDecorator(base = DefaultBlockDecorator) {
                on(BlockquoteKey) { _, _ -> }
            }

        custom.supportedKeys shouldBe setOf(BlockquoteKey, CodeBlockKey)
    }

    private object PanelKey : BlockTypeAttributeKey<String> {
        override val name: String = "panel"
        override val defaultValue: String = ""
    }

    @Test
    fun `supportedKeys merges transitively across multi-tier base decorators`() {
        val tier1 =
            BlockDecorator {
                on(BlockquoteKey) { _, _ -> }
            }
        val tier2 =
            BlockDecorator(base = tier1) {
                on(CodeBlockKey) { _, _ -> }
            }
        val tier3 =
            BlockDecorator(base = tier2) {
                on(CalloutKey) { _, _ -> }
            }

        tier3.supportedKeys shouldBe setOf(BlockquoteKey, CodeBlockKey, CalloutKey)
    }

    @Test
    fun `supportedKeys handles override in multi-tier base hierarchy`() {
        val tier1 =
            BlockDecorator {
                on(CalloutKey) { _, _ -> }
                on(BlockquoteKey) { _, _ -> }
            }
        val tier2 =
            BlockDecorator(base = tier1) {
                on(CodeBlockKey) { _, _ -> }
            }
        val tier3 =
            BlockDecorator(base = tier2) {
                on(CalloutKey) { _, _ -> }
            }

        tier3.supportedKeys shouldBe setOf(CalloutKey, BlockquoteKey, CodeBlockKey)
    }

    @Test
    fun `supportedKeys is empty when builder is empty and base is null`() {
        val decorator = BlockDecorator {}
        decorator.supportedKeys shouldBe emptySet()
    }

    @Test
    fun `supportedKeys equals base supportedKeys when builder is empty but base is provided`() {
        val decorator = BlockDecorator(base = DefaultBlockDecorator) {}
        decorator.supportedKeys shouldBe DefaultBlockDecorator.supportedKeys
    }
}
