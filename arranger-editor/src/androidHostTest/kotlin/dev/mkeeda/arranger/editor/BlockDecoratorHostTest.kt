package dev.mkeeda.arranger.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.AttributeKey
import dev.mkeeda.arranger.richtext.BlockTypeAttributeKey
import dev.mkeeda.arranger.richtext.BlockquoteKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.rangeOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlockDecoratorHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private enum class CalloutType {
        Info,
        Warning,
    }

    private object CalloutKey : BlockTypeAttributeKey<CalloutType> {
        override val name: String = "callout"
        override val defaultValue: CalloutType = CalloutType.Info
    }

    private object UnregisteredKey : BlockTypeAttributeKey<String> {
        override val name: String = "unregistered"
        override val defaultValue: String = ""
    }

    @Test
    fun `BlockDecorator DSL dispatches decoration to registered handler with typed value and context`() {
        var invokedValue: CalloutType? = null
        var invokedContext: BlockDecorationContext? = null

        val decorator =
            BlockDecorator {
                on(CalloutKey) { value, context ->
                    invokedValue = value
                    invokedContext = context
                }
            }

        composeTestRule.setContent {
            decorator.Decoration(
                key = CalloutKey,
                value = CalloutType.Warning,
                context = BlockDecorationContext(range = 0..10, lineCount = 2, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        invokedValue shouldBe CalloutType.Warning
        invokedContext.shouldNotBeNull()
        invokedContext.range shouldBe 0..10
        invokedContext.lineCount shouldBe 2
    }

    @Test
    fun `BlockDecorator DSL delegates unhandled key to base decorator`() {
        var baseInvokedKey: AttributeKey<*>? = null
        var baseInvokedValue: Any? = null

        val base =
            BlockDecorator {
                on(BlockquoteKey) { value, _ ->
                    baseInvokedKey = BlockquoteKey
                    baseInvokedValue = value
                }
            }

        val custom =
            BlockDecorator(base = base) {
                on(CalloutKey) { _, _ -> }
            }

        composeTestRule.setContent {
            custom.Decoration(
                key = BlockquoteKey,
                value = Unit,
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        baseInvokedKey shouldBe BlockquoteKey
        baseInvokedValue shouldBe Unit
    }

    @Test
    fun `BlockDecorator DSL overrides handler for key already handled by base decorator`() {
        var baseInvoked = false
        var customInvoked = false

        val base =
            BlockDecorator {
                on(CalloutKey) { _, _ ->
                    baseInvoked = true
                }
            }

        val custom =
            BlockDecorator(base = base) {
                on(CalloutKey) { _, _ ->
                    customInvoked = true
                }
            }

        composeTestRule.setContent {
            custom.Decoration(
                key = CalloutKey,
                value = CalloutType.Info,
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        customInvoked shouldBe true
        baseInvoked shouldBe false
    }

    @Test
    fun `BlockDecorator DSL gracefully performs no-op when key is unhandled and base is null`() {
        val decorator =
            BlockDecorator {
                on(CalloutKey) { _, _ -> }
            }

        composeTestRule.setContent {
            decorator.Decoration(
                key = UnregisteredKey,
                value = "test",
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `RichTextEditor renders custom BlockDecorator overlay`() {
        val customDecorator =
            BlockDecorator(base = DefaultBlockDecorator) {
                on(CalloutKey) { _, context ->
                    BlockContainer(
                        context = context,
                        leading = {
                            Box(
                                modifier =
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .testTag("custom_callout_leading"),
                            )
                        },
                    )
                }
            }

        val text = "Callout text line\nSecond line"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.indices) {
                            setParagraphAttribute(CalloutKey, CalloutType.Info)
                        }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = customDecorator,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("custom_callout_leading", useUnmergedTree = true).assertExists()
    }

    private data class ComplexConfig(
        val title: String,
        val level: Int,
    )

    private object ComplexConfigKey : BlockTypeAttributeKey<ComplexConfig> {
        override val name: String = "complex_config"
        override val defaultValue: ComplexConfig = ComplexConfig("default", 0)
    }

    private object NullableNoticeKey : BlockTypeAttributeKey<String?> {
        override val name: String = "nullable_notice"
        override val defaultValue: String? = null
    }

    private object PanelKey : BlockTypeAttributeKey<String> {
        override val name: String = "panel"
        override val defaultValue: String = "default"
    }

    @Test
    fun `BlockDecorator DSL dispatches recursively across 3-tier base decorators`() {
        var tier1Rendered = false
        var tier2Rendered = false
        var tier3Rendered = false

        val tier1 =
            BlockDecorator {
                on(CalloutKey) { _, _ ->
                    tier1Rendered = true
                }
            }
        val tier2 =
            BlockDecorator(base = tier1) {
                on(PanelKey) { _, _ ->
                    tier2Rendered = true
                }
            }
        val tier3 =
            BlockDecorator(base = tier2) {
                on(ComplexConfigKey) { _, _ ->
                    tier3Rendered = true
                }
            }

        composeTestRule.setContent {
            tier3.Decoration(
                key = CalloutKey,
                value = CalloutType.Info,
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
            tier3.Decoration(
                key = PanelKey,
                value = "test_panel",
                context = BlockDecorationContext(range = 6..10, lineCount = 1, modifier = Modifier),
            )
            tier3.Decoration(
                key = ComplexConfigKey,
                value = ComplexConfig("Header", 1),
                context = BlockDecorationContext(range = 11..15, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        tier1Rendered shouldBe true
        tier2Rendered shouldBe true
        tier3Rendered shouldBe true
    }

    @Test
    fun `BlockDecorator DSL correctly passes complex data class value to composable lambda`() {
        var observedConfig: ComplexConfig? = null

        val decorator =
            BlockDecorator {
                on(ComplexConfigKey) { config, _ ->
                    observedConfig = config
                    Box(modifier = Modifier.testTag("complex_${config.title}_${config.level}"))
                }
            }

        composeTestRule.setContent {
            decorator.Decoration(
                key = ComplexConfigKey,
                value = ComplexConfig(title = "Alert", level = 3),
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        observedConfig shouldBe ComplexConfig(title = "Alert", level = 3)
        composeTestRule.onNodeWithTag("complex_Alert_3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `BlockDecorator DSL correctly handles nullable attribute value with null argument`() {
        var observedNotice: String? = "initial"

        val decorator =
            BlockDecorator {
                on(NullableNoticeKey) { notice, _ ->
                    observedNotice = notice
                    Box(modifier = Modifier.testTag("nullable_notice_box"))
                }
            }

        composeTestRule.setContent {
            decorator.Decoration(
                key = NullableNoticeKey,
                value = null,
                context = BlockDecorationContext(range = 0..5, lineCount = 1, modifier = Modifier),
            )
        }
        composeTestRule.waitForIdle()

        observedNotice shouldBe null
        composeTestRule.onNodeWithTag("nullable_notice_box", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `RichTextEditor renders both adjacent distinct custom blocks independently`() {
        val decorator =
            BlockDecorator(base = DefaultBlockDecorator) {
                on(CalloutKey) { _, context ->
                    BlockContainer(
                        context = context,
                        leading = {
                            Box(
                                modifier =
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .testTag("adjacent_callout"),
                            )
                        },
                    )
                }
                on(PanelKey) { _, context ->
                    BlockContainer(
                        context = context,
                        leading = {
                            Box(
                                modifier =
                                    Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .testTag("adjacent_panel"),
                            )
                        },
                    )
                }
            }

        val text = "Callout row\nPanel row"
        val state =
            RichTextState(
                initialText =
                    RichString(text)
                        .edit {
                            editAttributes(text.rangeOf("Callout row")) {
                                setParagraphAttribute(CalloutKey, CalloutType.Info)
                            }
                        }
                        .edit {
                            editAttributes(text.rangeOf("Panel row")) {
                                setParagraphAttribute(PanelKey, "warning")
                            }
                        },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = decorator,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("adjacent_callout", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("adjacent_panel", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `RichTextEditor dynamically updates custom block decoration when text is edited`() {
        var renderedContext: BlockDecorationContext? = null

        val decorator =
            BlockDecorator {
                on(CalloutKey) { _, context ->
                    renderedContext = context
                    BlockContainer(
                        context = context,
                        leading = {
                            Box(
                                modifier =
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .testTag("dynamic_callout_leading"),
                            )
                        },
                    )
                }
            }

        val text = "Initial callout line"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.indices) {
                            setParagraphAttribute(CalloutKey, CalloutType.Info)
                        }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = decorator,
            )
        }
        composeTestRule.waitForIdle()

        renderedContext.shouldNotBeNull()
        renderedContext.range shouldBe 0..text.lastIndex
        renderedContext.lineCount shouldBe 1
        composeTestRule.onNodeWithTag("dynamic_callout_leading", useUnmergedTree = true).assertExists()

        // Append text to the paragraph
        composeTestRule.onNodeWithText(text).performTextInput(" - appended text")
        composeTestRule.waitForIdle()

        renderedContext.shouldNotBeNull()
        renderedContext.range shouldBe 0..state.richString.text.lastIndex
    }

    @Test
    fun `RichTextEditor dynamically increases lineCount when newline is inserted into block`() {
        var renderedContext: BlockDecorationContext? = null

        val decorator =
            BlockDecorator {
                on(CalloutKey) { _, context ->
                    renderedContext = context
                }
            }

        val text = "Line 1"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.indices) {
                            setParagraphAttribute(CalloutKey, CalloutType.Info)
                        }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = decorator,
            )
        }
        composeTestRule.waitForIdle()

        renderedContext.shouldNotBeNull()
        renderedContext.lineCount shouldBe 1

        // Insert newline and line 2 into the block
        state.edit {
            editAttributes(0..text.lastIndex) {
                // Keep the attribute across the updated range
            }
        }
        composeTestRule.onNodeWithText(text).performTextInput("\nLine 2")
        composeTestRule.waitForIdle()

        renderedContext.shouldNotBeNull()
        renderedContext.lineCount shouldBe 2
    }

    @Test
    fun `RichTextEditor removes block decoration when block attribute is cleared`() {
        val decorator =
            BlockDecorator {
                on(CalloutKey) { _, context ->
                    BlockContainer(
                        context = context,
                        leading = {
                            Box(
                                modifier =
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .testTag("removable_callout"),
                            )
                        },
                    )
                }
            }

        val text = "Decorated line"
        val state =
            RichTextState(
                initialText =
                    RichString(text).edit {
                        editAttributes(text.indices) {
                            setParagraphAttribute(CalloutKey, CalloutType.Info)
                        }
                    },
            )

        composeTestRule.setContent {
            RichTextEditor(
                state = state,
                blockDecorator = decorator,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("removable_callout", useUnmergedTree = true).assertExists()

        // Clear the paragraph attribute by updating the richString to plain text
        state.setRichString(RichString(text))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("removable_callout", useUnmergedTree = true).assertDoesNotExist()
    }
}
