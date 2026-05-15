package com.tunjid.heron.data.utilities.cursorQueryRefreshTracker

import com.tunjid.heron.data.repository.ProfilesQuery

internal fun ProfilesQuery.albumsIdentity(): String =
    "rocksky.albums:${profileId.id}"

internal fun ProfilesQuery.artistsIdentity(): String =
    "rocksky.artists:${profileId.id}"

internal fun ProfilesQuery.tracksIdentity(): String =
    "rocksky.tracks:${profileId.id}"

internal fun ProfilesQuery.scrobblesIdentity(): String =
    "rocksky.scrobbles:${profileId.id}"
