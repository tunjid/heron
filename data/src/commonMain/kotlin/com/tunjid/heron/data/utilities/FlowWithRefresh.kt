package com.tunjid.heron.data.utilities

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * Returns this [Flow] [merge] with a [Flow] that invokes [refresh] and completes.
 */
internal fun <T> Flow<T>.withRefresh(
    refresh: suspend () -> Unit
) = merge(
    this,
    flow { refresh() }
)