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

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class HeronRuleSetProvider :
    RuleSetProviderV3(
        id = RuleSetId(HeronRuleSetId),
    ) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { DaoReactiveReadsRule() },
        RuleProvider { EntitySaverShapeRule() },
        RuleProvider { NoScopeOnImplRule() },
        RuleProvider { NoComposableModifiersRule() },
    )
}

internal const val HeronRuleSetId = "heron"
