/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the License);
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an AS IS BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val uri: GenericUri,
    val creatorId: ProfileId,
    val value: Value,
    val version: Long?,
    val createdAt: Instant,
) {
    @Serializable
    value class Value(
        val value: String,
    )

    @Serializable
    data class Definition(
        val adultOnly: Boolean,
        val blurs: BlurTarget,
        val defaultSetting: Value,
        val identifier: Value,
        val severity: Severity,
    )

    enum class BlurTarget {
        Content,
        Media,
        None,
    }

    enum class Severity {
        Alert,
        Inform,
        None,
    }
}

@Serializable
data class Labeler(
    val uri: GenericUri,
    val creatorId: ProfileId,
    val definitions: List<Label.Definition>,
    val values: List<Label.Value>,
)

private val DefaultLabeler = Labeler(
    uri = GenericUri(uri = "at://did:plc:ar7c4by46qjdydhdevvrndac/app.bsky.labeler.service/self"),
    creatorId = ProfileId(id = "did:plc:ar7c4by46qjdydhdevvrndac"),
    definitions = listOf(
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("spam"),
            severity = Label.Severity.Inform,
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.None,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("impersonation"),
            severity = Label.Severity.Inform,
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("scam"),
            severity = Label.Severity.Alert,
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("intolerant"),
            severity = Label.Severity.Alert,
        ),
        Label.Definition(
            adultOnly = true,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("self-harm"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("security"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("misleading"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("threat"),
            severity = Label.Severity.Inform
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("unsafe-link"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(

            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("illicit"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("misinformation"),
            severity = Label.Severity.Inform
        ),
        Label.Definition(

            adultOnly = false,
            blurs = Label.BlurTarget.None,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("rumor"),
            severity = Label.Severity.Inform
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("rude"),
            severity = Label.Severity.Inform
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("extremist"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = true,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("warn"),
            identifier = Label.Value("sensitive"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("engagement-farming"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = false,
            blurs = Label.BlurTarget.Content,
            defaultSetting = Label.Value("hide"),
            identifier = Label.Value("inauthentic"),
            severity = Label.Severity.Alert
        ),
        Label.Definition(
            adultOnly = true,
            blurs = Label.BlurTarget.Media,
            defaultSetting = Label.Value("show"),
            identifier = Label.Value("sexual-figurative"),
            severity = Label.Severity.None,
        ),
    ),
    values = listOf(
        Label.Value(value = "!hide"),
        Label.Value(value = "!warn"),
        Label.Value(value = "porn"),
        Label.Value(value = "sexual"),
        Label.Value(value = "nudity"),
        Label.Value(value = "sexual-figurative"),
        Label.Value(value = "graphic-media"),
        Label.Value(value = "self-harm"),
        Label.Value(value = "sensitive"),
        Label.Value(value = "extremist"),
        Label.Value(value = "intolerant"),
        Label.Value(value = "threat"),
        Label.Value(value = "rude"),
        Label.Value(value = "illicit"),
        Label.Value(value = "security"),
        Label.Value(value = "unsafe-link"),
        Label.Value(value = "impersonation"),
        Label.Value(value = "misinformation"),
        Label.Value(value = "scam"),
        Label.Value(value = "engagement-farming"),
        Label.Value(value = "spam"),
        Label.Value(value = "rumor"),
        Label.Value(value = "misleading"),
        Label.Value(value = "inauthentic"),
    ),
)