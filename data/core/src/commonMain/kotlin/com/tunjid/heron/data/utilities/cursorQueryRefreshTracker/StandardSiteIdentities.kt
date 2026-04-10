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

package com.tunjid.heron.data.utilities.cursorQueryRefreshTracker

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.records.StandardPublicationDocumentsQuery

internal fun ProfilesQuery.authorDocumentsIdentity(): String =
    "standard.authorDocuments:${profileId.id}"

internal fun StandardPublicationDocumentsQuery.publicationDocumentsIdentity(): String =
    "standard.publicationDocuments:${publicationUri.uri}"

@Suppress("UnusedReceiverParameter")
internal fun CursorQuery.subscribedPublicationsIdentity(): String =
    "standard.subscribedPublications"
