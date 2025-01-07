package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.models.Post as CorePost
import com.tunjid.heron.data.core.models.Profile as CoreProfile

sealed class SearchResult {

    data class Profile(
        val profile: CoreProfile,
        val relationship: ProfileRelationship,
    ) : SearchResult()

    sealed class Post : SearchResult() {
        abstract val post: CorePost

        data class Top(
            override val post: CorePost,
        ) : Post()

        data class Latest(
            override val post: CorePost,
        ) : Post()
    }
}