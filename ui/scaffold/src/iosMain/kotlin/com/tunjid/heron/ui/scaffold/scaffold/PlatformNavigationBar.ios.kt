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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIImage
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarAppearance
import platform.UIKit.UITabBarDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.UIKit.UITabBarItemAppearance
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionAllowUserInteraction
import platform.UIKit.UIViewAnimationOptionBeginFromCurrentState
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve

@Composable
internal actual fun PaneScaffoldState.PlatformNavigationBar(
    modifier: Modifier,
    status: BottomNavStatus,
    onNavItemReselected: () -> Boolean,
) {
    if (!isIos26OrLater()) {
        CommonNavigationBar(
            modifier = modifier,
            status = status,
            onNavItemReselected = onNavItemReselected,
        )
        return
    }
    with(appScaffoldState.staticStates) {
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
        val collapse = when (status) {
            BottomNavStatus.Expanded -> GlassTabBarCollapse.None
            BottomNavStatus.Collapsed.Partially -> GlassTabBarCollapse.Partial
            BottomNavStatus.Collapsed.Fully -> GlassTabBarCollapse.Full
        }

        // The collapse the tab bar last finished animating to.
        var settledCollapse by remember { mutableStateOf(GlassTabBarCollapse.None) }
        var fullWidth by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        // The tab bar animates its own frame inside the interop view (see GlassTabBarView), so
        // the interop view only resizes in discrete steps: it widens as soon as the bar starts
        // expanding, and narrows once the bar has finished collapsing. Resizing it on every
        // frame would force a UITabBar relayout per frame, which stutters against the tab bar's
        // own animations. It can't just stay full width either: the interop view takes every
        // touch inside its frame, which would block the FAB beside a collapsed bar.
        val interopWidth = minOf(collapse, settledCollapse).width(fullWidth)

        Box(
            modifier = modifier
                .iosBottomNavSharedElement(
                    sharedContentState = rememberSharedContentState(NavigationBarSharedElementKey),
                )
                .fillMaxWidth()
                .height(height)
                .onSizeChanged { size ->
                    fullWidth = with(density) { size.width.toDp() }
                },
        ) {
            UIKitView(
                factory = {
                    GlassTabBarView(
                        onSelect = { index ->
                            onSelect(index)
                        },
                        onCollapseSettled = { settled ->
                            settledCollapse = settled
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(interopWidth),
                update = { view ->
                    view.configure(
                        items = navItems,
                        collapse = collapse,
                        fullWidth = fullWidth,
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
}

context(paneScaffoldState: PaneScaffoldState)
private fun Modifier.iosBottomNavSharedElement(
    sharedContentState: SharedTransitionScope.SharedContentState,
) = with(paneScaffoldState) {
    sharedElement(
        sharedContentState = sharedContentState,
        animatedVisibilityScope = this,
        zIndexInOverlay = UiTokens.navigationBarSharedElementZIndex,
    )
        // Prevent double rendering of shared elements as compose graphic layer and
        // shared element overlay semantics do not affect UI kit overlays.
        .visible(
            visible = isActive || !isTransitionActive,
        )
}

private class GlassTabBarView(
    private val onSelect: (Int) -> Unit,
    private val onCollapseSettled: (GlassTabBarCollapse) -> Unit,
) : UIView(frame = CGRectZero.readValue()),
    UITabBarDelegateProtocol {

    private val tabBar = UITabBar()
    private var barItems: List<UITabBarItem> = emptyList()
    private var lastItemsSignature: String? = null
    private var collapse = GlassTabBarCollapse.None
    private var fullWidth = 0.dp
    private var lastAppearanceSignature: String? = null

    init {
        tabBar.delegate = this
        addSubview(tabBar)
    }

    fun configure(
        items: List<NavItem>,
        collapse: GlassTabBarCollapse,
        fullWidth: Dp,
        enabled: Boolean,
        selectedColor: Color,
        unselectedColor: Color,
    ) {
        tabBar.userInteractionEnabled = enabled

        if (fullWidth != this.fullWidth) {
            this.fullWidth = fullWidth
            setNeedsLayout()
        }

        val collapseChanged = collapse != this.collapse
        this.collapse = collapse
        val showOnlySelected = collapse == GlassTabBarCollapse.Full

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
            // Fallback for item views mid-transition, before the appearance reaches them.
            tabBar.tintColor = selectedColor.toUIColor()
            tabBar.unselectedItemTintColor = unselectedColor.toUIColor()
            tabBar.standardAppearance = appearance
            tabBar.scrollEdgeAppearance = appearance
        }

        // Only rebuild the items when their identity/content changes; selection is synced every time.
        // When fully collapsed only the selected item is shown. Each bar item's tag keeps its
        // index in [items], so selection maps back correctly either way.
        val signature = items.joinToString(
            separator = "|",
            prefix = "$showOnlySelected|",
        ) { item ->
            "${item.stack.icon.name}:${item.badgeCount}:${showOnlySelected && item.selected}"
        }
        val itemsChanged = signature != lastItemsSignature
        if (itemsChanged) {
            lastItemsSignature = signature
            barItems = items.mapIndexedNotNull { index, item ->
                if (showOnlySelected && !item.selected) return@mapIndexedNotNull null
                // No title, to match the icon-only Material navigation bar.
                UITabBarItem(
                    title = null,
                    image = UIImage.imageNamed(item.stack.icon.heronIconAssetName()),
                    tag = index.toLong(),
                ).apply {
                    badgeValue = item.badgeCount.takeIf { it > 0L }?.toString()
                }
            }
        }
        val selectedIndex = items.indexOfFirst { it.selected }
        val applyItems = {
            // UITabBar's own animated setItems fades new item views in with the default tint
            // from a zero frame, so items are always set without it.
            if (itemsChanged) tabBar.setItems(barItems, animated = false)
            tabBar.selectedItem = barItems.firstOrNull { it.tag.toInt() == selectedIndex }
        }

        if (!collapseChanged) {
            applyItems()
            return
        }

        // Swap the items as a crossfade, laid out in the current frame, so the spring below
        // moves them from where they already are rather than from a zero frame.
        if (itemsChanged) transitionWithView(
            view = tabBar,
            duration = ItemCrossfadeDurationSeconds,
            options = UIViewAnimationOptionTransitionCrossDissolve or
                UIViewAnimationOptionAllowUserInteraction,
            animations = {
                applyItems()
                tabBar.layoutIfNeeded()
            },
            completion = null,
        )
        else applyItems()

        // Let Core Animation drive the collapse, so it runs at the display's refresh rate
        // independently of Compose frames.
        animateWithDuration(
            duration = CollapseAnimationDurationSeconds,
            delay = 0.0,
            usingSpringWithDamping = CollapseAnimationDampingRatio,
            initialSpringVelocity = 0.0,
            options = UIViewAnimationOptionBeginFromCurrentState or
                UIViewAnimationOptionAllowUserInteraction,
            animations = {
                layoutTabBar()
                // Lay out the tab bar's items inside the animation so they move with its frame.
                tabBar.layoutIfNeeded()
            },
            completion = { finished ->
                // An interrupted animation is followed by the one that interrupted it.
                if (finished && this.collapse == collapse) onCollapseSettled(collapse)
            },
        )
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        layoutTabBar()
    }

    // The tab bar is sized from the full width, not this view's bounds, since this view
    // narrows once the bar has collapsed. Points and dp are the same unit on iOS.
    private fun layoutTabBar() = bounds.useContents {
        val width = collapse.width(fullWidth).value.toDouble()
        // Collapse toward the leading edge.
        val isRtl = effectiveUserInterfaceLayoutDirection ==
            UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft
        tabBar.setFrame(
            CGRectMake(
                x = if (isRtl) size.width - width else 0.0,
                y = 0.0,
                width = width,
                height = size.height,
            ),
        )
    }

    override fun tabBar(
        tabBar: UITabBar,
        didSelectItem: UITabBarItem,
    ) {
        onSelect(didSelectItem.tag.toInt())
    }
}

// Ordered from widest to narrowest.
private enum class GlassTabBarCollapse {
    None,
    Partial,
    Full,
}

private fun GlassTabBarCollapse.width(fullWidth: Dp): Dp = when (this) {
    GlassTabBarCollapse.None -> fullWidth
    GlassTabBarCollapse.Partial -> fullWidth - PartiallyCollapsedTrailingInset
    GlassTabBarCollapse.Full -> FullyCollapsedWidth
}.coerceIn(
    minimumValue = 0.dp,
    maximumValue = fullWidth,
)

private const val CollapseAnimationDurationSeconds = 0.45
private const val CollapseAnimationDampingRatio = 0.85
private const val ItemCrossfadeDurationSeconds = 0.2

// Leaves room at the end of the bar for the collapsed FAB.
private val PartiallyCollapsedTrailingInset = DefaultFabSize + 16.dp

private val GlassTabBarHorizontalInset = 20.dp
private val GlassTabBarCapsuleHeight = 60.dp

// Sizes the tab bar's frame so the capsule holding the single selected item is a circle,
// matching the circular fully collapsed common navigation bar.
private val FullyCollapsedWidth = GlassTabBarCapsuleHeight + GlassTabBarHorizontalInset * 2
