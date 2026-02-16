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

package com.tunjid.heron.graze.editor.ui.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.analysis_arbitrary_default
import heron.feature.graze_editor.generated.resources.analysis_emotion_admiration
import heron.feature.graze_editor.generated.resources.analysis_emotion_amusement
import heron.feature.graze_editor.generated.resources.analysis_emotion_anger
import heron.feature.graze_editor.generated.resources.analysis_emotion_annoyance
import heron.feature.graze_editor.generated.resources.analysis_emotion_approval
import heron.feature.graze_editor.generated.resources.analysis_emotion_caring
import heron.feature.graze_editor.generated.resources.analysis_emotion_confusion
import heron.feature.graze_editor.generated.resources.analysis_emotion_curiosity
import heron.feature.graze_editor.generated.resources.analysis_emotion_desire
import heron.feature.graze_editor.generated.resources.analysis_emotion_disappointment
import heron.feature.graze_editor.generated.resources.analysis_emotion_disapproval
import heron.feature.graze_editor.generated.resources.analysis_emotion_disgust
import heron.feature.graze_editor.generated.resources.analysis_emotion_embarrassment
import heron.feature.graze_editor.generated.resources.analysis_emotion_excitement
import heron.feature.graze_editor.generated.resources.analysis_emotion_fear
import heron.feature.graze_editor.generated.resources.analysis_emotion_gratitude
import heron.feature.graze_editor.generated.resources.analysis_emotion_grief
import heron.feature.graze_editor.generated.resources.analysis_emotion_joy
import heron.feature.graze_editor.generated.resources.analysis_emotion_love
import heron.feature.graze_editor.generated.resources.analysis_emotion_nervousness
import heron.feature.graze_editor.generated.resources.analysis_emotion_neutral
import heron.feature.graze_editor.generated.resources.analysis_emotion_optimism
import heron.feature.graze_editor.generated.resources.analysis_emotion_pride
import heron.feature.graze_editor.generated.resources.analysis_emotion_realization
import heron.feature.graze_editor.generated.resources.analysis_emotion_relief
import heron.feature.graze_editor.generated.resources.analysis_emotion_remorse
import heron.feature.graze_editor.generated.resources.analysis_emotion_sadness
import heron.feature.graze_editor.generated.resources.analysis_emotion_surprise
import heron.feature.graze_editor.generated.resources.analysis_language_arabic
import heron.feature.graze_editor.generated.resources.analysis_language_bulgarian
import heron.feature.graze_editor.generated.resources.analysis_language_chinese
import heron.feature.graze_editor.generated.resources.analysis_language_dutch
import heron.feature.graze_editor.generated.resources.analysis_language_english
import heron.feature.graze_editor.generated.resources.analysis_language_french
import heron.feature.graze_editor.generated.resources.analysis_language_german
import heron.feature.graze_editor.generated.resources.analysis_language_greek
import heron.feature.graze_editor.generated.resources.analysis_language_hindi
import heron.feature.graze_editor.generated.resources.analysis_language_italian
import heron.feature.graze_editor.generated.resources.analysis_language_japanese
import heron.feature.graze_editor.generated.resources.analysis_language_polish
import heron.feature.graze_editor.generated.resources.analysis_language_portuguese
import heron.feature.graze_editor.generated.resources.analysis_language_russian
import heron.feature.graze_editor.generated.resources.analysis_language_spanish
import heron.feature.graze_editor.generated.resources.analysis_language_swahili
import heron.feature.graze_editor.generated.resources.analysis_language_thai
import heron.feature.graze_editor.generated.resources.analysis_language_turkish
import heron.feature.graze_editor.generated.resources.analysis_language_urdu
import heron.feature.graze_editor.generated.resources.analysis_language_vietnamese
import heron.feature.graze_editor.generated.resources.analysis_nsfw_nsfw
import heron.feature.graze_editor.generated.resources.analysis_nsfw_sfw
import heron.feature.graze_editor.generated.resources.analysis_sentiment_negative
import heron.feature.graze_editor.generated.resources.analysis_sentiment_neutral
import heron.feature.graze_editor.generated.resources.analysis_sentiment_positive
import heron.feature.graze_editor.generated.resources.analysis_topic_arts_culture
import heron.feature.graze_editor.generated.resources.analysis_topic_business_entrepreneurs
import heron.feature.graze_editor.generated.resources.analysis_topic_celebrity_pop_culture
import heron.feature.graze_editor.generated.resources.analysis_topic_diaries_daily_life
import heron.feature.graze_editor.generated.resources.analysis_topic_family
import heron.feature.graze_editor.generated.resources.analysis_topic_fashion_style
import heron.feature.graze_editor.generated.resources.analysis_topic_film_tv_video
import heron.feature.graze_editor.generated.resources.analysis_topic_fitness_health
import heron.feature.graze_editor.generated.resources.analysis_topic_food_dining
import heron.feature.graze_editor.generated.resources.analysis_topic_gaming
import heron.feature.graze_editor.generated.resources.analysis_topic_learning_educational
import heron.feature.graze_editor.generated.resources.analysis_topic_music
import heron.feature.graze_editor.generated.resources.analysis_topic_news_social_concern
import heron.feature.graze_editor.generated.resources.analysis_topic_other_hobbies
import heron.feature.graze_editor.generated.resources.analysis_topic_relationships
import heron.feature.graze_editor.generated.resources.analysis_topic_science_technology
import heron.feature.graze_editor.generated.resources.analysis_topic_sports
import heron.feature.graze_editor.generated.resources.analysis_topic_travel_adventure
import heron.feature.graze_editor.generated.resources.analysis_topic_youth_student_life
import heron.feature.graze_editor.generated.resources.analysis_toxicity_identity_hate
import heron.feature.graze_editor.generated.resources.analysis_toxicity_insult
import heron.feature.graze_editor.generated.resources.analysis_toxicity_obscene
import heron.feature.graze_editor.generated.resources.analysis_toxicity_severe_toxicity
import heron.feature.graze_editor.generated.resources.analysis_toxicity_threat
import heron.feature.graze_editor.generated.resources.analysis_toxicity_toxic
import heron.feature.graze_editor.generated.resources.emotion_analysis
import heron.feature.graze_editor.generated.resources.emotion_category
import heron.feature.graze_editor.generated.resources.financial_category
import heron.feature.graze_editor.generated.resources.financial_sentiment
import heron.feature.graze_editor.generated.resources.image_arbitrary
import heron.feature.graze_editor.generated.resources.image_nsfw
import heron.feature.graze_editor.generated.resources.language_analysis
import heron.feature.graze_editor.generated.resources.language_name
import heron.feature.graze_editor.generated.resources.moderation_category_unknown
import heron.feature.graze_editor.generated.resources.sentiment_analysis
import heron.feature.graze_editor.generated.resources.sentiment_category
import heron.feature.graze_editor.generated.resources.tag
import heron.feature.graze_editor.generated.resources.text_arbitrary
import heron.feature.graze_editor.generated.resources.topic_analysis
import heron.feature.graze_editor.generated.resources.topic_label
import heron.feature.graze_editor.generated.resources.toxic_category
import heron.feature.graze_editor.generated.resources.toxicity_analysis
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnalysisFilter(
    filter: Filter.Analysis,
    onUpdate: (Filter.Analysis) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (filter) {
        is Filter.Analysis.Language -> stringResource(Res.string.language_analysis)
        is Filter.Analysis.Sentiment -> stringResource(Res.string.sentiment_analysis)
        is Filter.Analysis.FinancialSentiment -> stringResource(Res.string.financial_sentiment)
        is Filter.Analysis.Emotion -> stringResource(Res.string.emotion_analysis)
        is Filter.Analysis.Toxicity -> stringResource(Res.string.toxicity_analysis)
        is Filter.Analysis.Topic -> stringResource(Res.string.topic_analysis)
        is Filter.Analysis.TextArbitrary -> stringResource(Res.string.text_arbitrary)
        is Filter.Analysis.ImageNsfw -> stringResource(Res.string.image_nsfw)
        is Filter.Analysis.ImageArbitrary -> stringResource(Res.string.image_arbitrary)
    }

    if (filter is Filter.Analysis.TextArbitrary || filter is Filter.Analysis.ImageArbitrary) {
        return UnsupportedFilter(
            modifier = modifier,
            title = title,
            onRemove = onRemove,
        )
    }

    val categoryLabel = when (filter) {
        is Filter.Analysis.Language -> stringResource(Res.string.language_name)
        is Filter.Analysis.Sentiment -> stringResource(Res.string.sentiment_category)
        is Filter.Analysis.FinancialSentiment -> stringResource(Res.string.financial_category)
        is Filter.Analysis.Emotion -> stringResource(Res.string.emotion_category)
        is Filter.Analysis.Toxicity -> stringResource(Res.string.toxic_category)
        is Filter.Analysis.Topic -> stringResource(Res.string.topic_label)
        is Filter.Analysis.TextArbitrary -> stringResource(Res.string.tag)
        is Filter.Analysis.ImageNsfw -> stringResource(Res.string.tag)
        is Filter.Analysis.ImageArbitrary -> stringResource(Res.string.tag)
    }

    StandardFilter(
        modifier = modifier,
        title = title,
        onRemove = onRemove,
        startContent = {
            when (filter) {
                is Filter.Analysis.Language -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.Language.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.Language.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.Sentiment -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.Sentiment.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.Sentiment.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.FinancialSentiment -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.FinancialSentiment.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.FinancialSentiment.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.Emotion -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.Emotion.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.Emotion.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.Toxicity -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.Toxicity.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.Toxicity.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.Topic -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.Topic.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.Topic.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.TextArbitrary -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.TextArbitrary.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.TextArbitrary.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.ImageNsfw -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.ImageNsfw.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.ImageNsfw.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )

                is Filter.Analysis.ImageArbitrary -> Dropdown(
                    label = categoryLabel,
                    selected = filter.category,
                    options = Filter.Analysis.ImageArbitrary.Category.entries,
                    modifier = Modifier.fillMaxWidth(),
                    stringRes = Filter.Analysis.ImageArbitrary.Category::stringRes,
                    onSelect = { onUpdate(filter.copy(category = it)) },
                )
            }
        },
        endContent = {
            ComparatorDropdown(
                selected = filter.operator,
                options = Filter.Comparator.Range.entries,
                onSelect = { newOperator ->
                    onUpdate(
                        when (filter) {
                            is Filter.Analysis.Language -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Sentiment -> filter.copy(operator = newOperator)
                            is Filter.Analysis.FinancialSentiment -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Emotion -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Toxicity -> filter.copy(operator = newOperator)
                            is Filter.Analysis.Topic -> filter.copy(operator = newOperator)
                            is Filter.Analysis.TextArbitrary -> filter.copy(operator = newOperator)
                            is Filter.Analysis.ImageNsfw -> filter.copy(operator = newOperator)
                            is Filter.Analysis.ImageArbitrary -> filter.copy(operator = newOperator)
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        additionalContent = {
            ThresholdSlider(
                threshold = filter.threshold,
                onThresholdChanged = { threshold ->
                    onUpdate(
                        when (filter) {
                            is Filter.Analysis.Language -> filter.copy(threshold = threshold)
                            is Filter.Analysis.Sentiment -> filter.copy(threshold = threshold)
                            is Filter.Analysis.FinancialSentiment -> filter.copy(threshold = threshold)
                            is Filter.Analysis.Emotion -> filter.copy(threshold = threshold)
                            is Filter.Analysis.Toxicity -> filter.copy(threshold = threshold)
                            is Filter.Analysis.Topic -> filter.copy(threshold = threshold)
                            is Filter.Analysis.TextArbitrary -> filter.copy(threshold = threshold)
                            is Filter.Analysis.ImageNsfw -> filter.copy(threshold = threshold)
                            is Filter.Analysis.ImageArbitrary -> filter.copy(threshold = threshold)
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

fun Filter.Analysis.Language.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.Language.Category.Japanese -> Res.string.analysis_language_japanese
    Filter.Analysis.Language.Category.Dutch -> Res.string.analysis_language_dutch
    Filter.Analysis.Language.Category.Arabic -> Res.string.analysis_language_arabic
    Filter.Analysis.Language.Category.Polish -> Res.string.analysis_language_polish
    Filter.Analysis.Language.Category.German -> Res.string.analysis_language_german
    Filter.Analysis.Language.Category.Italian -> Res.string.analysis_language_italian
    Filter.Analysis.Language.Category.Portuguese -> Res.string.analysis_language_portuguese
    Filter.Analysis.Language.Category.Turkish -> Res.string.analysis_language_turkish
    Filter.Analysis.Language.Category.Spanish -> Res.string.analysis_language_spanish
    Filter.Analysis.Language.Category.Hindi -> Res.string.analysis_language_hindi
    Filter.Analysis.Language.Category.Greek -> Res.string.analysis_language_greek
    Filter.Analysis.Language.Category.Urdu -> Res.string.analysis_language_urdu
    Filter.Analysis.Language.Category.Bulgarian -> Res.string.analysis_language_bulgarian
    Filter.Analysis.Language.Category.English -> Res.string.analysis_language_english
    Filter.Analysis.Language.Category.French -> Res.string.analysis_language_french
    Filter.Analysis.Language.Category.Chinese -> Res.string.analysis_language_chinese
    Filter.Analysis.Language.Category.Russian -> Res.string.analysis_language_russian
    Filter.Analysis.Language.Category.Thai -> Res.string.analysis_language_thai
    Filter.Analysis.Language.Category.Swahili -> Res.string.analysis_language_swahili
    Filter.Analysis.Language.Category.Vietnamese -> Res.string.analysis_language_vietnamese
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.Sentiment.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.Sentiment.Category.Positive -> Res.string.analysis_sentiment_positive
    Filter.Analysis.Sentiment.Category.Negative -> Res.string.analysis_sentiment_negative
    Filter.Analysis.Sentiment.Category.Neutral -> Res.string.analysis_sentiment_neutral
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.FinancialSentiment.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.FinancialSentiment.Category.Positive -> Res.string.analysis_sentiment_positive
    Filter.Analysis.FinancialSentiment.Category.Negative -> Res.string.analysis_sentiment_negative
    Filter.Analysis.FinancialSentiment.Category.Neutral -> Res.string.analysis_sentiment_neutral
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.Emotion.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.Emotion.Category.Admiration -> Res.string.analysis_emotion_admiration
    Filter.Analysis.Emotion.Category.Amusement -> Res.string.analysis_emotion_amusement
    Filter.Analysis.Emotion.Category.Anger -> Res.string.analysis_emotion_anger
    Filter.Analysis.Emotion.Category.Annoyance -> Res.string.analysis_emotion_annoyance
    Filter.Analysis.Emotion.Category.Approval -> Res.string.analysis_emotion_approval
    Filter.Analysis.Emotion.Category.Caring -> Res.string.analysis_emotion_caring
    Filter.Analysis.Emotion.Category.Confusion -> Res.string.analysis_emotion_confusion
    Filter.Analysis.Emotion.Category.Curiosity -> Res.string.analysis_emotion_curiosity
    Filter.Analysis.Emotion.Category.Desire -> Res.string.analysis_emotion_desire
    Filter.Analysis.Emotion.Category.Disappointment -> Res.string.analysis_emotion_disappointment
    Filter.Analysis.Emotion.Category.Disapproval -> Res.string.analysis_emotion_disapproval
    Filter.Analysis.Emotion.Category.Disgust -> Res.string.analysis_emotion_disgust
    Filter.Analysis.Emotion.Category.Embarrassment -> Res.string.analysis_emotion_embarrassment
    Filter.Analysis.Emotion.Category.Excitement -> Res.string.analysis_emotion_excitement
    Filter.Analysis.Emotion.Category.Fear -> Res.string.analysis_emotion_fear
    Filter.Analysis.Emotion.Category.Gratitude -> Res.string.analysis_emotion_gratitude
    Filter.Analysis.Emotion.Category.Grief -> Res.string.analysis_emotion_grief
    Filter.Analysis.Emotion.Category.Joy -> Res.string.analysis_emotion_joy
    Filter.Analysis.Emotion.Category.Love -> Res.string.analysis_emotion_love
    Filter.Analysis.Emotion.Category.Nervousness -> Res.string.analysis_emotion_nervousness
    Filter.Analysis.Emotion.Category.Optimism -> Res.string.analysis_emotion_optimism
    Filter.Analysis.Emotion.Category.Pride -> Res.string.analysis_emotion_pride
    Filter.Analysis.Emotion.Category.Realization -> Res.string.analysis_emotion_realization
    Filter.Analysis.Emotion.Category.Relief -> Res.string.analysis_emotion_relief
    Filter.Analysis.Emotion.Category.Remorse -> Res.string.analysis_emotion_remorse
    Filter.Analysis.Emotion.Category.Sadness -> Res.string.analysis_emotion_sadness
    Filter.Analysis.Emotion.Category.Surprise -> Res.string.analysis_emotion_surprise
    Filter.Analysis.Emotion.Category.Neutral -> Res.string.analysis_emotion_neutral
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.Toxicity.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.Toxicity.Category.Toxic -> Res.string.analysis_toxicity_toxic
    Filter.Analysis.Toxicity.Category.SevereToxicity -> Res.string.analysis_toxicity_severe_toxicity
    Filter.Analysis.Toxicity.Category.Obscene -> Res.string.analysis_toxicity_obscene
    Filter.Analysis.Toxicity.Category.Threat -> Res.string.analysis_toxicity_threat
    Filter.Analysis.Toxicity.Category.Insult -> Res.string.analysis_toxicity_insult
    Filter.Analysis.Toxicity.Category.IdentityHate -> Res.string.analysis_toxicity_identity_hate
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.Topic.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.Topic.Category.ArtsAndCulture -> Res.string.analysis_topic_arts_culture
    Filter.Analysis.Topic.Category.BusinessAndEntrepreneurs -> Res.string.analysis_topic_business_entrepreneurs
    Filter.Analysis.Topic.Category.CelebrityAndPopCulture -> Res.string.analysis_topic_celebrity_pop_culture
    Filter.Analysis.Topic.Category.DiariesAndDailyLife -> Res.string.analysis_topic_diaries_daily_life
    Filter.Analysis.Topic.Category.Family -> Res.string.analysis_topic_family
    Filter.Analysis.Topic.Category.FashionAndStyle -> Res.string.analysis_topic_fashion_style
    Filter.Analysis.Topic.Category.FilmTVAndVideo -> Res.string.analysis_topic_film_tv_video
    Filter.Analysis.Topic.Category.FitnessAndHealth -> Res.string.analysis_topic_fitness_health
    Filter.Analysis.Topic.Category.FoodAndDining -> Res.string.analysis_topic_food_dining
    Filter.Analysis.Topic.Category.Gaming -> Res.string.analysis_topic_gaming
    Filter.Analysis.Topic.Category.LearningAndEducational -> Res.string.analysis_topic_learning_educational
    Filter.Analysis.Topic.Category.Music -> Res.string.analysis_topic_music
    Filter.Analysis.Topic.Category.NewsAndSocialConcern -> Res.string.analysis_topic_news_social_concern
    Filter.Analysis.Topic.Category.OtherHobbies -> Res.string.analysis_topic_other_hobbies
    Filter.Analysis.Topic.Category.Relationships -> Res.string.analysis_topic_relationships
    Filter.Analysis.Topic.Category.ScienceAndTechnology -> Res.string.analysis_topic_science_technology
    Filter.Analysis.Topic.Category.Sports -> Res.string.analysis_topic_sports
    Filter.Analysis.Topic.Category.TravelAndAdventure -> Res.string.analysis_topic_travel_adventure
    Filter.Analysis.Topic.Category.YouthAndStudentLife -> Res.string.analysis_topic_youth_student_life
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.TextArbitrary.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.TextArbitrary.Category.Default -> Res.string.analysis_arbitrary_default
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.ImageNsfw.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.ImageNsfw.Category.NSFW -> Res.string.analysis_nsfw_nsfw
    Filter.Analysis.ImageNsfw.Category.SFW -> Res.string.analysis_nsfw_sfw
    else -> Res.string.moderation_category_unknown
}

fun Filter.Analysis.ImageArbitrary.Category.stringRes(): StringResource = when (this) {
    Filter.Analysis.ImageArbitrary.Category.Default -> Res.string.analysis_arbitrary_default
    else -> Res.string.moderation_category_unknown
}
