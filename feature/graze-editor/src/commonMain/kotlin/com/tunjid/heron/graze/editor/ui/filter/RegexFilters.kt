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

package com.tunjid.heron.graze.editor.ui.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.graze.Filter
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.regex_any
import heron.feature.graze_editor.generated.resources.regex_matches
import heron.feature.graze_editor.generated.resources.regex_negation
import heron.feature.graze_editor.generated.resources.regex_none
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegexFilter(
    filter: Filter.Regex,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UnsupportedFilter(
        modifier = modifier,
        title = stringResource(
            when (filter) {
                is Filter.Regex.Any -> Res.string.regex_any
                is Filter.Regex.Matches -> Res.string.regex_matches
                is Filter.Regex.Negation -> Res.string.regex_negation
                is Filter.Regex.None -> Res.string.regex_none
            },
        ),
        onRemove = onRemove,
    )
}
