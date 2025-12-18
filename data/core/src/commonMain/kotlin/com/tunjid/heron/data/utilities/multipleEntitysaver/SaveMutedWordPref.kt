package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.actor.PreferencesUnion
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.MutedWordId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.preferences.MutedWordEntity
import kotlin.time.Instant

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId,
    mutedWordsPref: PreferencesUnion.MutedWordsPref,
) {
    mutedWordsPref.value.items.forEach { mutedWord ->
        val targets = mutedWord.targets.map {
            MutedWordPreference.Target(it.value)
        }

        val actorTarget = mutedWord.actorTarget?.let {
            MutedWordPreference.Target(it.value)
        }

        val preference = MutedWordPreference(
            value = mutedWord.value,
            targets = MutedWordPreference.TargetsList(targets),
            actorTarget = actorTarget,
            expiresAt = mutedWord.expiresAt,
        )

        mutedWord.id?.let { id ->
            val entity = preference.toEntity(
                viewingProfileId = viewingProfileId,
                id = id,
            )
            add(entity)
        }
    }
}

fun MutedWordPreference.toEntity(
    viewingProfileId: ProfileId,
    id: String,
): MutedWordEntity {
    val targetsCbor = targets.toUrlEncodedBase64()

    val actorTargetCbor = actorTarget?.toUrlEncodedBase64()

    return MutedWordEntity(
        id = MutedWordId(id),
        value = value,
        viewingProfileId = viewingProfileId,
        targetsCbor = targetsCbor,
        actorTargetCbor = actorTargetCbor,
        expiresAt = expiresAt ?: Instant.DISTANT_FUTURE,
    )
}
