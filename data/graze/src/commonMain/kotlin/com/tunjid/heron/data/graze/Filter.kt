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
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

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

    @Serializable(with = RootSerializer::class)
    sealed interface Root : Filter {
        val filters: List<Filter>
    }

    @Serializable
    @SerialName("and")
    data class And(
        override val id: Id = Id(),
        override val filters: List<Filter>,
    ) : Root {
        companion object {
            fun empty() = And(
                filters = emptyList(),
            )
        }
    }

    @Serializable
    @SerialName("or")
    data class Or(
        override val id: Id = Id(),
        override val filters: List<Filter>,
    ) : Root {
        companion object {
            fun empty() = Or(
                filters = emptyList(),
            )
        }
    }

    object RootSerializer : KSerializer<Root> {
        private val delegate = PolymorphicSerializer(Root::class)
        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun serialize(encoder: Encoder, value: Root) {
            if (encoder is JsonEncoder) {
                val json = encoder.json
                val filtersElement = json.encodeToJsonElement(
                    ListSerializer(Filter.serializer()),
                    value.filters,
                )
                val jsonObject = buildJsonObject {
                    when (value) {
                        is And -> put("and", filtersElement)
                        is Or -> put("or", filtersElement)
                    }
                }
                encoder.encodeJsonElement(jsonObject)
            } else {
                delegate.serialize(encoder, value)
            }
        }

        override fun deserialize(decoder: Decoder): Root {
            if (decoder is JsonDecoder) {
                val jsonObject = decoder.decodeJsonElement().jsonObject

                return when {
                    "and" in jsonObject -> {
                        val filters = decoder.json.decodeFromJsonElement(
                            ListSerializer(Filter.serializer()),
                            jsonObject["and"]!!,
                        )
                        And(filters = filters)
                    }
                    "or" in jsonObject -> {
                        val filters = decoder.json.decodeFromJsonElement(
                            ListSerializer(Filter.serializer()),
                            jsonObject["or"]!!,
                        )
                        Or(filters = filters)
                    }
                    else -> throw SerializationException("Expected 'and' or 'or' key for Filter.Root")
                }
            } else {
                return delegate.deserialize(decoder)
            }
        }
    }

