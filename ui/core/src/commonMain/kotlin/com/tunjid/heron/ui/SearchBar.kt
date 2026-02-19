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

package com.tunjid.heron.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import heron.ui.core.generated.resources.Res
import heron.ui.core.generated.resources.clear_search
import heron.ui.core.generated.resources.search
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchBar(
    searchQuery: String,
    focusRequester: FocusRequester? = null,
    onQueryChanged: (String) -> Unit,
    onQueryConfirmed: () -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()) {
        OutlinedTextField(
            modifier =
                when (focusRequester) {
                    null -> Modifier
                    else -> Modifier.focusRequester(focusRequester)
                }.fillMaxWidth(),
            value = searchQuery,
            onValueChange = { onQueryChanged(it) },
            suffix = {
                AnimatedVisibility(
                    modifier = Modifier,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    visible = searchQuery.isNotBlank(),
                ) {
                    Icon(
                        modifier = Modifier.clip(CircleShape).clickable { onQueryChanged("") },
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = stringResource(Res.string.clear_search),
                    )
                }
            },
            textStyle = MaterialTheme.typography.labelLarge,
            singleLine = true,
            shape = SearchBarShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions { onQueryConfirmed() },
        )
        AnimatedVisibility(
            modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.CenterStart),
            visible = searchQuery.isBlank(),
        ) {
            Text(
                text = stringResource(Res.string.search),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private val SearchBarShape = RoundedCornerShape(36.dp)
