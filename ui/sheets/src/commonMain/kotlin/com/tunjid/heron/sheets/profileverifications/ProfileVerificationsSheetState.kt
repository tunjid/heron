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

package com.tunjid.heron.sheets.profileverifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileVerification
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.sheets.utilities.BottomSheetItemCard
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.ui.profile.nameOrHandleOrUnknown
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.trusted_verifier_body
import heron.ui.timeline.generated.resources.trusted_verifier_label
import heron.ui.timeline.generated.resources.trusted_verifier_title
import heron.ui.timeline.generated.resources.verified_account_label
import heron.ui.timeline.generated.resources.verified_body
import heron.ui.timeline.generated.resources.verified_by
import heron.ui.timeline.generated.resources.verified_title
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

@Stable
class ProfileVerificationsSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: ProfileVerificationsStateHolder,
) : BottomSheetState(scope) {

    fun show(
        profileId: Id.Profile,
    ) {
        stateHolder(ProfileVerificationsAction.Load(profileId))
        show()
    }

    override fun onHidden() = Unit

    companion object {
        @Composable
        internal fun rememberUpdatedProfileVerificationsSheetState(
            stateHolder: ProfileVerificationsStateHolder,
            onProfileClicked: (Profile) -> Unit,
        ): ProfileVerificationsSheetState {
            val state = rememberBottomSheetState(
                stateHolder = stateHolder,
                block = ::ProfileVerificationsSheetState,
            )
            ProfileVerificationsBottomSheet(
                sheetState = state,
                onProfileClicked = onProfileClicked,
            )
            return state
        }
    }
}

@Composable
private fun ProfileVerificationsBottomSheet(
    sheetState: ProfileVerificationsSheetState,
    onProfileClicked: (Profile) -> Unit,
) {
    sheetState.ModalBottomSheet {
        val state = sheetState.stateHolder.produceState()
        val subject = state.subject
        val status = subject?.verification
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (subject != null && status != null) {
                val isTrustedVerifier = status.isTrustedVerifier
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (isTrustedVerifier) {
                            TrustedVerifierInfographic(subject = subject)
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isTrustedVerifier) Res.string.trusted_verifier_title
                                    else Res.string.verified_title,
                                    subject.nameOrHandleOrUnknown,
                                ),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(
                                    if (isTrustedVerifier) Res.string.trusted_verifier_body
                                    else Res.string.verified_body,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (!isTrustedVerifier) {
                    item {
                        Text(
                            modifier = Modifier
                                .padding(top = 8.dp),
                            text = stringResource(Res.string.verified_by),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(
                        items = state.verifications,
                        key = { it.uri.uri },
                    ) { verification ->
                        VerificationItem(
                            verification = verification,
                            onClick = {
                                onProfileClicked(verification.issuer)
                                sheetState.hide()
                            },
                        )
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(240.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustedVerifierInfographic(
    subject: Profile,
) {
    BottomSheetItemCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 20.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfographicStep(
                label = subject.nameOrHandleOrUnknown,
            ) {
                AsyncImage(
                    args = remember(
                        subject.avatar,
                        subject.contentDescription,
                    ) {
                        ImageArgs(
                            url = subject.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = subject.contentDescription,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                    modifier = Modifier
                        .size(48.dp),
                )
            }
            InfographicArrow()
            InfographicStep(
                label = stringResource(Res.string.trusted_verifier_label),
            ) {
                Icon(
                    modifier = Modifier
                        .size(48.dp),
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = UiTokens.BookmarkBlue,
                )
            }
            InfographicArrow()
            InfographicStep(
                label = stringResource(Res.string.verified_account_label),
            ) {
                Icon(
                    modifier = Modifier
                        .size(48.dp),
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = UiTokens.BookmarkBlue,
                )
            }
        }
    }
}

@Composable
private fun RowScope.InfographicStep(
    label: String,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun InfographicArrow() {
    Icon(
        modifier = Modifier
            .size(20.dp),
        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun VerificationItem(
    verification: ProfileVerification,
    onClick: () -> Unit,
) {
    val issuer = verification.issuer
    BottomSheetItemCard(
        modifier = Modifier
            .alpha(if (verification.isValid) 1f else InvalidAlpha),
        onClick = onClick,
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            avatar = {
                AsyncImage(
                    args = remember(issuer.avatar) {
                        ImageArgs(
                            url = issuer.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = issuer.contentDescription,
                            shape = RoundedPolygonShape.Circle,
                        )
                    },
                    modifier = Modifier
                        .size(44.dp),
                )
            },
            label = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ProfileName(profile = issuer)
                    ProfileHandle(profile = issuer)
                }
            },
            action = {
                TimeDelta(
                    delta = Clock.System.now() - verification.createdAt,
                )
            },
        )
    }
}

private const val InvalidAlpha = 0.6f
