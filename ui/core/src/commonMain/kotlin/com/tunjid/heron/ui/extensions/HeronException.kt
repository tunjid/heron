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

package com.tunjid.heron.ui.extensions

import com.tunjid.heron.data.core.types.AtProtoException
import com.tunjid.heron.data.core.types.ExpiredSessionException
import com.tunjid.heron.data.core.types.HeronException
import com.tunjid.heron.data.core.types.InvalidTokenException
import com.tunjid.heron.data.core.types.MutedThreadException
import com.tunjid.heron.data.core.types.NotificationFilteredOutException
import com.tunjid.heron.data.core.types.RecordCreationException
import com.tunjid.heron.data.core.types.RestrictedProfileException
import com.tunjid.heron.data.core.types.SessionSwitchException
import com.tunjid.heron.data.core.types.UnknownNotificationException
import com.tunjid.heron.data.core.types.UnresolvableProfileException
import com.tunjid.heron.data.core.types.UnresolvableRecordException
import com.tunjid.heron.ui.text.Memo
import heron.ui.core.generated.resources.Res
import heron.ui.core.generated.resources.error_atproto_with_error
import heron.ui.core.generated.resources.error_expired_session
import heron.ui.core.generated.resources.error_generic
import heron.ui.core.generated.resources.error_invalid_token
import heron.ui.core.generated.resources.error_muted_thread
import heron.ui.core.generated.resources.error_network_bad_request
import heron.ui.core.generated.resources.error_network_forbidden
import heron.ui.core.generated.resources.error_network_not_found
import heron.ui.core.generated.resources.error_network_server_error
import heron.ui.core.generated.resources.error_network_too_many_requests
import heron.ui.core.generated.resources.error_network_unauthorized
import heron.ui.core.generated.resources.error_network_unknown
import heron.ui.core.generated.resources.error_notification_filtered_out
import heron.ui.core.generated.resources.error_record_creation
import heron.ui.core.generated.resources.error_restricted_profile
import heron.ui.core.generated.resources.error_session_switch
import heron.ui.core.generated.resources.error_unknown_notification
import heron.ui.core.generated.resources.error_unresolvable_profile
import heron.ui.core.generated.resources.error_unresolvable_record

val HeronException.messageResource: Memo.Resource
    get() = when (this) {
        is ExpiredSessionException -> Memo.Resource(stringResource = Res.string.error_expired_session)
        is MutedThreadException -> Memo.Resource(stringResource = Res.string.error_muted_thread)
        is NotificationFilteredOutException -> Memo.Resource(stringResource = Res.string.error_notification_filtered_out)
        is RecordCreationException -> Memo.Resource(stringResource = Res.string.error_record_creation)
        is RestrictedProfileException -> Memo.Resource(stringResource = Res.string.error_restricted_profile)
        is SessionSwitchException -> Memo.Resource(stringResource = Res.string.error_session_switch)
        is UnknownNotificationException -> Memo.Resource(stringResource = Res.string.error_unknown_notification)
        is UnresolvableProfileException -> Memo.Resource(stringResource = Res.string.error_unresolvable_profile)
        is UnresolvableRecordException -> Memo.Resource(stringResource = Res.string.error_unresolvable_record)
        is InvalidTokenException -> Memo.Resource(stringResource = Res.string.error_invalid_token)
        is AtProtoException ->
            if (error != null) Memo.Resource(
                stringResource = Res.string.error_atproto_with_error,
                args = listOf(error as Any),
            )
            else when (statusCode) {
                400 -> Memo.Resource(stringResource = Res.string.error_network_bad_request)
                401 -> Memo.Resource(stringResource = Res.string.error_network_unauthorized)
                403 -> Memo.Resource(stringResource = Res.string.error_network_forbidden)
                404 -> Memo.Resource(stringResource = Res.string.error_network_not_found)
                429 -> Memo.Resource(stringResource = Res.string.error_network_too_many_requests)
                in 500..599 -> Memo.Resource(stringResource = Res.string.error_network_server_error)
                else -> Memo.Resource(
                    stringResource = Res.string.error_network_unknown,
                    args = listOf(statusCode as Any),
                )
            }
    }

val Throwable.userMessage: Memo
    get() =
        if (this is HeronException) this.messageResource
        else Memo.Resource(
            stringResource = Res.string.error_generic,
            args = listOf((message ?: toString()) as Any),
        )
