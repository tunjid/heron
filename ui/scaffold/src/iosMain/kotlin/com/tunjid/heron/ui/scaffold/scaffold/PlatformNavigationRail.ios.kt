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

@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class, BetaInteropApi::class)

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.tunjid.composables.constrainedsize.constrainedSizePlacement
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.identity.isStable
import com.tunjid.heron.ui.scaffold.navigation.NavItem
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UICornerConfiguration
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImageRenderingMode
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionAllowUserInteraction
import platform.UIKit.UIViewAnimationOptionBeginFromCurrentState
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIVisualEffectView
import platform.UIKit.systemRedColor
import platform.objc.sel_registerName

@Composable
internal actual fun PaneScaffoldState.PlatformNavigationRail(
    modifier: Modifier,
    onNavItemReselected: () -> Boolean,
) {
    if (!isIos26OrLater()) {
        CommonNavigationRail(
            modifier = modifier,
            onNavItemReselected = onNavItemReselected,
        )
        return
    }
    with(appScaffoldState.staticStates) {
        val onSelect by rememberUpdatedState<(Int) -> Unit> select@{ index ->
            val item = navItems.getOrNull(index) ?: return@select
            if (item.selected && onNavItemReselected()) return@select
            onNavItemSelected(item)
        }
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
        val railWidth = minOf(GlassRailWidth, navigationRailWidth)
        val railHeight = GlassRailItemExtent * navItems.size + GlassRailVerticalPadding * 2

        Box(
            modifier = modifier
                .widthIn(max = navigationRailWidth)
                .fillMaxSize(),
        ) {
            if (navItems.isNotEmpty()) UIKitView(
                factory = {
                    GlassNavRailView(
                        onSelect = { index ->
                            onSelect(index)
                        },
                    )
                },
                modifier = Modifier
                    .navigationRailPosition(
                        bottomAligned = bottomAlignsNavigationRail,
                    )
                    // Prevent double rendering during shared element transitions, as UIKit
                    // overlays are unaffected by the shared element overlay.
                    .visible(
                        visible = isActive || !isTransitionActive,
                    )
                    .constrainedSizePlacement(
                        orientation = Orientation.Horizontal,
                        minSize = railWidth,
                        atStart = navRailPlacesAtStart(
                            layoutDirection = LocalLayoutDirection.current,
                        ),
                    )
                    .size(
                        width = railWidth,
                        height = railHeight,
                    ),
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
}

private class GlassNavRailView(
    private val onSelect: (Int) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {

    private val glassView = UIVisualEffectView(
        effect = UIGlassEffect.effectWithStyle(
            style = UIGlassEffectStyle.UIGlassEffectStyleRegular,
        ).apply {
            interactive = true
        },
    ).apply {
        cornerConfiguration = UICornerConfiguration.capsuleConfiguration()
    }

    private val selectionPlatter = UIView().apply {
        userInteractionEnabled = false
    }

    private var itemViews: List<GlassNavRailItemView> = emptyList()
    private var lastItemsSignature: String? = null
    private var lastPlatterColor: Color? = null
    private var selectedIndex = -1

    init {
        addSubview(glassView)
        glassView.contentView.addSubview(selectionPlatter)
        // A recognizer rather than touch overrides, so the interactive glass still sees the touches.
        addGestureRecognizer(
            UITapGestureRecognizer(
                target = this,
                action = sel_registerName("onTap:"),
            ).apply {
                cancelsTouchesInView = false
            },
        )
    }

    fun configure(
        items: List<NavItem>,
        enabled: Boolean,
        selectedColor: Color,
        unselectedColor: Color,
    ) {
        userInteractionEnabled = enabled

        val platterColor = unselectedColor.copy(alpha = SelectionPlatterAlpha)
        if (platterColor != lastPlatterColor) {
            lastPlatterColor = platterColor
            selectionPlatter.backgroundColor = platterColor.toUIColor()
        }

        // Only rebuild the item views when their identity changes; badges and tints are synced
        // every time.
        val signature = items.joinToString(separator = "|") { item ->
            item.stack.icon.name
        }
        if (signature != lastItemsSignature) {
            lastItemsSignature = signature
            itemViews.forEach(UIView::removeFromSuperview)
            itemViews = items.map { item ->
                GlassNavRailItemView(
                    assetName = item.stack.icon.heronIconAssetName(),
                ).also(glassView.contentView::addSubview)
            }
            setNeedsLayout()
        }
        items.forEachIndexed { index, item ->
            itemViews[index].configure(
                badgeCount = item.badgeCount,
                tint = if (item.selected) selectedColor else unselectedColor,
            )
        }

        val newSelectedIndex = items.indexOfFirst(NavItem::selected)
        if (newSelectedIndex == selectedIndex) return
        val hadSelection = selectedIndex >= 0 && window != null
        selectedIndex = newSelectedIndex
        if (!hadSelection) {
            layoutSelectionPlatter()
            return
        }
        // Let Core Animation slide the platter, so it runs at the display's refresh rate
        // independently of Compose frames.
        animateWithDuration(
            duration = SelectionAnimationDurationSeconds,
            delay = 0.0,
            usingSpringWithDamping = SelectionAnimationDampingRatio,
            initialSpringVelocity = 0.0,
            options = UIViewAnimationOptionBeginFromCurrentState or
                UIViewAnimationOptionAllowUserInteraction,
            animations = ::layoutSelectionPlatter,
            completion = null,
        )
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        glassView.setFrame(bounds)
        itemViews.forEachIndexed { index, itemView ->
            itemView.setFrame(itemFrame(index))
        }
        layoutSelectionPlatter()
    }

    private fun layoutSelectionPlatter() {
        selectionPlatter.hidden = selectedIndex < 0
        if (selectedIndex < 0) return
        val frame = itemFrame(selectedIndex)
        selectionPlatter.setFrame(frame)
        selectionPlatter.layer.cornerRadius = frame.useContents { minOf(size.width, size.height) / 2 }
    }

    // Each item is a square cell centered across the capsule, so the selection platter on the first
    // and last items is concentric with the capsule's ends.
    private fun itemFrame(index: Int) = bounds.useContents {
        val extent = GlassRailItemExtent.value.toDouble()
        CGRectMake(
            x = (size.width - extent) / 2,
            y = GlassRailVerticalPadding.value + extent * index,
            width = extent,
            height = extent,
        )
    }

    @ObjCAction
    fun onTap(recognizer: UITapGestureRecognizer) {
        val y = recognizer.locationInView(this).useContents { y }
        val index = ((y - GlassRailVerticalPadding.value) / GlassRailItemExtent.value).toInt()
        if (index in itemViews.indices) onSelect(index)
    }
}

private class GlassNavRailItemView(
    assetName: String,
) : UIView(frame = CGRectZero.readValue()) {

    private val iconView = UIImageView(
        image = UIImage.imageNamed(assetName)
            ?.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate),
    ).apply {
        contentMode = UIViewContentMode.UIViewContentModeCenter
    }

    private val badgeLabel = UILabel().apply {
        backgroundColor = UIColor.systemRedColor
        textColor = UIColor.whiteColor
        font = UIFont.systemFontOfSize(
            fontSize = BadgeFontSize,
            weight = UIFontWeightSemibold,
        )
        textAlignment = NSTextAlignmentCenter
        clipsToBounds = true
        hidden = true
    }

    private var badgeCount = 0L

    init {
        userInteractionEnabled = false
        addSubview(iconView)
        addSubview(badgeLabel)
    }

    fun configure(
        badgeCount: Long,
        tint: Color,
    ) {
        iconView.tintColor = tint.toUIColor()
        if (badgeCount == this.badgeCount) return
        this.badgeCount = badgeCount
        // Mirrors the Material Badge: a count below MaxBadgeCount, a dot at or above it.
        badgeLabel.hidden = badgeCount <= 0L
        badgeLabel.text = badgeCount.takeIf { it in 1..<MaxBadgeCount }?.toString()
        setNeedsLayout()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        iconView.setFrame(bounds)
        bounds.useContents {
            val showsCount = badgeLabel.text != null
            val height = if (showsCount) BadgeHeight else BadgeDotSize
            val width = if (showsCount) {
                maxOf(
                    height,
                    badgeLabel.intrinsicContentSize.useContents { width } + BadgeHorizontalPadding * 2,
                )
            } else {
                height
            }
            // Anchor the badge's leading edge to the top trailing corner of the icon.
            badgeLabel.setFrame(
                CGRectMake(
                    x = size.width / 2 + BadgeIconOffset,
                    y = size.height / 2 - BadgeIconOffset - height,
                    width = width,
                    height = height,
                ),
            )
            badgeLabel.layer.cornerRadius = height / 2
        }
    }
}

private const val SelectionAnimationDurationSeconds = 0.45
private const val SelectionAnimationDampingRatio = 0.85
private const val SelectionPlatterAlpha = 0.14f

private const val MaxBadgeCount = 100L
private const val BadgeFontSize = 11.0
private const val BadgeHeight = 16.0
private const val BadgeDotSize = 8.0
private const val BadgeHorizontalPadding = 4.0
private const val BadgeIconOffset = 6.0

// As thick as the bottom tab bar's glass capsule.
private val GlassRailWidth = UiTokens.bottomNavHeight(
    isCompact = true,
)

// The bottom tab bar's selection platter is inset 4pt from its 62pt capsule.
private val GlassRailItemExtent = GlassRailWidth - 8.dp
private val GlassRailVerticalPadding = 4.dp
