package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult

internal fun SemanticsNodeInteraction.clickOnCharacter(index: Int) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(textLayoutResults)
    val layoutResult = textLayoutResults.first()
    val boundingBox = layoutResult.getBoundingBox(index)
    performTouchInput {
        advanceEventTime(1000)
        click(boundingBox.center)
    }
}
