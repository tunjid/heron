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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSDirectionalEdgeInsetsMake
import platform.UIKit.NSLineBreakByClipping
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIControlContentHorizontalAlignmentLeading
import platform.UIKit.UIEvent
import platform.UIKit.UIImage
import platform.UIKit.UIView

@Composable
internal actual fun PlatformFab(
    modifier: Modifier,
    expanded: Boolean,
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    if (!isIos26OrLater()) {
        CommonFab(
            modifier = modifier,
            expanded = expanded,
            text = text,
            icon = icon,
            onClick = onClick,
        )
        return
    }
    val currentOnClick by rememberUpdatedState(onClick)
    val showText = icon == null || expanded
    val assetName = icon?.heronIconAssetName()

    var expandedWidth by remember {
        mutableStateOf(160.dp)
    }
    val contentWidth = if (showText) expandedWidth else FabHeightPoints.dp
    // Matches the spring PaneFab's animateBounds moves the fab with, so the leading edge travels
    // exactly as far as the width changes and the trailing edge stays pinned.
    val animatedWidth by animateDpAsState(
        targetValue = contentWidth,
        animationSpec = GlassFabWidthSpring,
        label = "GlassFabWidth",
    )

    val currentContentWidth by rememberUpdatedState(contentWidth)
    val isResizing by remember {
        derivedStateOf { animatedWidth != currentContentWidth }
    }

    UIKitView(
        factory = {
            GlassFabView(
                onTap = { currentOnClick() },
                onExpandedWidthMeasured = { points ->
                    expandedWidth = points.coerceAtLeast(FabHeightPoints).dp
                },
            )
        },
        modifier = modifier
            .approachLayout(
                isMeasurementApproachInProgress = { animatedWidth != contentWidth },
            ) { measurable, constraints ->
                val width = animatedWidth.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = width,
                        maxWidth = width,
                    ),
                )
                layout(
                    width = placeable.width,
                    height = placeable.height,
                ) {
                    placeable.place(
                        x = 0,
                        y = 0,
                    )
                }
            }
            .width(contentWidth),
        update = { view ->
            view.configure(
                title = if (showText || isResizing) text else null,
                assetName = assetName,
            )
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative(),
            isNativeAccessibilityEnabled = false,
            placedAsOverlay = true,
        ),
    )
}

private class GlassFabView(
    private val onTap: () -> Unit,
    private val onExpandedWidthMeasured: (Double) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {

    private val button = UIButton().apply {
        userInteractionEnabled = false
        // Anchor the icon to the leading edge so it doesn't drift to the middle of a pill that's
        // wider than its content mid animation.
        contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeading
    }

    private var lastTitle: String? = null
    private var lastAssetName: String? = null
    private var configured = false

    init {
        addSubview(button)
    }

    fun configure(
        title: String?,
        assetName: String?,
    ) {
        if (configured && title == lastTitle && assetName == lastAssetName) return
        lastTitle = title
        lastAssetName = assetName
        configured = true

        val image = assetName?.let { UIImage.imageNamed(it) }
        val configuration = UIButtonConfiguration.glassButtonConfiguration()
        configuration.image = image
        configuration.title = title
        // Space the icon from the label to match the Material fab's 8.dp spacer.
        configuration.imagePadding = 8.0
        // Keep the label on a single line and clip it, so it neither wraps nor ellipsizes while
        // the pill resizes.
        configuration.titleLineBreakMode = NSLineBreakByClipping
        if (image != null) {
            // Inset the icon so that it's centered in the collapsed, circular fab.
            val horizontalInset = image.size.useContents {
                ((FabHeightPoints - width) / 2).coerceAtLeast(0.0)
            }
            configuration.contentInsets = NSDirectionalEdgeInsetsMake(
                top = 0.0,
                leading = horizontalInset,
                bottom = 0.0,
                trailing = horizontalInset,
            )
        }
        button.configuration = configuration

        // The collapsed fab has a fixed width, only a labelled one needs measuring.
        if (title == null) return

        // Ask the configured button for its natural width and report it back to Compose.
        val fitting = button.sizeThatFits(
            CGSizeMake(
                width = 10_000.0,
                height = FabHeightPoints,
            ),
        )
        onExpandedWidthMeasured(
            fitting.useContents { width },
        )
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        button.setFrame(bounds)
    }

    override fun touchesEnded(
        touches: Set<*>,
        withEvent: UIEvent?,
    ) {
        super.touchesEnded(touches, withEvent)
        onTap()
    }
}

private const val FabHeightPoints = 56.0

private val GlassFabWidthSpring = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Dp.VisibilityThreshold,
)
