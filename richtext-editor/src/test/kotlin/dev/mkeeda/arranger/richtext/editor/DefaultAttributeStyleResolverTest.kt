package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.unit.sp
import dev.mkeeda.arranger.richtext.ListIndentLevel
import io.kotest.matchers.shouldBe
import org.junit.Test

class DefaultAttributeStyleResolverTest {
    @Test
    fun listIndentLevel_toIndent_values() {
        // Level1 should produce 1 * 24 = 24.sp
        ListIndentLevel.Level1.toIndent() shouldBe 24.sp

        // Level2 should produce 2 * 24 = 48.sp
        ListIndentLevel.Level2.toIndent() shouldBe 48.sp

        // Level6 should produce 6 * 24 = 144.sp
        ListIndentLevel.Level6.toIndent() shouldBe 144.sp

        // Unspecified should produce 0.sp
        ListIndentLevel.Unspecified.toIndent() shouldBe 0.sp
    }
}
