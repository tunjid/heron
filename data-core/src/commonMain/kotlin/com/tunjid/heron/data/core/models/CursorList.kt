package com.tunjid.heron.data.core.models

data class CursorList<T>(
    val nextCursor: String,
    val items: List<T>,
): List<T> by items