package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id

object Constants {
    val timelineFeedId = Id("at://self")
    val blockedPostId = Id("at://blocked")
    val notFoundPostId = Id("at://not_found")
    val unknownPostId = Id("at://unknown")
}