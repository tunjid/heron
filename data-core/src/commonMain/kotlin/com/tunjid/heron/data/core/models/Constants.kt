package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri

object Constants {
    const val UNKNOWN = "at://unknown"

    val timelineFeed = Uri("at://self")
    val blockedPostId = Id("at://blocked")
    val notFoundPostId = Id("at://not_found")
    val unknownPostId = Id(UNKNOWN)
    val unknownPostUri = Uri(UNKNOWN)
    val unknownAuthorId = Id(UNKNOWN)
}