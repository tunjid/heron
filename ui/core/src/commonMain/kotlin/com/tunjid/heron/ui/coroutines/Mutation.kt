package com.tunjid.heron.ui.coroutines

import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

context(scope: CoroutineScope)
inline fun <T> Flow<T>.launchAndCollect(crossinline block: suspend (T) -> Unit) {
    scope.launch {
        collect {
            block(it)
        }
    }
}

context(scope: CoroutineScope)
inline fun <T, S> Flow<T>.launchAndCollectWithState(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    scope.launch {
        collectWithState(state, block)
    }
}

suspend inline fun <T, S> Flow<T>.collectWithState(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    collect { emission ->
        with(state) {
            block(emission)
        }
    }
}

context(scope: CoroutineScope)
inline fun <T> Flow<T>.launchAndCollectLatest(crossinline block: suspend (T) -> Unit) {
    scope.launch {
        collectLatest {
            block(it)
        }
    }
}

context(scope: CoroutineScope)
inline fun <T, S> Flow<T>.launchAndCollectLatestWithState(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    scope.launch {
        collectLatestWithState(state, block)
    }
}

suspend inline fun <T, S> Flow<T>.collectLatestWithState(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    collectLatest { emission ->
        with(state) {
            block(emission)
        }
    }
}

fun <Action : Any, State : Any> ActionSuspendingStateMutator<Action, State>?.isNoOp() =
    this == null || this is NoOpActionSuspendingStateMutator

fun <Action : Any, State : Any> noOpActionSuspendingStateMutator(
    state: State
): ActionSuspendingStateMutator<Action, State> = NoOpActionSuspendingStateMutator(state)

private class NoOpActionSuspendingStateMutator<Action : Any, State : Any>(
    override val state: State
) : ActionSuspendingStateMutator<Action, State> {
    override val accept: (Action) -> Unit = {}

    override suspend fun collect() = Unit
}
