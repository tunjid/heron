/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.lint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Room reads in Heron must be reactive, consumers must be able to handle data changing under them.
 * A read `@Query` must return a `Flow` and must not suspend.
 */
class DaoReactiveReadsRule :
    Rule(
        ruleId = RuleId("$HeronRuleSetId:dao-reactive-reads"),
        about = About(
            maintainer = "Heron",
        ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.FUN) return
        val function = node.psi as? KtNamedFunction ?: return
        if (function.containingClassOrObject?.hasAnnotation(name = "Dao") != true) return

        val query = function.queryText() ?: return
        if (!query.isRead()) return

        if (function.hasModifier(KtTokens.SUSPEND_KEYWORD)) emit(
            node.startOffset,
            "DAO reads must be reactive: return a Flow instead of suspending",
            false,
        )
        else if (function.typeReference?.text?.startsWith("Flow<") != true) emit(
            node.startOffset,
            "DAO reads must return a Flow",
            false,
        )
    }
}

private fun KtAnnotated.hasAnnotation(
    name: String,
): Boolean = annotationEntries.any { it.shortName?.asString() == name }

private fun KtNamedFunction.queryText(): String? = annotationEntries
    .firstOrNull { it.shortName?.asString() == "Query" }
    ?.valueArguments
    ?.firstOrNull()
    ?.getArgumentExpression()
    ?.let { it as? KtStringTemplateExpression }
    ?.entries
    ?.joinToString(separator = "") { it.text }

private fun String.isRead(): Boolean = trimStart()
    .substringBefore(delimiter = ' ')
    .substringBefore(delimiter = '\n')
    .uppercase()
    .let { it == "SELECT" || it == "WITH" }

private val KtNamedFunction.containingClassOrObject: KtClassOrObject?
    get() = parent?.parent as? KtClassOrObject
