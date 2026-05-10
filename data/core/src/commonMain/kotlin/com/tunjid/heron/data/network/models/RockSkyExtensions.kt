/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.data.network.models

import app.rocksky.actor.AlbumView
import app.rocksky.actor.ArtistView
import app.rocksky.actor.ScrobbleView
import app.rocksky.actor.TrackView
import com.tunjid.heron.data.core.models.RockSkyAlbum
import com.tunjid.heron.data.core.models.RockSkyArtist
import com.tunjid.heron.data.core.models.RockSkyScrobble
import com.tunjid.heron.data.core.models.RockSkyTrack
import com.tunjid.heron.data.core.types.AlbumId
import com.tunjid.heron.data.core.types.AlbumUri
import com.tunjid.heron.data.core.types.ArtistId
import com.tunjid.heron.data.core.types.ArtistUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ScrobbleId
import com.tunjid.heron.data.core.types.ScrobbleUri
import com.tunjid.heron.data.core.types.TrackId
import com.tunjid.heron.data.core.types.TrackUri

internal fun AlbumView.asExternalModel(): RockSkyAlbum = RockSkyAlbum(
    cid = AlbumId(id),
    uri = AlbumUri(uri.atUri),
    title = title,
    artist = artist,
    releaseDate = releaseDate,
    year = year?.toInt(),
    albumArt = albumArt?.uri?.let(::ImageUri),
    artistUri = ArtistUri(artistUri.atUri),
    playCount = playCount,
    uniqueListeners = uniqueListeners,
    tracks = emptyList(),
    appleMusicLink = appleMusicLink?.uri,
    spotifyLink = spotifyLink?.uri,
    tidalLink = tidalLink?.uri,
    youtubeLink = youtubeLink?.uri,
)

internal fun TrackView.asExternalModel(): RockSkyTrack = RockSkyTrack(
    cid = TrackId(id),
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumArt = albumArt?.uri?.let(::ImageUri),
    trackNumber = trackNumber?.toInt(),
    discNumber = discNumber?.toInt(),
    duration = duration,
    uri = TrackUri(uri.atUri),
    albumUri = albumUri?.atUri?.takeIf(String::isNotEmpty)?.let(::AlbumUri),
    artistUri = artistUri?.atUri?.takeIf(String::isNotEmpty)?.let(::ArtistUri),
    createdAt = createdAt,
    playCount = playCount,
    uniqueListeners = uniqueListeners,
)

internal fun ArtistView.asExternalModel(): RockSkyArtist = RockSkyArtist(
    cid = ArtistId(id),
    name = name,
    picture = picture?.uri?.let(::ImageUri),
    uri = ArtistUri(uri.atUri),
    playCount = playCount,
    uniqueListeners = uniqueListeners,
    tags = tags,
)

internal fun ScrobbleView.asExternalModel(): RockSkyScrobble = RockSkyScrobble(
    cid = ScrobbleId(id),
    trackId = TrackId(trackId),
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumArt = albumArt?.uri?.let(::ImageUri),
    handle = handle?.handle?.let(::ProfileHandle),
    did = ProfileId(did.did),
    avatar = avatar?.uri?.let(::ImageUri),
    uri = ScrobbleUri(uri.atUri),
    trackUri = trackUri?.atUri?.takeIf(String::isNotEmpty)?.let(::TrackUri),
    artistUri = artistUri?.atUri?.takeIf(String::isNotEmpty)?.let(::ArtistUri),
    albumUri = albumUri?.atUri?.takeIf(String::isNotEmpty)?.let(::AlbumUri),
    createdAt = createdAt,
)
