package dev.mkeeda.arranger.runtime.node

class HeadingNode : MarkupTextNode() {
    var level: HeadingLevel = HeadingLevel.H1
        internal set

    var title: String = ""
        internal set

    override fun toSemanticText(): String = title

    override fun equals(other: Any?): Boolean {
        return if (other is HeadingNode) {
            level == other.level && title == other.title
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + level.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    override fun toString(): String {
        return "HeadingNode(level = $level, title = $title)"
    }
}

enum class HeadingLevel {
    H1, H2, H3;
}

fun HeadingNode(title: String, level: HeadingLevel): HeadingNode = HeadingNode().apply {
    this.title = title
    this.level = level
}
