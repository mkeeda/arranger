package dev.mkeeda.arranger.runtime

abstract class DocumentNode {
    val children = mutableListOf<DocumentNode>()

    abstract fun render(): String
}

class TextNode : DocumentNode() {
    var text: String = ""
    override fun render(): String = text
}

class HeadingNode : DocumentNode() {
    var level: HeadingLevel = HeadingLevel.H1
    var title: String = ""
    override fun render(): String = title
}

class GroupNode : DocumentNode() {
    override fun render(): String {
        return children.joinToString(separator = "\n") { it.render() }
    }
}
