package dev.mkeeda.arranger.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import dev.mkeeda.arranger.core.node.AstNode
import dev.mkeeda.arranger.core.node.RootElement
import dev.mkeeda.arranger.core.node.TextElement
import dev.mkeeda.arranger.core.node.node

@Stable
class EditorState internal constructor(
    internal val rootNode: AstNode = AstNode(element = RootElement())
) {
    val richText: AnnotatedString
        get() = AnnotatedString("")

    private var prevValue by mutableStateOf(TextFieldValue(""))

    fun setText(currentValue: TextFieldValue, editorStyle: EditorStyle?) {
        if (currentValue.selection.collapsed.not()) return

        when {
            prevValue.text.length < currentValue.text.length -> insertText(currentValue, editorStyle)
            prevValue.text.length > currentValue.text.length -> removeText(currentValue, editorStyle)
        }

        savePrevValue(newPrevValue = currentValue)
    }

    private fun insertText(currentValue: TextFieldValue, editorStyle: EditorStyle?) {
        val currentCursorPosition = currentValue.selection.start
        val prevCursorPosition = prevValue.selection.start
        val newText = currentValue.text.subSequence(startIndex = prevCursorPosition, endIndex = currentCursorPosition)
        val newTextNodes = newText.lineSequence().map {  line ->
            TextElement(text = line, editorStyle = editorStyle).node()
        }

        if (currentCursorPosition == currentValue.text.lastIndex) {
            // append to last node
        } else {
            // insert to middle of nodes
        }
    }

    private fun removeText(currentValue: TextFieldValue, editorStyle: EditorStyle?) {

    }

    private fun savePrevValue(newPrevValue: TextFieldValue) {
        prevValue = newPrevValue
    }
}

private fun <T> CharSequence.paragraphMap(transform: (CharSequence) -> T): List<T> {
    TODO()
}
