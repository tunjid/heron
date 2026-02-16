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

import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.ProfileViewerState

class UnresolvableProfileException(
    profileId: Id.Profile,
) : IllegalArgumentException("The profile with $profileId is not resolvable")

class UnresolvableRecordException(
    uri: RecordUri,
) : IllegalArgumentException("The record URI $uri is not resolvable")

class RestrictedProfileException(
    profileId: ProfileId,
    profileViewerState: ProfileViewerState,
) : IllegalArgumentException("The profile with did $profileId is restricted $profileViewerState")

class UnknownNotificationException(
    uri: RecordUri,
) : IllegalArgumentException("The record URI $uri does not have a known notification")

class NotificationFilteredOutException(
    val reason: Notification.Reason,
) : IllegalArgumentException("Notification filtered out by user preferences: $reason")

class MutedThreadException(
    postUri: PostUri,
) : IllegalArgumentException("The post with URI $postUri has been muted")

class RecordCreationException(
    profileId: ProfileId,
    collection: String,
) : IllegalArgumentException("Record creation for $profileId in collection $collection failed")

class SessionSwitchException(
    profileId: Id.Profile,
) : Exception("Unable to switch to a accounts to $profileId")
