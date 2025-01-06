package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewBasic
import app.bsky.actor.ProfileViewDetailed
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.profileProfileRelationshipsEntities

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    profileView: ProfileViewBasic,
) {
    add(profileView.profileEntity())
    profileView.profileProfileRelationshipsEntities(
        viewingProfileId = viewingProfileId,
    ).forEach(::add)
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    profileView: ProfileView,
) {
    add(profileView.profileEntity())
    profileView.profileProfileRelationshipsEntities(
        viewingProfileId = viewingProfileId,
    ).forEach(::add)
}

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    profileView: ProfileViewDetailed,
) {
    add(profileView.profileEntity())
    profileView.profileProfileRelationshipsEntities(
        viewingProfileId = viewingProfileId,
    ).forEach(::add)
}