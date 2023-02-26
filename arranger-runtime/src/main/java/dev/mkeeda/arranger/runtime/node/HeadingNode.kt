package dev.mkeeda.arranger.runtime.node

class HeadingNode : DocumentNode() {
    var level: HeadingLevel = HeadingLevel.H1
    var title: String = ""

    override fun toSemanticText(): String = title
}

enum class HeadingLevel {
    H1, H2, H3;
}
