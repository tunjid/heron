package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.models.Profile as CoreProfile
import com.tunjid.heron.data.core.models.Post as CorePost

sealed class SearchResult {

    data class Profile(
        val profile: CoreProfile,
        val relationship: ProfileRelationship,
    ): SearchResult()

    data class Post(
        val post: CorePost
    ): SearchResult()
}