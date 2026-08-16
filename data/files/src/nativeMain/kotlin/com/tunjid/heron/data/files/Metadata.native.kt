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

@file:OptIn(ExperimentalForeignApi::class)

package com.tunjid.heron.data.files

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFNumberIntType
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSURL
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.kCGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth

internal actual fun PlatformFile.imageDimensionsOrNull(): Dimensions? {
    val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) ?: return null
    val imageSource = CGImageSourceCreateWithURL(url.reinterpret(), null)
    val properties = imageSource?.let {
        CGImageSourceCopyPropertiesAtIndex(it, PrimaryImageIndex, null)
    }

    try {
        if (properties == null) return null
        val width = properties.intValue(kCGImagePropertyPixelWidth) ?: return null
        val height = properties.intValue(kCGImagePropertyPixelHeight) ?: return null

        // ImageIO reports the dimensions the pixels are stored at alongside the orientation
        // they should be displayed with, applying neither. This also covers HEIC, which is
        // what the photo library hands back on an iPhone.
        return Dimensions(
            width = width,
            height = height,
        )
            .orientedBy(properties.intValue(kCGImagePropertyOrientation))
    } finally {
        properties?.let(::CFRelease)
        imageSource?.let(::CFRelease)
        CFBridgingRelease(url)
    }
}

private fun CFDictionaryRef.intValue(
    key: CFStringRef?,
): Int? = memScoped {
    val number = CFDictionaryGetValue(this@intValue, key) ?: return@memScoped null
    val value = alloc<IntVar>()
    if (!CFNumberGetValue(number.reinterpret(), kCFNumberIntType, value.ptr)) return@memScoped null
    value.value
}

private const val PrimaryImageIndex = 0uL
