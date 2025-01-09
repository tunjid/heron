package com.tunjid.heron.data.core.models

import kotlinx.serialization.Serializable

sealed interface Embed {
    sealed interface Media: Embed
}

@Serializable
data object UnknownEmbed : Embed