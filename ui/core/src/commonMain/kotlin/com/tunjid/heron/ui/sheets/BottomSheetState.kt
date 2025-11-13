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

package com.tunjid.heron.ui.sheets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
abstract class BottomSheetState(
    internal val scope: BottomSheetScope,
) {
    abstract fun onHidden()

    fun show() {
        scope.showBottomSheet = true
    }

    fun hide() {
        scope.coroutineScope.launch {
            scope.sheetState.hide()
            scope.showBottomSheet = false
        }
    }
}

@Stable
class BottomSheetScope(
    internal val sheetState: SheetState,
    internal val coroutineScope: CoroutineScope,
) {
    var showBottomSheet by mutableStateOf(false)
        internal set

    companion object Companion {

        @Composable
        fun <T : BottomSheetState> rememberBottomSheetState(
            block: (BottomSheetScope) -> T,
        ): T {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            return remember(sheetState, scope) {
                block(
                    BottomSheetScope(
                        sheetState = sheetState,
                        coroutineScope = scope,
                    ),
                )
            }
        }

        @Composable
        fun BottomSheetState.ModalBottomSheet(
            content: @Composable ColumnScope.() -> Unit,
        ) {
            if (scope.showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = ::hide,
                    sheetState = scope.sheetState,
                    content = content,
                )
                DisposableEffect(Unit) {
                    onDispose(::onHidden)
                }
            }
        }
    }
}