// ==============================================================================
// 3. Attributes
// ==============================================================================

    @Serializable
    sealed interface Attribute : Filter {

        @Serializable
        @SerialName("attribute_compare")
        data class Compare(
            override val id: Id = Id(),
            val selector: Selector,
            val operator: Comparator,
            val targetValue: String,
        ) : Attribute {
            @Serializable
            @JvmInline
            value class Selector(val value: String) {
                companion object {
                    val Text = Selector("text")
                    val Reply = Selector("reply")
                    val Embed = Selector("embed")
                    val UserHandle = Selector("hydrated_metadata.user.handle")
                    val MentionHandle = Selector("hydrated_metadata.mentions[*].handle")
                    val QuoteAuthorHandle = Selector("hydrated_metadata.quote_post.author.handle")

                    val entries = listOf(
                        Text,
                        Reply,
                        Embed,
                        UserHandle,
                        MentionHandle,
                        QuoteAuthorHandle,
                    )
                }
            }

            companion object {
                fun empty() = Compare(
                    selector = Selector.Text,
                    operator = Comparator.Equality.Equal,
                    targetValue = "",
                )
            }
        }

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

            companion object {
                fun empty() = Embed(
                    operator = Comparator.Equality.Equal,
                    embedType = Kind.Image,
                )
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
        ) : Entity {
            companion object {
                fun empty() = Matches(
                    entityType = Type.Hashtags,
                    values = emptyList(),
                )
            }
        }

        @Serializable
        @SerialName("entity_excludes")
        data class Excludes(
            override val id: Id = Id(),
            override val entityType: Type,
            override val values: List<String>,
        ) : Entity {
            companion object {
                fun empty() = Excludes(
                    entityType = Type.Hashtags,
                    values = emptyList(),
                )
            }
        }

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
        ) : Regex {
            companion object {
                fun empty() = Matches(
                    variable = "",
                    pattern = "",
                    isCaseInsensitive = false,
                )
            }
        }

        @Serializable
        @SerialName("regex_negation_matches")
        data class Negation(
            override val id: Id = Id(),
            val variable: String,
            val pattern: String,
            val isCaseInsensitive: Boolean,
        ) : Regex {
            companion object {
                fun empty() = Negation(
                    variable = "",
                    pattern = "",
                    isCaseInsensitive = false,
                )
            }
        }

        @Serializable
        @SerialName("regex_any")
        data class Any(
            override val id: Id = Id(),
            val variable: String,
            val terms: List<String>,
            val isCaseInsensitive: Boolean,
        ) : Regex {
            companion object {
                fun empty() = Any(
                    variable = "",
                    terms = emptyList(),
                    isCaseInsensitive = false,
                )
            }
        }

        @Serializable
        @SerialName("regex_none")
        data class None(
            override val id: Id = Id(),
            val variable: String,
            val terms: List<String>,
            val isCaseInsensitive: Boolean,
        ) : Regex {
            companion object {
                fun empty() = None(
                    variable = "",
                    terms = emptyList(),
                    isCaseInsensitive = false,
                )
            }
        }
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
            val direction: Direction,
        ) : Social {
            @Serializable
            @JvmInline
            value class Direction(val value: String) {
                companion object {
                    val Following = Direction("follows")
                    val Followers = Direction("followers")

                    val entries = listOf(
                        Following,
                        Followers,
                    )
                }
            }

            companion object {
                fun empty() = Graph(
                    username = "",
                    operator = Comparator.Set.In,
                    direction = Direction.Following,
                )
            }
        }

        @Serializable
        @SerialName("social_list")
        data class UserList(
            override val id: Id = Id(),
            val dids: List<String>,
            val operator: Comparator.Set,
        ) : Social {
            companion object {
                fun empty() = UserList(
                    dids = emptyList(),
                    operator = Comparator.Set.In,
                )
            }
        }

        @Serializable
        @SerialName("starter_pack_member")
        data class StarterPack(
            override val id: Id = Id(),
            val url: String,
            val operator: Comparator.Set,
        ) : Social {
            companion object {
                fun empty() = StarterPack(
                    url = "",
                    operator = Comparator.Set.In,
                )
            }
        }

        @Serializable
        @SerialName("list_member")
        data class ListMember(
            override val id: Id = Id(),
            val url: String,
            val operator: Comparator.Set,
        ) : Social {
            companion object {
                fun empty() = ListMember(
                    url = "",
                    operator = Comparator.Set.In,
                )
            }
        }

        @Serializable
        @SerialName("magic_audience")
        data class MagicAudience(
            override val id: Id = Id(),
            val audienceId: String,
            val operator: Comparator.Set,
        ) : Social {
            companion object {
                fun empty() = MagicAudience(
                    audienceId = "",
                    operator = Comparator.Set.In,
                )
            }
        }
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

            companion object {
                fun empty() = Similarity(
                    path = "",
                    config = Config(
                        anchorText = "",
                        modelName = "",
                    ),
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
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

            companion object {
                fun empty() = Probability(
                    config = Config(
                        modelName = "",
                    ),
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("content_moderation")
        data class Moderation(
            override val id: Id = Id(),
            val category: Category,
            val operator: Comparator.Range,
            override val threshold: Double,
        ) : ML {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Sexual = Category("sexual")
                    val Hate = Category("hate")
                    val Violence = Category("violence")
                    val Harassment = Category("harassment")
                    val SelfHarm = Category("self-harm")
                    val SexualMinors = Category("sexual/minors")
                    val HateThreatening = Category("hate/threatening")
                    val ViolenceGraphic = Category("violence/graphic")
                    val OK = Category("OK")

                    val entries = listOf(
                        Sexual,
                        Hate,
                        Violence,
                        Harassment,
                        SelfHarm,
                        SexualMinors,
                        HateThreatening,
                        ViolenceGraphic,
                        OK,
                    )
                }
            }

            companion object {
                fun empty() = Moderation(
                    category = Category.Sexual,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }
    }

// ==============================================================================
// 8. Analysis
// ==============================================================================

    @Serializable
    sealed interface Analysis : Filter {
        val operator: Comparator.Range
        val threshold: Double

        @Serializable
        @SerialName("language_analysis")
        data class Language(
            override val id: Id = Id(),
            @SerialName("language_name")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Japanese = Category("Japanese")
                    val Dutch = Category("Dutch")
                    val Arabic = Category("Arabic")
                    val Polish = Category("Polish")
                    val German = Category("German")
                    val Italian = Category("Italian")
                    val Portuguese = Category("Portuguese")
                    val Turkish = Category("Turkish")
                    val Spanish = Category("Spanish")
                    val Hindi = Category("Hindi")
                    val Greek = Category("Greek")
                    val Urdu = Category("Urdu")
                    val Bulgarian = Category("Bulgarian")
                    val English = Category("English")
                    val French = Category("French")
                    val Chinese = Category("Chinese")
                    val Russian = Category("Russian")
                    val Thai = Category("Thai")
                    val Swahili = Category("Swahili")
                    val Vietnamese = Category("Vietnamese")

                    val entries = listOf(
                        Japanese,
                        Dutch,
                        Arabic,
                        Polish,
                        German,
                        Italian,
                        Portuguese,
                        Turkish,
                        Spanish,
                        Hindi,
                        Greek,
                        Urdu,
                        Bulgarian,
                        English,
                        French,
                        Chinese,
                        Russian,
                        Thai,
                        Swahili,
                        Vietnamese,
                    )
                }
            }

            companion object {
                fun empty() = Language(
                    category = Category.English,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("sentiment_analysis")
        data class Sentiment(
            override val id: Id = Id(),
            @SerialName("sentiment_category")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Positive = Category("Positive")
                    val Negative = Category("Negative")
                    val Neutral = Category("Neutral")

                    val entries = listOf(
                        Positive,
                        Negative,
                        Neutral,
                    )
                }
            }

            companion object {
                fun empty() = Sentiment(
                    category = Category.Neutral,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("financial_sentiment_analysis")
        data class FinancialSentiment(
            override val id: Id = Id(),
            @SerialName("financial_category")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Positive = Category("Positive")
                    val Negative = Category("Negative")
                    val Neutral = Category("Neutral")

                    val entries = listOf(
                        Positive,
                        Negative,
                        Neutral,
                    )
                }
            }

            companion object {
                fun empty() = FinancialSentiment(
                    category = Category.Neutral,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("emotion_sentiment_analysis")
        data class Emotion(
            override val id: Id = Id(),
            @SerialName("emotion_category")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Admiration = Category("Admiration")
                    val Amusement = Category("Amusement")
                    val Anger = Category("Anger")
                    val Annoyance = Category("Annoyance")
                    val Approval = Category("Approval")
                    val Caring = Category("Caring")
                    val Confusion = Category("Confusion")
                    val Curiosity = Category("Curiosity")
                    val Desire = Category("Desire")
                    val Disappointment = Category("Disappointment")
                    val Disapproval = Category("Disapproval")
                    val Disgust = Category("Disgust")
                    val Embarrassment = Category("Embarrassment")
                    val Excitement = Category("Excitement")
                    val Fear = Category("Fear")
                    val Gratitude = Category("Gratitude")
                    val Grief = Category("Grief")
                    val Joy = Category("Joy")
                    val Love = Category("Love")
                    val Nervousness = Category("Nervousness")
                    val Optimism = Category("Optimism")
                    val Pride = Category("Pride")
                    val Realization = Category("Realization")
                    val Relief = Category("Relief")
                    val Remorse = Category("Remorse")
                    val Sadness = Category("Sadness")
                    val Surprise = Category("Surprise")
                    val Neutral = Category("Neutral")

                    val entries = listOf(
                        Admiration,
                        Amusement,
                        Anger,
                        Annoyance,
                        Approval,
                        Caring,
                        Confusion,
                        Curiosity,
                        Desire,
                        Disappointment,
                        Disapproval,
                        Disgust,
                        Embarrassment,
                        Excitement,
                        Fear,
                        Gratitude,
                        Grief,
                        Joy,
                        Love,
                        Nervousness,
                        Optimism,
                        Pride,
                        Realization,
                        Relief,
                        Remorse,
                        Sadness,
                        Surprise,
                        Neutral,
                    )
                }
            }

            companion object {
                fun empty() = Emotion(
                    category = Category.Neutral,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("toxicity_analysis")
        data class Toxicity(
            override val id: Id = Id(),
            @SerialName("toxic_category")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Toxic = Category("Toxic")
                    val SevereToxicity = Category("Severe Toxicity")
                    val Obscene = Category("Obscene")
                    val Threat = Category("Threat")
                    val Insult = Category("Insult")
                    val IdentityHate = Category("Identity Hate")

                    val entries = listOf(
                        Toxic,
                        SevereToxicity,
                        Obscene,
                        Threat,
                        Insult,
                        IdentityHate,
                    )
                }
            }

            companion object {
                fun empty() = Toxicity(
                    category = Category.Toxic,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("topic_analysis")
        data class Topic(
            override val id: Id = Id(),
            @SerialName("topic_label")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val ArtsAndCulture = Category("Arts & Culture")
                    val BusinessAndEntrepreneurs = Category("Business & Entrepreneurs")
                    val CelebrityAndPopCulture = Category("Celebrity & Pop Culture")
                    val DiariesAndDailyLife = Category("Diaries & Daily Life")
                    val Family = Category("Family")
                    val FashionAndStyle = Category("Fashion & Style")
                    val FilmTVAndVideo = Category("Film, TV & Video")
                    val FitnessAndHealth = Category("Fitness & Health")
                    val FoodAndDining = Category("Food & Dining")
                    val Gaming = Category("Gaming")
                    val LearningAndEducational = Category("Learning & Educational")
                    val Music = Category("Music")
                    val NewsAndSocialConcern = Category("News & Social Concern")
                    val OtherHobbies = Category("Other Hobbies")
                    val Relationships = Category("Relationships")
                    val ScienceAndTechnology = Category("Science & Technology")
                    val Sports = Category("Sports")
                    val TravelAndAdventure = Category("Travel & Adventure")
                    val YouthAndStudentLife = Category("Youth & Student Life")

                    val entries = listOf(
                        ArtsAndCulture,
                        BusinessAndEntrepreneurs,
                        CelebrityAndPopCulture,
                        DiariesAndDailyLife,
                        Family,
                        FashionAndStyle,
                        FilmTVAndVideo,
                        FitnessAndHealth,
                        FoodAndDining,
                        Gaming,
                        LearningAndEducational,
                        Music,
                        NewsAndSocialConcern,
                        OtherHobbies,
                        Relationships,
                        ScienceAndTechnology,
                        Sports,
                        TravelAndAdventure,
                        YouthAndStudentLife,
                    )
                }
            }

            companion object {
                fun empty() = Topic(
                    category = Category.ArtsAndCulture,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("text_arbitrary")
        data class TextArbitrary(
            override val id: Id = Id(),
            @SerialName("tag")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Default = Category("Default")
                    val entries = listOf(Default)
                }
            }

            companion object {
                fun empty() = TextArbitrary(
                    category = Category.Default,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("image_nsfw")
        data class ImageNsfw(
            override val id: Id = Id(),
            @SerialName("tag")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val NSFW = Category("NSFW")
                    val SFW = Category("SFW")
                    val entries = listOf(NSFW, SFW)
                }
            }

            companion object {
                fun empty() = ImageNsfw(
                    category = Category.NSFW,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }

        @Serializable
        @SerialName("image_arbitrary")
        data class ImageArbitrary(
            override val id: Id = Id(),
            @SerialName("tag")
            val category: Category,
            override val operator: Comparator.Range,
            override val threshold: Double,
        ) : Analysis {
            @Serializable
            @JvmInline
            value class Category(val value: String) {
                companion object {
                    val Default = Category("Default")
                    val entries = listOf(Default)
                }
            }

            companion object {
                fun empty() = ImageArbitrary(
                    category = Category.Default,
                    operator = Comparator.Range.GreaterThan,
                    threshold = 0.8,
                )
            }
        }
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
