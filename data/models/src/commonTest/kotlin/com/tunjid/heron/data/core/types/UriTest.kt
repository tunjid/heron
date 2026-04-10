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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UriTest {

    private val authority = "did:plc:abc123"
    private val rkey = "3kxyz"

    // ----- asRecordUriOrNull: prefixed input (canonical form) -----

    @Test
    fun asRecordUriOrNull_parsesPrefixedPostUri() {
        val input = "at://$authority/${PostUri.NAMESPACE}/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<PostUri>(parsed)
        assertEquals(input, parsed.uri)
    }

    @Test
    fun asRecordUriOrNull_parsesPrefixedFeedGeneratorUri() {
        val input = "at://$authority/${FeedGeneratorUri.NAMESPACE}/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<FeedGeneratorUri>(parsed)
        assertEquals(input, parsed.uri)
    }

    @Test
    fun asRecordUriOrNull_parsesPrefixedListUri() {
        val input = "at://$authority/${ListUri.NAMESPACE}/$rkey"
        assertIs<ListUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_unknownCollectionBecomesUnknownRecordUri() {
        val input = "at://$authority/made.up.collection/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<UnknownRecordUri>(parsed)
        assertEquals(input, parsed.uri)
    }

    // ----- asRecordUriOrNull: unprefixed input (leniency) -----

    @Test
    fun asRecordUriOrNull_parsesUnprefixedPostUri() {
        val input = "$authority/${PostUri.NAMESPACE}/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<PostUri>(parsed)
        // Output is normalized to include the at:// prefix.
        assertEquals("at://$input", parsed.uri)
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedFeedGeneratorUri() {
        val input = "$authority/${FeedGeneratorUri.NAMESPACE}/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<FeedGeneratorUri>(parsed)
        assertEquals("at://$input", parsed.uri)
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedListUri() {
        val input = "$authority/${ListUri.NAMESPACE}/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<ListUri>(parsed)
        assertEquals("at://$input", parsed.uri)
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedStarterPackUri() {
        val input = "$authority/${StarterPackUri.NAMESPACE}/$rkey"
        assertIs<StarterPackUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedLikeUri() {
        val input = "$authority/${LikeUri.NAMESPACE}/$rkey"
        assertIs<LikeUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedRepostUri() {
        val input = "$authority/${RepostUri.NAMESPACE}/$rkey"
        assertIs<RepostUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedFollowUri() {
        val input = "$authority/${FollowUri.NAMESPACE}/$rkey"
        assertIs<FollowUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedBlockUri() {
        val input = "$authority/${BlockUri.NAMESPACE}/$rkey"
        assertIs<BlockUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedListMemberUri() {
        val input = "$authority/${ListMemberUri.NAMESPACE}/$rkey"
        assertIs<ListMemberUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedLabelerUri() {
        val input = "$authority/${LabelerUri.NAMESPACE}/$rkey"
        assertIs<LabelerUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedStandardPublicationUri() {
        val input = "$authority/${StandardPublicationUri.NAMESPACE}/$rkey"
        assertIs<StandardPublicationUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedStandardDocumentUri() {
        val input = "$authority/${StandardDocumentUri.NAMESPACE}/$rkey"
        assertIs<StandardDocumentUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_parsesUnprefixedStandardSubscriptionUri() {
        val input = "$authority/${StandardSubscriptionUri.NAMESPACE}/$rkey"
        assertIs<StandardSubscriptionUri>(input.asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_unprefixedUnknownCollectionBecomesUnknownRecordUri() {
        val input = "$authority/made.up.collection/$rkey"
        val parsed = input.asRecordUriOrNull()
        assertIs<UnknownRecordUri>(parsed)
        assertEquals("at://$input", parsed.uri)
    }

    // ----- asRecordUriOrNull: invalid inputs -----

    @Test
    fun asRecordUriOrNull_emptyStringReturnsNull() {
        assertNull("".asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_prefixOnlyReturnsNull() {
        assertNull("at://".asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_missingRkeyReturnsNull() {
        assertNull("at://$authority/${PostUri.NAMESPACE}/".asRecordUriOrNull())
        assertNull("$authority/${PostUri.NAMESPACE}/".asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_missingCollectionReturnsNull() {
        assertNull("at://$authority//$rkey".asRecordUriOrNull())
        assertNull("$authority//$rkey".asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_leadingSlashUnprefixedReturnsNull() {
        assertNull("/${PostUri.NAMESPACE}/$rkey".asRecordUriOrNull())
    }

    @Test
    fun asRecordUriOrNull_onlyAuthorityReturnsNull() {
        assertNull(authority.asRecordUriOrNull())
        assertNull("at://$authority".asRecordUriOrNull())
    }

    // ----- asEmbeddableRecordUriOrNull: leniency -----

    @Test
    fun asEmbeddableRecordUriOrNull_parsesUnprefixedPostUri() {
        val input = "$authority/${PostUri.NAMESPACE}/$rkey"
        val parsed = input.asEmbeddableRecordUriOrNull()
        assertIs<PostUri>(parsed)
        assertEquals("at://$input", parsed.uri)
    }

    @Test
    fun asEmbeddableRecordUriOrNull_parsesPrefixedStarterPackUri() {
        val input = "at://$authority/${StarterPackUri.NAMESPACE}/$rkey"
        val parsed = input.asEmbeddableRecordUriOrNull()
        assertIs<StarterPackUri>(parsed)
        assertEquals(input, parsed.uri)
    }

    @Test
    fun asEmbeddableRecordUriOrNull_unprefixedNonEmbeddableCollectionReturnsNull() {
        val input = "$authority/${FollowUri.NAMESPACE}/$rkey"
        assertNull(input.asEmbeddableRecordUriOrNull())
    }

    // ----- Round-trip: downstream helpers work on both inputs -----

    @Test
    fun recordKey_and_profileId_workForUnprefixedInput() {
        val parsed = "$authority/${PostUri.NAMESPACE}/$rkey".asRecordUriOrNull()
        assertIs<PostUri>(parsed)
        assertEquals(ProfileId(authority), parsed.profileId())
        assertEquals(RecordKey(rkey), parsed.recordKey)
    }

    @Test
    fun normalization_isIdempotent() {
        val unprefixed = "$authority/${PostUri.NAMESPACE}/$rkey"
        val first = unprefixed.asRecordUriOrNull()
        val second = first?.uri?.asRecordUriOrNull()
        assertEquals(first, second)
        assertEquals(first?.uri?.startsWith("at://"), true)
    }
}
