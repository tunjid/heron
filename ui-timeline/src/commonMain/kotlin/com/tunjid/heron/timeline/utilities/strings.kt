package com.tunjid.heron.timeline.utilities

/**
 * Returns a mapping of byte offsets to character offsets.
 * Assumes that you are providing a valid UTF-8 string as input.
 * Text encodings are really a lot of fun.
 */
fun String.byteOffsets(): List<Int> = buildList {
    var i = 0
    var lastWas4Bytes = false

    while (i < length) {
        lastWas4Bytes = false
        val c = this@byteOffsets[i].code

        if (c < 0x80) {
            // A 7-bit character with 1 byte.
            repeat(1) { add(i) }
            i++
        } else if (c < 0x800) {
            // An 11-bit character with 2 bytes.
            repeat(2) { add(i) }
            i++
        } else if (c < 0xD800 || c > 0xDFFF) {
            // A 16-bit character with 3 bytes.
            repeat(3) { add(i) }
            i++
        } else {
            val low = if (i + 1 < length) this@byteOffsets[i + 1].code else 0

            if (c > 0xDBFF || low < 0xDC00 || low > 0xDFFF) {
                // A malformed surrogate, which yields '?'.
                repeat(1) { add(i) }
                i++
            } else {
                // A 21-bit character with 4 bytes.
                repeat(4) { add(i) }
                i += 2
                lastWas4Bytes = true
            }
        }
    }
    if (isNotEmpty()) {
        if (lastWas4Bytes) add(i - 1) else add(i)
    }
}
