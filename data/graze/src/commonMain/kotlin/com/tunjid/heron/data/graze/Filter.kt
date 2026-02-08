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

package com.tunjid.heron.data.graze

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Root interface for the Graze filter hierarchy.
 */
@Serializable
sealed interface Filter {
    val id: Id

    @JvmInline
    @Serializable
    value class Id
    @OptIn(ExperimentalUuidApi::class)
    internal constructor(
        val value: String = Uuid.random().toString(),
    )

// ==============================================================================
// 1. Comparator Hierarchy
// ==============================================================================

    @Serializable(with = ComparatorSerializer::class)
    sealed interface Comparator {
        val value: String

        @Serializable
        enum class Equality(override val value: String) : Comparator {
            @SerialName("==")
            Equal("=="),

            @SerialName("!=")
            NotEqual("!="),
        }

        @Serializable
        enum class Range(override val value: String) : Comparator {
            @SerialName(">")
            GreaterThan(">"),

            @SerialName("<")
            LessThan("<"),

            @SerialName(">=")
            GreaterThanOrEqual(">="),

            @SerialName("<=")
            LessThanOrEqual("<="),
        }

        @Serializable
        enum class Set(override val value: String) : Comparator {
            @SerialName("in")
            In("in"),

            @SerialName("not_in")
            NotIn("not_in"),
        }
    }

    /**
     * Custom serializer that uses the `value` property for both serialization and deserialization.
     * This ensures a single source of truth for the operator strings.
     */
    object ComparatorSerializer : KSerializer<Comparator> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Comparator", PrimitiveKind.STRING)

        // Pre-aggregated list of all possible comparators to avoid recreating it on every deserialize call
        private val allComparators: List<Comparator> =
            Comparator.Equality.entries + Comparator.Range.entries + Comparator.Set.entries

        override fun serialize(encoder: Encoder, value: Comparator) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): Comparator {
            val decoded = decoder.decodeString()
            return allComparators.find { it.value == decoded }
                ?: throw IllegalArgumentException("Unknown comparator: $decoded")
        }
    }

// ==============================================================================
// 2. Logic Containers
// ==============================================================================

    @Serializable
    sealed interface Root : Filter {
        val filters: List<Filter>
    }

    @Serializable
    @SerialName("and")
    data class And(
        override val id: Id = Id(),
        override val filters: List<Filter>,
    ) : Root

    @Serializable
    @SerialName("or")
    data class Or(
        override val id: Id = Id(),
        override val filters: List<Filter>,
    ) : Root

// ==============================================================================
// 3. Attributes
// ==============================================================================

    @Serializable
    sealed interface Attribute : Filter {

        @Serializable
        @SerialName("attribute_compare")
        data class Compare(
            override val id: Id = Id(),
            val selector: String,
            val operator: Comparator,
            val targetValue: String,
        ) : Attribute

        @Serializable
        @SerialName("embed_type")
        data class Embed(
            override val id: Id = Id(),
            val operator: Comparator.Equality,
            val embedType: Kind,
        ) : Attribute {
            @Serializable
            enum class Kind {
                @SerialName("image")
                Image,

                @SerialName("link")
                Link,

                @SerialName("post")
                Post,

                @SerialName("image_group")
                ImageGroup,

                @SerialName("video")
                Video,

                @SerialName("gif")
                Gif,
            }
        }
    }

// ==============================================================================
// 4. Entity
// ==============================================================================

    @Serializable
    sealed interface Entity : Filter {
        val entityType: Type
        val values: List<String>

        @Serializable
        @SerialName("entity_matches")
        data class Matches(
            override val id: Id = Id(),
            override val entityType: Type,
            override val values: List<String>,
        ) : Entity

        @Serializable
        @SerialName("entity_excludes")
        data class Excludes(
            override val id: Id = Id(),
            override val entityType: Type,
            override val values: List<String>,
        ) : Entity

        enum class Type {
            @SerialName("hashtags")
            Hashtags,

            @SerialName("langs")
            Languages,

            @SerialName("urls")
            Urls,

            @SerialName("mentions")
            Mentions,

            @SerialName("domains")
            Domains,
        }
    }

