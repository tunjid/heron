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

package com.tunjid.heron.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.Login
import com.tunjid.heron.ui.icons.regular.Add
import com.tunjid.heron.ui.icons.regular.AlternateEmail
import com.tunjid.heron.ui.icons.regular.Edit
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.mention
import heron.feature.profile.generated.resources.post
import heron.ui.core.generated.resources.feed_generator_create
import heron.ui.core.generated.resources.sign_in
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class ProfileFabState(
    val text: StringResource,
    val icon: ImageVector,
) {

    data object SignedOut : ProfileFabState(
        text = CommonStrings.sign_in,
        icon = HeronIcons.AutoMirrored.Login,
    )

    sealed class SignedIn(
        text: StringResource,
        icon: ImageVector,
    ) : ProfileFabState(text, icon) {

        data object Feed : SignedIn(
            text = CommonStrings.feed_generator_create,
            icon = HeronIcons.Regular.Add,
        )

        data object Edit : SignedIn(
            text = Res.string.post,
            icon = HeronIcons.Regular.Edit,
        )

        data object Mention : SignedIn(
            text = Res.string.mention,
            icon = HeronIcons.Regular.AlternateEmail,
        )
    }
}

@Composable
fun PaneScaffoldState.ProfileFab(
    modifier: Modifier = Modifier,
    state: ProfileFabState,
    fabExpanded: Boolean,
    onStateClicked: (ProfileFabState) -> Unit,
) {
    PaneFab(
        modifier = modifier,
        text = stringResource(state.text),
        icon = state.icon,
        expanded = fabExpanded,
        onClick = {
            onStateClicked(state)
        },
    )
}
