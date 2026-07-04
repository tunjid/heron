package com.tunjid.heron.data.utilities.taskstore

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.tasks.FailedTask
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TaskStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SavedStateTaskStore(
    private val savedStateDataSource: SavedStateDataSource,
) : TaskStore {

    override val pending: Flow<List<Task>>
        get() = savedStateDataSource.savedState
            .map { it.tasks.pending }
            .distinctUntilChanged()

    override val failed: Flow<List<FailedTask>>
        get() = savedStateDataSource.savedState
            .map { it.tasks.failed }
            .distinctUntilChanged()

    override suspend fun add(
        task: Task,
    ): Boolean {
        var added = false
        savedStateDataSource.updateTasks {
            when {
                pending.any { it.id == task.id } -> this
                else -> {
                    added = true
                    copy(pending = pending + task)
                }
            }
        }
        return added
    }

    override suspend fun remove(
        id: TaskId,
    ) = savedStateDataSource.updateTasks {
        copy(
            pending = pending.filterNot { it.id == id },
            failed = failed.filterNot { it.task.id == id },
        )
    }

    override suspend fun markFailed(
        id: TaskId,
        reason: String?,
    ) = savedStateDataSource.updateTasks {
        when (val task = pending.firstOrNull { it.id == id }) {
            null -> this
            else -> copy(
                pending = pending.filterNot { it.id == id },
                failed = (
                    failed.filterNot { it.task.id == id } + FailedTask(
                        task = task,
                        reason = reason,
                    )
                    ).takeLast(MaximumFailedTasks),
            )
        }
    }
}

private suspend inline fun SavedStateDataSource.updateWrites(
    crossinline block: SavedState.Writes.(signedInProfileId: ProfileId?) -> SavedState.Writes,
) {
    updateSignedInProfileData { signedInProfileId ->
        copy(writes = writes.block(signedInProfileId))
    }
}
private const val MaximumFailedTasks = 10
