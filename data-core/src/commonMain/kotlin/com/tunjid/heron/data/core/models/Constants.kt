package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri

object Constants {
    val timelineFeed = Uri("at://self")
    val blockedPostId = Id("at://blocked")
    val notFoundPostId = Id("at://not_found")
    val unknownPostId = Id("at://unknown")
}