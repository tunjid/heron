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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSLineBreakByTruncatingTail
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
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

    // Width the native button measured for its current content (points map 1:1 to dp on iOS).
    var contentWidth by remember {
        mutableStateOf(if (showText) 160.dp else FabHeightPoints.dp)
    }
    val animatedWidth by animateDpAsState(
        targetValue = contentWidth,
        label = "GlassFabWidth",
    )

    UIKitView(
        factory = {
            GlassFabView(
                onTap = { currentOnClick() },
                onWidthMeasured = { points ->
                    contentWidth = points.coerceAtLeast(FabHeightPoints).dp
                },
            )
        },
        modifier = modifier.width(animatedWidth),
        update = { view ->
            view.configure(
                title = if (showText) text else null,
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
    private val onWidthMeasured: (Double) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {

    private val button = UIButton().apply {
        userInteractionEnabled = false
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

        val configuration = UIButtonConfiguration.glassButtonConfiguration()
        configuration.image = assetName?.let { UIImage.imageNamed(it) }
        configuration.title = title
        // Space the icon from the label to match the Material fab's 8.dp spacer.
        configuration.imagePadding = 8.0
        // Keep the label on a single line and ellipsize, so it never wraps while the pill grows.
        configuration.titleLineBreakMode = NSLineBreakByTruncatingTail
        button.configuration = configuration

        // Ask the configured button for its natural width and report it back to Compose.
        val fitting = button.sizeThatFits(
            CGSizeMake(
                width = 10_000.0,
                height = FabHeightPoints,
            ),
        )
        onWidthMeasured(
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
