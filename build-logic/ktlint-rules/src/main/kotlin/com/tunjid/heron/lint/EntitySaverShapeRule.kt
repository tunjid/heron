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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Keeps `MultipleEntitySaver` a simple buffer: callers either `add` or `remove` entities.
 */
class EntitySaverShapeRule :
    Rule(
        ruleId = RuleId("$HeronRuleSetId:entity-saver-shape"),
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
        val saver = node.psi as? KtClass ?: return
        if (saver.name != SaverClassName) return

        val lazyListNames = saver.lazyListPropertyNames()

        saver.primaryConstructorParameters
            .filter { it.hasValOrVar() && !it.isDao() }
            .forEach { parameter ->
                emit(
                    parameter.textOffset,
                    "$SaverClassName constructor properties must be DAOs",
                    false,
                )
            }

        saver.declarations.forEach { declaration ->
            when (declaration) {
                is KtProperty -> if (declaration.name !in lazyListNames) emit(
                    declaration.textOffset,
                    "$SaverClassName may only buffer entities in private LazyList properties",
                    false,
                )

                is KtNamedFunction -> declaration.checkFunction(
                    lazyListNames = lazyListNames,
                    emit = emit,
                )

                else -> Unit
            }
        }
    }
}

private fun KtNamedFunction.checkFunction(
    lazyListNames: Set<String>,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
) {
    val functionName = name
    if (functionName in VerbNames) {
        if (!isVerbAppend(lazyListNames = lazyListNames)) emit(
            textOffset,
            "$SaverClassName.$functionName must take one entity and only append it to a LazyList; " +
                "triage belongs in flushPendingOperations",
            false,
        )
        return
    }
    if (isPrivate() || functionName in AllowedFunctionNames) return
    emit(
        textOffset,
        "$SaverClassName only exposes add, remove and flushPendingOperations; " +
            "express intent with entities (e.g. stubbed sentinels), not new operations",
        false,
    )
}

/**
 * Matches `fun add(entity: T) = someLazyList.add(entity)`.
 */
private fun KtNamedFunction.isVerbAppend(
    lazyListNames: Set<String>,
): Boolean {
    val parameter = valueParameters.singleOrNull() ?: return false
    if (receiverTypeReference != null) return false
    val body = bodyExpression as? KtDotQualifiedExpression ?: return false
    val receiver = body.receiverExpression as? KtNameReferenceExpression ?: return false
    val call = body.selectorExpression as? KtCallExpression ?: return false
    val argument = call.valueArguments.singleOrNull()
        ?.getArgumentExpression() as? KtNameReferenceExpression
        ?: return false

    return receiver.getReferencedName() in lazyListNames &&
        call.calleeExpression?.text == "add" &&
        argument.getReferencedName() == parameter.name
}

private fun KtClass.lazyListPropertyNames(): Set<String> = declarations
    .filterIsInstance<KtProperty>()
    .filter { property ->
        property.isPrivate() &&
            !property.isVar &&
            property.initializer
                ?.let { it as? KtCallExpression }
                ?.calleeExpression
                ?.text == "LazyList"
    }
    .mapNotNull(KtProperty::getName)
    .toSet()

private fun KtParameter.isDao(): Boolean =
    !isMutable && typeReference?.text?.endsWith("Dao") == true

private fun KtModifierListOwner.isPrivate(): Boolean =
    hasModifier(KtTokens.PRIVATE_KEYWORD)

private const val SaverClassName = "MultipleEntitySaver"

private val VerbNames = setOf(
    "add",
    "remove",
)

private val AllowedFunctionNames = setOf(
    "flushPendingOperations",
    // Composes the private embed adds so posts and their embeds are always saved together.
    "associatePostEmbeds",
)
