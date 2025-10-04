package com.tunjid.heron.ui.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun insertMention(
    postText: TextFieldValue,
    selectedHandle: String,
): TextFieldValue {
    val text = postText.text
    val cursor = postText.selection.start

    val atIndex = text.lastIndexOf('@', cursor - 1)
    if (atIndex == -1) return postText

    val before = text.substring(0, atIndex)
    val after = text.substring(cursor)
    val newText = "$before@$selectedHandle $after"

    val newCursor = before.length + selectedHandle.length + 2

    return postText.copy(
        text = newText,
        selection = TextRange(newCursor),
    )
}
