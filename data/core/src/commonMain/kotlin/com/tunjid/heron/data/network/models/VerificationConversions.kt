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

package com.tunjid.heron.data.network.models

import app.bsky.actor.VerificationState
import app.bsky.actor.VerificationView
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileVerificationUri
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.profile.ProfileVerificationEntity

internal fun VerificationState.verificationStatusEntity(): ProfileEntity.VerificationStatus =
    ProfileEntity.VerificationStatus(
        verifiedStatus = verifiedStatus.value,
        trustedVerifierStatus = trustedVerifierStatus.value,
    )

internal fun VerificationView.profileVerificationEntity(
    verifiedProfileId: ProfileId,
): ProfileVerificationEntity =
    ProfileVerificationEntity(
        uri = uri.atUri.let(::ProfileVerificationUri),
        verifiedProfileId = verifiedProfileId,
        issuerProfileId = issuer.did.let(::ProfileId),
        isValid = isValid,
        createdAt = createdAt,
    )

internal fun VerificationView.issuerProfileEntity(): ProfileEntity =
    ProfileEntity(
        did = issuer.did.let(::ProfileId),
        handle = issuerHandle?.handle?.let(::ProfileHandle) ?: Constants.unknownAuthorHandle,
        displayName = issuerDisplayName,
        description = null,
        avatar = null,
        banner = null,
        followersCount = null,
        followsCount = null,
        postsCount = null,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = null,
        associated = ProfileEntity.Associated(
            createdListCount = 0,
            createdFeedGeneratorCount = 0,
            createdStarterPackCount = 0,
        ),
        status = null,
        pronouns = null,
        verification = null,
    )

internal fun VerificationState.verificationStatus(): Profile.VerificationStatus =
    Profile.VerificationStatus(
        verifiedStatus = verifiedStatus.value.toDomainVerificationStatus(),
        trustedVerifierStatus = trustedVerifierStatus.value.toDomainVerificationStatus(),
    )

private fun String.toDomainVerificationStatus(): Profile.VerificationStatus.Status =
    when (this) {
        VERIFICATION_STATUS_VALID -> Profile.VerificationStatus.Status.Valid
        VERIFICATION_STATUS_INVALID -> Profile.VerificationStatus.Status.Invalid
        else -> Profile.VerificationStatus.Status.None
    }

private const val VERIFICATION_STATUS_VALID = "valid"
private const val VERIFICATION_STATUS_INVALID = "invalid"
