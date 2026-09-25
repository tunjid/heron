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

package com.tunjid.heron.data.utilities.writequeue

import com.tunjid.heron.data.core.types.FileUri
import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.tasks.Progress
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered

internal class WriteProgress : AbstractCoroutineContextElement(WriteProgress) {

    private val state = MutableStateFlow(State())

    val updates: Flow<Progress>
        get() = state
            .filter { it.uploads.isNotEmpty() }
            .map(State::progress)
            .distinctUntilChangedBy { (it.fraction * 100).toInt() }
            .conflate()
            .onEach { delay(ReportInterval) }

    fun append(
        file: File,
        sizeInBytes: Long,
        processedRemotely: Boolean,
    ) = state.update {
        it.copy(
            uploads = it.uploads + Pair(
                file.uri,
                if (processedRemotely) Upload.CompleteWhenServerProcessed(
                    sizeInBytes = sizeInBytes.coerceAtLeast(1L),
                )
                else Upload.CompleteWhenUploaded(
                    sizeInBytes = sizeInBytes.coerceAtLeast(1L),
                ),
            ),
        )
    }

    fun onUploaded(
        file: File,
        bytes: Long,
    ) = update(file) {
        when (this) {
            is Upload.CompleteWhenUploaded -> copy(uploadedBytes = bytes)
            is Upload.CompleteWhenServerProcessed -> copy(uploadedBytes = bytes)
        }
    }

    fun onProcessed(
        file: File,
        percent: Long,
    ) = update(file) {
        when (this) {
            is Upload.CompleteWhenUploaded -> this
            is Upload.CompleteWhenServerProcessed -> copy(processedPercent = percent)
        }
    }

    fun onPublished() = state.update {
        it.copy(published = true)
    }

    private fun update(
        file: File,
        block: Upload.() -> Upload,
    ) = state.update { current ->
        val upload = current.uploads[file.uri] ?: return@update current
        current.copy(uploads = current.uploads + Pair(file.uri, upload.block()))
    }

    private sealed class Upload {
        abstract val weight: Long

        abstract val completed: Long

        data class CompleteWhenUploaded(
            val sizeInBytes: Long,
            val uploadedBytes: Long = 0L,
        ) : Upload() {
            override val weight: Long
                get() = sizeInBytes

            override val completed: Long
                get() = uploadedBytes.coerceIn(0L, sizeInBytes)
        }

        data class CompleteWhenServerProcessed(
            val sizeInBytes: Long,
            val uploadedBytes: Long = 0L,
            val processedPercent: Long = 0L,
        ) : Upload() {
            override val weight: Long
                get() = sizeInBytes * 2

            override val completed: Long
                get() = uploadedBytes.coerceIn(0L, sizeInBytes) +
                    sizeInBytes * processedPercent.coerceIn(0L, 100L) / 100
        }
    }

    private data class State(
        val uploads: Map<FileUri, Upload> = emptyMap(),
        val published: Boolean = false,
    ) {
        fun progress(): Progress {
            val total = uploads.values.sumOf(Upload::weight)
            val completed = uploads.values.sumOf(Upload::completed)
            val publishWeight = (total / 100).coerceAtLeast(1L)
            return Progress(
                completedBytes = completed + if (published) publishWeight else 0L,
                totalBytes = total + publishWeight,
            )
        }
    }

    companion object Key : CoroutineContext.Key<WriteProgress>
}

internal suspend fun Source.reportingUploadProgress(
    file: File,
): Source {
    val writeProgress = currentCoroutineContext()[WriteProgress] ?: return this
    return CountingRawSource(
        delegate = this,
        onRead = { bytes ->
            writeProgress.onUploaded(
                file = file,
                bytes = bytes,
            )
        },
    ).buffered()
}

private class CountingRawSource(
    private val delegate: RawSource,
    private val onRead: (Long) -> Unit,
) : RawSource {

    private var totalRead = 0L

    override fun readAtMostTo(
        sink: Buffer,
        byteCount: Long,
    ): Long {
        val read = delegate.readAtMostTo(sink, byteCount)
        if (read > 0L) {
            totalRead += read
            onRead(totalRead)
        }
        return read
    }

    override fun close() = delegate.close()
}

private val ReportInterval = 500.milliseconds
