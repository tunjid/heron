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

package com.tunjid.heron.home

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

actual fun timelinePreferenceDragAndDropTransferData(
    title: String
): DragAndDropTransferData = DragAndDropTransferData(
    transferable = DragAndDropTransferable(
        StringSelection(title)
    ),
    supportedActions = listOf(
        DragAndDropTransferAction.Move,
    ),
)

actual fun DragAndDropEvent.draggedId(): String? {
    return awtTransferable.getTransferData(DataFlavor.stringFlavor) as? String
}

actual fun Modifier.timelinePreferenceDragAndDropSource(
    sourceId: String
): Modifier = this.dragAndDropSource {
    timelinePreferenceDragAndDropTransferData(sourceId)
}
