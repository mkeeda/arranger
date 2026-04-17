package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.HexColor
import dev.mkeeda.arranger.richtext.TextSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttributeConversionsTest {
    @Test
    fun `HexColor toColor returns expected Color`() {
        HexColor("#FF0000").toColor() shouldBe Color(0xFFFF0000)
        HexColor("#00FF00").toColor() shouldBe Color(0xFF00FF00)
    }

    @Test
    fun `HexColor toColor returns Unspecified when color is Unspecified`() {
        HexColor.Unspecified.toColor() shouldBe Color.Unspecified
    }

    @Test
    fun `HexColor toColor returns Unspecified when color format is bad`() {
        HexColor("invalid").toColor() shouldBe Color.Unspecified
    }

    @Test
    fun `TextSize toTextUnit returns expected TextUnit`() {
        TextSize(16f).toTextUnit() shouldBe 16f.sp
    }

    @Test
    fun `TextSize toTextUnit returns Unspecified when size is Unspecified`() {
        TextSize.Unspecified.toTextUnit() shouldBe androidx.compose.ui.unit.TextUnit.Unspecified
    }
}
