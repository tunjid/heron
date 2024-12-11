package com.tunjid.heron.scaffold.navigation

import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.canPop
import com.tunjid.treenav.popToRoot
import com.tunjid.treenav.switch

internal data class NavItem(
    val stack: AppStack,
    val index: Int,
    val selected: Boolean
)

val MultiStackNav.canGoUp get() = stacks.getOrNull(currentIndex)?.canPop == true

internal val MultiStackNav.navItems
    get() = stacks
        .map(StackNav::name)
        .mapIndexed { index, name ->
            NavItem(
                stack = AppStack.entries.first { it.stackName == name },
                index = index,
                selected = currentIndex == index,
            )
        }

internal fun MultiStackNav.navItemSelected(item: NavItem) =
    if (item.selected) popToRoot(indexToPop = item.index)
    else switch(toIndex = item.index)
