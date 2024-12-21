package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.data.core.models.Post

val Post.createdAt get() = record?.createdAt ?: indexedAt