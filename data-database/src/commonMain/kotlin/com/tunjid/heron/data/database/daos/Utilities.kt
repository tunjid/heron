package com.tunjid.heron.data.database.daos


/**
 * Performs an upsert by first attempting to insert [items] using [insertEntities] with the result
 * of the inserts returned.
 *
 * Items that were not inserted due to conflicts are then updated using [updatePartials].
 */
suspend fun <T, R> partialUpsert(
    items: List<T>,
    partialMapper: (T) -> R,
    insertEntities: suspend (List<T>) -> List<Long>,
    updatePartials: suspend (List<R>) -> Unit,
) {
    val insertResults = insertEntities(items)

    val updateList = items.zip(insertResults)
        .mapNotNull { (item, insertResult) ->
            if (insertResult == -1L) item else null
        }
    if (updateList.isNotEmpty()) updatePartials(updateList.map(partialMapper))
}