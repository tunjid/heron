/*
 *    Originally from ComposeMediaPlayer by Elie G.
 *    https://github.com/kdroidFilter/ComposeMediaPlayer
 *
 *    Copyright (c) 2025 Elie G.
 *    Licensed under the MIT License.
 *
 *    Adapted for Heron by Adetunji Dahunsi.
 */

package com.tunjid.heron.media.video.mac

import java.nio.ByteBuffer

internal fun calculateFrameHash(buffer: ByteBuffer, pixelCount: Int): Int {
    if (pixelCount <= 0) return 0

    var hash = 1
    val step = if (pixelCount <= 200) 1 else pixelCount / 200
    for (i in 0 until pixelCount step step) {
        hash = 31 * hash + buffer.getInt(i * 4)
    }
    return hash
}

internal fun copyBgraFrame(
    src: ByteBuffer,
    dst: ByteBuffer,
    width: Int,
    height: Int,
    dstRowBytes: Int,
) {
    require(width > 0) { "width must be > 0 (was $width)" }
    require(height > 0) { "height must be > 0 (was $height)" }
    val srcRowBytes = width * 4
    require(dstRowBytes >= srcRowBytes) {
        "dstRowBytes ($dstRowBytes) must be >= srcRowBytes ($srcRowBytes)"
    }

    val requiredSrcBytes = srcRowBytes.toLong() * height.toLong()
    val requiredDstBytes = dstRowBytes.toLong() * height.toLong()
    require(src.capacity().toLong() >= requiredSrcBytes) {
        "src buffer too small: ${src.capacity()} < $requiredSrcBytes"
    }
    require(dst.capacity().toLong() >= requiredDstBytes) {
        "dst buffer too small: ${dst.capacity()} < $requiredDstBytes"
    }

    val srcBuf = src.duplicate()
    val dstBuf = dst.duplicate()
    srcBuf.rewind()
    dstBuf.rewind()

    if (dstRowBytes == srcRowBytes) {
        srcBuf.limit(requiredSrcBytes.toInt())
        dstBuf.limit(requiredSrcBytes.toInt())
        dstBuf.put(srcBuf)
        return
    }

    val srcCapacity = srcBuf.capacity()
    val dstCapacity = dstBuf.capacity()
    for (row in 0 until height) {
        val srcPos = row * srcRowBytes
        srcBuf.limit(srcCapacity)
        srcBuf.position(srcPos)
        srcBuf.limit(srcPos + srcRowBytes)

        val dstPos = row * dstRowBytes
        dstBuf.limit(dstCapacity)
        dstBuf.position(dstPos)
        dstBuf.limit(dstPos + srcRowBytes)

        dstBuf.put(srcBuf)
    }
}
