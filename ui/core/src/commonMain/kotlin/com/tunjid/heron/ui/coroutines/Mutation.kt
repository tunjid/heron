package com.tunjid.heron.ui.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

context(scope: CoroutineScope)
inline fun <T, S> Flow<T>.launchAndFoldMutations(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    scope.launch {
        foldMutations(state, block)
    }
}

suspend inline fun <T, S> Flow<T>.foldMutations(
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
inline fun <T, S> Flow<T>.launchAndFoldLatestMutations(
    state: S,
    crossinline block: suspend S.(T) -> Unit,
) {
    scope.launch {
        foldLatestMutations(state, block)
    }
}

suspend inline fun <T, S> Flow<T>.foldLatestMutations(
    state: S,
    crossinline  block: suspend S.(T) -> Unit,
) {
    collectLatest { emission ->
        with(state) {
            block(emission)
        }
    }
}
