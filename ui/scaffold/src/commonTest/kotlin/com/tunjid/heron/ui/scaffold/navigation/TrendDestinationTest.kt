package com.tunjid.heron.ui.scaffold.navigation

import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TrendDestinationTest {

    @Test
    fun blackSkyTopicLinkGetsLeadingSlashToMatchTheTopicRoute() {
        // Without the leading slash pathDestination's stripBeforePath would drop the "topic" segment.
        assertEquals("/topic/5018", trend(link = "topic/5018").destinationPath)
    }

    @Test
    fun blackSkyTopicLinkToleratesAnExistingLeadingSlash() {
        assertEquals("/topic/42", trend(link = "/topic/42").destinationPath)
    }

    @Test
    fun nonTopicLinkIsUnchanged() {
        assertEquals("/search/kotlin", trend(link = "/search/kotlin").destinationPath)
    }

    @Test
    fun topicDestinationCarriesTheTrendModel() {
        val trend = trend(link = "topic/5018", displayName = "Music", category = "culture")
        val destination = pathDestination(
            path = trend.destinationPath,
            models = listOf(trend),
        ) as NavigationAction.Destination.ToRawUrl

        assertEquals("/topic/5018", destination.path)
        assertEquals(trend, destination.models.filterIsInstance<Trend>().single())
    }

    @Test
    fun trendRoundTripsAsUrlEncodableModel() {
        // The feed route reads the trend back via route.model<Trend>(), which is a base64 CBOR
        // round-trip through the polymorphic UrlEncodableModel serializer.
        val trend: UrlEncodableModel = trend(
            link = "topic/5018",
            displayName = "Music",
            category = "culture",
        )
        assertEquals(
            trend,
            trend.toUrlEncodedBase64().fromBase64EncodedUrl<UrlEncodableModel>(),
        )
    }

    private fun trend(
        link: String,
        topic: String = "topic",
        displayName: String? = "Display",
        category: String? = null,
    ) = Trend(
        topic = topic,
        status = null,
        displayName = displayName,
        description = null,
        link = link,
        startedAt = Instant.parse("2024-01-01T00:00:00Z"),
        postCount = 0,
        category = category,
        actors = emptyList(),
    )
}