// ==============================================================================
// 5. Regex
// ==============================================================================

    @Serializable
    sealed interface Regex : Filter {

        @Serializable
        @SerialName("regex_matches")
        data class Matches(
            override val id: Id = Id(),
            val variable: String,
            val pattern: String,
            val isCaseInsensitive: Boolean,
        ) : Regex

        @Serializable
        @SerialName("regex_negation_matches")
        data class Negation(
            override val id: Id = Id(),
            val variable: String,
            val pattern: String,
            val isCaseInsensitive: Boolean,
        ) : Regex

        @Serializable
        @SerialName("regex_any")
        data class Any(
            override val id: Id = Id(),
            val variable: String,
            val terms: List<String>,
            val isCaseInsensitive: Boolean,
        ) : Regex

        @Serializable
        @SerialName("regex_none")
        data class None(
            override val id: Id = Id(),
            val variable: String,
            val terms: List<String>,
            val isCaseInsensitive: Boolean,
        ) : Regex
    }

// ==============================================================================
// 6. Social
// ==============================================================================

    @Serializable
    sealed interface Social : Filter {

        @Serializable
        @SerialName("social_graph")
        data class Graph(
            override val id: Id = Id(),
            val username: String,
            val operator: Comparator.Set,
            val direction: String,
        ) : Social

        @Serializable
        @SerialName("social_list")
        data class UserList(
            override val id: Id = Id(),
            val dids: List<String>,
            val operator: Comparator.Set,
        ) : Social

        @Serializable
        @SerialName("starter_pack_member")
        data class StarterPack(
            override val id: Id = Id(),
            val url: String,
            val operator: Comparator.Set,
        ) : Social

        @Serializable
        @SerialName("list_member")
        data class ListMember(
            override val id: Id = Id(),
            val url: String,
            val operator: Comparator.Set,
        ) : Social

        @Serializable
        @SerialName("magic_audience")
        data class MagicAudience(
            override val id: Id = Id(),
            val audienceId: String,
            val operator: Comparator.Set,
        ) : Social
    }

// ==============================================================================
// 7. ML (Machine Learning)
// ==============================================================================

    @Serializable
    sealed interface ML : Filter {
        val threshold: Double

        @Serializable
        @SerialName("text_similarity")
        data class Similarity(
            override val id: Id = Id(),
            val path: String,
            val config: Config,
            val operator: Comparator.Range,
            override val threshold: Double,
        ) : ML {
            @Serializable
            data class Config(
                @SerialName("anchor_text")
                val anchorText: String,
                @SerialName("model_name")
                val modelName: String,
            )
        }

        @Serializable
        @SerialName("model_probability")
        data class Probability(
            override val id: Id = Id(),
            val config: Config,
            val operator: Comparator.Range,
            override val threshold: Double,
        ) : ML {
            @Serializable
            data class Config(
                @SerialName("model_name")
                val modelName: String,
            )
        }

        @Serializable
        @SerialName("content_moderation")
        data class Moderation(
            override val id: Id = Id(),
            val category: String,
            val operator: Comparator.Range,
            override val threshold: Double,
        ) : ML
    }

// ==============================================================================
// 8. Analysis
// ==============================================================================

    @Serializable
    sealed interface Analysis : Filter {
        val category: String
        val operator: Comparator.Range
        val threshold: Double

        @Serializable
        @SerialName("language_analysis")
        data class Language(
            override val id: Id = Id(),
            @SerialName("language_name")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("sentiment_analysis")
        data class Sentiment(
            override val id: Id = Id(),
            @SerialName("sentiment_category")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("financial_sentiment_analysis")
        data class FinancialSentiment(
            override val id: Id = Id(),
            @SerialName("financial_category")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("emotion_sentiment_analysis")
        data class Emotion(
            override val id: Id = Id(),
            @SerialName("emotion_category")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("toxicity_analysis")
        data class Toxicity(
            override val id: Id = Id(),
            @SerialName("toxic_category")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("topic_analysis")
        data class Topic(
            override val id: Id = Id(),
            @SerialName("topic_label")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("text_arbitrary")
        data class TextArbitrary(
            override val id: Id = Id(),
            @SerialName("tag")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("image_nsfw")
        data class ImageNsfw(
            override val id: Id = Id(),
            @SerialName("tag")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis

        @Serializable
        @SerialName("image_arbitrary")
        data class ImageArbitrary(
            override val id: Id = Id(),
            @SerialName("tag")
            override val category: String,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis
    }
}

val Filter.Attribute.Embed.Kind.isGalleryMedia
    get() = when (this) {
        Filter.Attribute.Embed.Kind.Image,
        Filter.Attribute.Embed.Kind.ImageGroup,
        Filter.Attribute.Embed.Kind.Video,
        -> true
        Filter.Attribute.Embed.Kind.Link,
        Filter.Attribute.Embed.Kind.Post,
        Filter.Attribute.Embed.Kind.Gif,
        -> false
    }
