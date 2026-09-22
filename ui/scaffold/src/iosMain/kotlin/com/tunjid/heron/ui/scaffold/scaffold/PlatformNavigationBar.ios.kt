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

@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIImage
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarAppearance
import platform.UIKit.UITabBarDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.UIKit.UITabBarItemAppearance
import platform.UIKit.UIView

@Composable
internal actual fun AppScaffoldState.PlatformNavigationBar(
    modifier: Modifier,
    onNavItemReselected: () -> Boolean,
) {
    if (!isIos26OrLater()) {
        CommonNavigationBar(
            modifier = modifier,
            onNavItemReselected = onNavItemReselected,
        )
        return
    }
    with(staticStates) {
        val onSelect by rememberUpdatedState<(Int) -> Unit> { index ->
            val item = navItems[index]
            if (!(item.selected && onNavItemReselected())) onNavItemSelected(item)
        }
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
        val bottomInset = with(LocalDensity.current) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        // Liquid Glass chrome always uses the compact height; a regular height glass bar looks
        // awkward.
        val height = UiTokens.bottomNavHeight(
            isCompact = true,
        ) + bottomInset

        UIKitView(
            factory = {
                GlassTabBarView(
                    onSelect = { index ->
                        onSelect(index)
                    },
                )
            },
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            update = { view ->
                view.configure(
                    items = navItems,
                    enabled = identityState.isStable,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                )
            },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.Cooperative(),
                isNativeAccessibilityEnabled = false,
                placedAsOverlay = true,
            ),
        )
    }
}

private class GlassTabBarView(
    private val onSelect: (Int) -> Unit,
) : UIView(frame = CGRectZero.readValue()),
    UITabBarDelegateProtocol {

    private val tabBar = UITabBar()
    private var barItems: List<UITabBarItem> = emptyList()
    private var lastItemsSignature: String? = null
    private var lastAppearanceSignature: String? = null

    init {
        tabBar.delegate = this
        addSubview(tabBar)
    }

    fun configure(
        items: List<NavItem>,
        enabled: Boolean,
        selectedColor: Color,
        unselectedColor: Color,
    ) {
        tabBar.userInteractionEnabled = enabled

        // Tint the icons from the app theme; rebuild the appearance only when the colors change.
        val appearanceSignature = "${selectedColor.value}:${unselectedColor.value}"
        if (appearanceSignature != lastAppearanceSignature) {
            lastAppearanceSignature = appearanceSignature
            val itemAppearance = UITabBarItemAppearance().apply {
                normal.iconColor = unselectedColor.toUIColor()
                selected.iconColor = selectedColor.toUIColor()
            }
            val appearance = UITabBarAppearance().apply {
                configureWithDefaultBackground()
                stackedLayoutAppearance = itemAppearance
                inlineLayoutAppearance = itemAppearance
                compactInlineLayoutAppearance = itemAppearance
            }
            tabBar.standardAppearance = appearance
            tabBar.scrollEdgeAppearance = appearance
        }

        // Only rebuild the items when their identity/content changes; selection is synced every time.
        val signature = items.joinToString(separator = "|") { item ->
            "${item.stack.icon.name}:${item.badgeCount}"
        }
        if (signature != lastItemsSignature) {
            lastItemsSignature = signature
            barItems = items.mapIndexed { index, item ->
                // No title, to match the icon-only Material navigation bar.
                UITabBarItem(
                    title = null,
                    image = UIImage.imageNamed(item.stack.icon.heronIconAssetName()),
                    tag = index.toLong(),
                ).apply {
                    badgeValue = item.badgeCount.takeIf { it > 0L }?.toString()
                }
            }
            tabBar.setItems(barItems, animated = false)
        }
        tabBar.selectedItem = barItems.getOrNull(items.indexOfFirst { it.selected })
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        tabBar.setFrame(bounds)
    }

    override fun tabBar(
        tabBar: UITabBar,
        didSelectItem: UITabBarItem,
    ) {
        onSelect(didSelectItem.tag.toInt())
    }
}
