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
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass

/**
 * Scoping a dependency is decided where the type is bound.
 */
class NoScopeOnImplRule :
    Rule(
        ruleId = RuleId("$HeronRuleSetId:no-scope-on-impl"),
        about = About(
            maintainer = "Heron",
        ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.CLASS) return
        val implementation = node.psi as? KtClass ?: return
        if (!implementation.isInjected()) return

        implementation.annotationEntries
            .filter { it.shortName?.asString() == "SingleIn" }
            .forEach { annotation ->
                emit(
                    annotation.textOffset,
                    "Don't scope @Inject classes; put @SingleIn on the provider in di/Bindings.kt",
                    false,
                )
            }
    }
}

private fun KtClass.isInjected(): Boolean =
    hasAnnotation(name = "Inject") ||
        primaryConstructor?.hasAnnotation(name = "Inject") == true

private fun KtAnnotated.hasAnnotation(
    name: String,
): Boolean = annotationEntries.any { it.shortName?.asString() == name }
