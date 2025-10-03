package com.tunjid.heron.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.ui.text.links

fun detectActiveLink(
    annotated: AnnotatedString,
    selection: TextRange,
): LinkTarget? {
    val links = annotated.links()
    val cursor = selection.start
    return links.firstOrNull { cursor in it.start..it.end }?.target
}
