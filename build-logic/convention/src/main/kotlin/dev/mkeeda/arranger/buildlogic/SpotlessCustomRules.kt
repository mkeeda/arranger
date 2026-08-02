package dev.mkeeda.arranger.buildlogic

import java.io.Serializable

object SpotlessCustomRules : (String) -> String, Serializable {
    override fun invoke(content: String): String = noFullyQualifiedNames(content)

    private val fullyQualifiedPattern = Regex("""(?<!\bimport\s)(?<!\bpackage\s)(androidx\.compose\.[a-zA-Z0-9_.]+|dev\.mkeeda\.[a-zA-Z0-9_.]+)""")

    /**
     * Mechanically prevents the usage of fully qualified names for project-specific or standard compose packages
     * in the body of Kotlin files.
     * Import aliases (`import package.Name as Alias`) should be used if there is a naming collision.
     */
    fun noFullyQualifiedNames(content: String): String {
        val lines = content.lines()
        val violations = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            val isComment = trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*/")
            if (!trimmed.startsWith("package ") && !trimmed.startsWith("import ") && !isComment) {
                val match = fullyQualifiedPattern.find(trimmed)
                if (match != null) {
                    violations.add("Line ${index + 1}: Found fully qualified name '${match.value}' in code body.\n    $trimmed")
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw AssertionError(
                "Fully qualified names are not allowed in the code body. " +
                "Please use imports or import aliases (e.g., `import package.ClassName as AliasName`) if there is a naming collision.\n" +
                "Violations found:\n" + violations.joinToString("\n")
            )
        }

        return content
    }
}
