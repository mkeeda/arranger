package dev.mkeeda.arranger.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class ArrangerRuleSetProvider : RuleSetProviderV3(RuleSetId("arranger")) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { NoFullyQualifiedNamesRule() }
    )
}
