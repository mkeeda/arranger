package dev.mkeeda.arranger.runtime

import dev.mkeeda.arranger.runtime.node.MarkupTextNode

interface MarkupTextParser {
    fun parse(markupText: String): MarkupTextNode
}
