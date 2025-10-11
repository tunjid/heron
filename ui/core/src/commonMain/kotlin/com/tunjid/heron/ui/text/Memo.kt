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

package com.tunjid.heron.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

sealed interface Memo {
    data class Resource(
        val stringResource: StringResource,
        val args: List<Any> = emptyList(),
    ) : Memo

    data class Text(
        val message: String,
    ) : Memo
}

suspend fun Memo.message(): String =
    when (this) {
        is Memo.Resource -> when {
            args.isEmpty() -> getString(
                resource = stringResource,
            )
            else -> getString(
                resource = stringResource,
                *(
                    args
                        .map { if (it is StringResource) getString(it) else it }
                        .toTypedArray()
                    ),
            )
        }
        is Memo.Text -> message
    }

val Memo.message: String
    @Composable get() =
        when (this) {
            is Memo.Resource -> when {
                args.isEmpty() -> stringResource(
                    resource = stringResource,
                )
                else -> stringResource(
                    resource = stringResource,
                    *(
                        args
                            .map { if (it is StringResource) stringResource(it) else it }
                            .toTypedArray()
                        ),
                )
            }
            is Memo.Text -> message
        }
