package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.mapToManyMutations
import kotlinx.coroutines.flow.Flow

interface MutedWordUpdateAction {
    val mutedWordPreference: List<MutedWordPreference>
}

fun <T, S> Flow<T>.updateMutedWordMutations(
    writeQueue: WriteQueue,
): Flow<Mutation<S>> where T : MutedWordUpdateAction = mapToManyMutations {
    writeQueue.enqueue(
        Writable.TimelineUpdate(
            Timeline.Update.OfMutedWord.ReplaceAll(
                mutedWordPreferences = it.mutedWordPreference,
            ),
        ),
    )
}
