package com.tunjid.heron.data.core.models

data class CursorList<T>(
    val items: List<T>,
    val nextCursor: String?,
): List<T> by items