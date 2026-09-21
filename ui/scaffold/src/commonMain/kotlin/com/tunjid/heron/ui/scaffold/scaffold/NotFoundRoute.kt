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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.SearchOff
import heron.ui.scaffold.generated.resources.Res
import heron.ui.scaffold.generated.resources.go_back
import heron.ui.scaffold.generated.resources.go_home
import heron.ui.scaffold.generated.resources.not_found_description
import heron.ui.scaffold.generated.resources.not_found_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotFoundRoute(
    onGoBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyContent(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            titleRes = Res.string.not_found_title,
            descriptionRes = Res.string.not_found_description,
            icon = HeronIcons.Regular.SearchOff,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            TextButton(
                onClick = onGoBack,
            ) {
                Text(text = stringResource(Res.string.go_back))
            }
            FilledTonalButton(
                onClick = onGoHome,
            ) {
                Text(text = stringResource(Res.string.go_home))
            }
        }
    }
}
