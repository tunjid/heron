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

import android.content.ClipData
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent

actual fun timelineEditDragAndDropTransferData(title: String): DragAndDropTransferData =
    DragAndDropTransferData(
        clipData = ClipData.newPlainText("Drag timeline", title),
        localState = title,
    )

actual fun DragAndDropEvent.draggedId(): String? = toAndroidDragEvent().localState as? String

@Suppress("DEPRECATION")
actual fun Modifier.timelineEditDragAndDropSource(sourceId: String): Modifier =
    dragAndDropSource(
        block = {
            detectDragGesturesAfterLongPress(
                onDragStart = { startTransfer(timelineEditDragAndDropTransferData(sourceId)) },
                onDrag = { _, _ -> },
            )
        }
    )
