package com.tunjid.heron.ui

import androidx.compose.ui.text.TextRange
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget

fun List<Link>.detectActiveLink(selection: TextRange): LinkTarget? {
    val cursor = selection.start
    return this.firstOrNull { cursor in it.start..it.end }?.target
}
