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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Modifiers are built from `Modifier.Node` elements, not composition: no `@Composable`
 * `Modifier` extensions and no `Modifier.composed { }`.
 */
class NoComposableModifiersRule :
    Rule(
        ruleId = RuleId("$HeronRuleSetId:no-composable-modifiers"),
        about = About(
            maintainer = "Heron",
        ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (node.elementType) {
            ElementType.FUN -> {
                val function = node.psi as? KtNamedFunction ?: return
                if (!function.isComposableModifier()) return
                emit(
                    function.textOffset,
                    "Modifiers must not be @Composable; build them from a Modifier.Node element",
                    false,
                )
            }

            ElementType.CALL_EXPRESSION -> {
                val call = node.psi as? KtCallExpression ?: return
                if (call.calleeExpression?.text != "composed") return
                if (call.lambdaArguments.isEmpty()) return
                emit(
                    node.startOffset,
                    "Don't use Modifier.composed; build the modifier from a Modifier.Node element",
                    false,
                )
            }
        }
    }
}

private fun KtNamedFunction.isComposableModifier(): Boolean =
    receiverTypeReference?.text == "Modifier" &&
        annotationEntries.any { it.shortName?.asString() == "Composable" }
