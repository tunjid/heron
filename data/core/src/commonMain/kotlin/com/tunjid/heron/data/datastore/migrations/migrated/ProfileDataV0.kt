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

package com.tunjid.heron.data.datastore.migrations.migrated

import com.tunjid.heron.data.repository.SavedState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.tunjid.heron.data.repository.SavedState.ProfileData")
internal data class ProfileDataV0(
    val preferences: PreferencesV0,
    val notifications: SavedState.Notifications,
    // Need default for migration
    val writes: SavedState.Writes = SavedState.Writes(),
) {
    fun asProfileData(auth: SavedState.AuthTokens?) =
        SavedState.ProfileData(
            preferences = preferences.asPreferences(),
            notifications = notifications,
            writes = writes,
            auth = auth,
        )
}
