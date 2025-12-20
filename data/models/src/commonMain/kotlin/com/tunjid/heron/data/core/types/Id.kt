/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.core.types

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
sealed interface Id {
    val id: String

    @Serializable
    sealed interface Profile : Id
}

@Serializable
@JvmInline
value class PostId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ProfileId(
    override val id: String,
) : Id.Profile {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ProfileHandle(
    override val id: String,
) : Id.Profile {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ProfileHandleOrId(
    override val id: String,
) : Id.Profile {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class FeedGeneratorId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ListId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class StarterPackId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class LabelerId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ThreadGateId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class GenericId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class ConversationId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}

@Serializable
@JvmInline
value class MessageId(
    override val id: String,
) : Id {
    override fun toString(): String = id
}
