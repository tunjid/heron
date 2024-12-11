package com.tunjid.heron.feed.utilities

import com.tunjid.heron.data.core.models.Post

val Post.createdAt get() = record?.createdAt ?: indexedAt