/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.profiles

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.SharedElementScope
import com.tunjid.heron.scaffold.scaffold.AppLogo

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProfilesScreen(
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
) = with(sharedElementScope) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Image(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.Center)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(AppLogo),
                    animatedVisibilityScope = sharedElementScope,
                ),
            imageVector = AppLogo,
            contentDescription = null,
        )
    }
}

