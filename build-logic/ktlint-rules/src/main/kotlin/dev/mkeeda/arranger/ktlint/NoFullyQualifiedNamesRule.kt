package dev.mkeeda.arranger.ktlint

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoFullyQualifiedNamesRule : Rule(
    ruleId = RuleId("arranger:no-fully-qualified-names"),
    about = About(
        maintainer = "Arranger",
        repositoryUrl = "https://github.com/mkeeda/arranger",
        issueTrackerUrl = "https://github.com/mkeeda/arranger/issues",
    ),
) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == ElementType.DOT_QUALIFIED_EXPRESSION || node.elementType == ElementType.USER_TYPE) {
            // Check if node is part of an import or package statement
            if (node.isInsideImportOrPackage()) {
                return
            }

            // Also ensure we are at the outermost qualified expression to avoid multiple errors for one expression
            if (node.treeParent?.elementType == ElementType.DOT_QUALIFIED_EXPRESSION ||
                node.treeParent?.elementType == ElementType.USER_TYPE
            ) {
                return
            }

            val text = node.text
            if (isFullyQualified(node.elementType, text)) {
                emit(
                    node.startOffset,
                    "Fully qualified name '$text' is not allowed in code body. Use imports or import aliases (e.g. `import package.ClassName as AliasName`) instead.",
                    false,
                )
            }
        }
    }

    private fun isFullyQualified(elementType: org.jetbrains.kotlin.com.intellij.psi.tree.IElementType, text: String): Boolean {
        if (elementType == ElementType.USER_TYPE) {
            return USER_TYPE_QUALIFIED_PATTERN.containsMatchIn(text)
        }
        return PACKAGE_PREFIX_PATTERN.containsMatchIn(text) || MULTI_PACKAGE_CLASS_PATTERN.containsMatchIn(text)
    }

    private fun ASTNode.isInsideImportOrPackage(): Boolean {
        var current: ASTNode? = this.treeParent
        while (current != null) {
            if (current.elementType == ElementType.IMPORT_DIRECTIVE ||
                current.elementType == ElementType.PACKAGE_DIRECTIVE
            ) {
                return true
            }
            current = current.treeParent
        }
        return false
    }

    companion object {
        private val USER_TYPE_QUALIFIED_PATTERN = Regex("""^[a-z][a-zA-Z0-9_]*\..*""")

        private val PACKAGE_PREFIX_PATTERN = Regex(
            """^(androidx|dev|com|org|io|java|javax|kotlin|kotlinx|android|net)\.[a-zA-Z0-9_]+"""
        )

        private val MULTI_PACKAGE_CLASS_PATTERN = Regex(
            """^[a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*)+\.[A-Z]"""
        )
    }
}
