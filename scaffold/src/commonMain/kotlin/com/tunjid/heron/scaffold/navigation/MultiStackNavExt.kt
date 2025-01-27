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

package com.tunjid.heron.scaffold.navigation

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.popToRoot
import com.tunjid.treenav.switch

internal data class NavItem(
    val stack: AppStack,
    val index: Int,
    val selected: Boolean,
)

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

internal val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexedNotNull { index, name ->
            val stack = AppStack.entries.firstOrNull {
                it.stackName == name
            } ?: return@mapIndexedNotNull null
            NavItem(
                stack = stack,
                index = index,
                selected = currentIndex == index,
            )
        }

internal fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)
