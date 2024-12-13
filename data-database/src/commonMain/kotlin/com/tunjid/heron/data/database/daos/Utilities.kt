package com.tunjid.heron.data.database.daos


/**
 * Performs an upsert by first attempting to insert [items] using [insertMany] with the the result
 * of the inserts returned.
 *
 * Items that were not inserted due to conflicts are then updated using [updateMany]
 */
suspend fun <T, R> upsert(
    items: List<T>,
    entityMapper: (T) -> R,
    insertMany: suspend (List<T>) -> List<Long>,
    updateMany: suspend (List<R>) -> Unit,
) {
    val insertResults = insertMany(items)

    val updateList = items.zip(insertResults)
        .mapNotNull { (item, insertResult) ->
            if (insertResult == -1L) item else null
        }
    if (updateList.isNotEmpty()) updateMany(updateList.map(entityMapper))
}