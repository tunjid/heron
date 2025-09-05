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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.MessageId
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileUri
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.core.types.Uri
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * A declaration for compile time known Model type that can be serialized as bytes and eencoded
 * in a url as a base 64 string
 * .
 * Currently does this using CBOR; Protobufs were evaluated but they're too terse.
 */
sealed interface UrlEncodableModel

inline fun <reified T : UrlEncodableModel> T.toBytes(): ByteArray =
    ModelSerializerFormat.encodeToByteArray(value = this)

inline fun <reified T : UrlEncodableModel> String.fromBase64EncodedUrl(): T =
    ModelSerializerFormat.decodeFromByteArray(ModelUrlSafeBase64.decode(this))

inline fun <reified T : UrlEncodableModel> T.toUrlEncodedBase64(): String =
    ModelUrlSafeBase64.encode(toBytes())

// This is brittle. Code gen should do this
val ModelSerializersModule = SerializersModule {
    polymorphic(UrlEncodableModel::class) {
        subclass(Profile::class)
        subclass(Post::class)
        subclass(FeedGenerator::class)
        subclass(FeedList::class)
        subclass(StarterPack::class)

        subclass(Post.Create::class)
        subclass(Post.Create.Reply::class)
        subclass(Post.Create.Mention::class)
        subclass(Post.Create.Quote::class)
        subclass(Post.Create.Timeline::class)

        subclass(Embed.Media::class)
        subclass(Video::class)
        subclass(ImageList::class)
    }
    polymorphic(Id::class) {
        subclass(PostId::class)
        subclass(ProfileId::class)
        subclass(ProfileHandle::class)
        subclass(ProfileHandleOrId::class)
        subclass(FeedGeneratorId::class)
        subclass(ListId::class)
        subclass(StarterPackId::class)
        subclass(GenericId::class)
        subclass(ConversationId::class)
        subclass(MessageId::class)
    }
    polymorphic(Id.Profile::class) {
        subclass(ProfileId::class)
        subclass(ProfileHandle::class)
        subclass(ProfileHandleOrId::class)
    }
    polymorphic(Uri::class) {
        subclass(PostUri::class)
        subclass(ProfileUri::class)
        subclass(FeedGeneratorUri::class)
        subclass(ListUri::class)
        subclass(StarterPackUri::class)
        subclass(ListMemberUri::class)
        subclass(ImageUri::class)
        subclass(GenericUri::class)
    }
    polymorphic(Embed::class) {
        subclass(ExternalEmbed::class)
        subclass(Video::class)
        subclass(ImageList::class)
        subclass(UnknownEmbed::class)
    }
    polymorphic(Embed.Media::class) {
        subclass(Video::class)
        subclass(ImageList::class)
    }
    polymorphic(LinkTarget::class) {
        subclass(LinkTarget.OfProfile::class)
        subclass(LinkTarget.UserHandleMention::class)
        subclass(LinkTarget.UserDidMention::class)
        subclass(LinkTarget.ExternalLink::class)
        subclass(LinkTarget.Hashtag::class)
    }
    polymorphic(LinkTarget.OfProfile::class) {
        subclass(LinkTarget.UserHandleMention::class)
        subclass(LinkTarget.UserDidMention::class)
    }
    polymorphic(Post.Create::class) {
        subclass(Post.Create.Reply::class)
        subclass(Post.Create.Mention::class)
        subclass(Post.Create.Quote::class)
        subclass(Post.Create.Timeline::class)
    }
    polymorphic(MediaFile::class) {
        subclass(MediaFile.Photo::class)
        subclass(MediaFile.Video::class)
    }
}

val ModelSerializerFormat: BinaryFormat = Cbor {
    ignoreUnknownKeys = true
    serializersModule = ModelSerializersModule
}

val ModelUrlSafeBase64 = Base64.UrlSafe.withPadding(
    option = PaddingOption.ABSENT,
)
