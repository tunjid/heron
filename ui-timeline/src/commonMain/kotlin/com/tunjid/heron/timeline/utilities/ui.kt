package com.tunjid.heron.timeline.utilities

//fun Handle.color(): Color {
//  return Color(0xFF000000 or (hashCode().toLong() and 0x00FFFFFF))
//}

fun format(value: Long): String {
    return when (value) {
        in 10_000_000_000..Long.MAX_VALUE -> format(value, 1_000_000f, "B", wholeNumber = true)
        in 1_000_000_000..10_000_000_000 -> format(value, 1_000_000_000f, "B")
        in 10_000_000..1_000_000_000 -> format(value, 1_000_000f, "M", wholeNumber = true)
        in 1_000_000..10_000_000 -> format(value, 1_000_000f, "M")
        in 100_000..1_000_000 -> format(value, 1_000f, "K", wholeNumber = true)
        in 10_000..1_000_000 -> format(value, 1_000f, "K")
        else -> format(value, 1f, "", wholeNumber = true)
    }
}

private fun format(
    value: Long,
    div: Float,
    suffix: String,
    wholeNumber: Boolean = false,
): String {
    val msd = (value / div).toInt()
    val lsd = (value * 10 / div).toInt() % 10

    return if (msd >= 1000 && lsd == 0) {
        msd.formatDecimalSeparator()
    } else if (lsd == 0 || wholeNumber) {
        "$msd$suffix"
    } else {
        "$msd.$lsd$suffix"
    }
}

private fun Int.formatDecimalSeparator(): String {
    return toString()
        .reversed()
        .chunked(3)
        .joinToString(separator = ",")
        .reversed()
}