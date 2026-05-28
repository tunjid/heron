package com.tunjid.heron.data.repository.records

import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.ListRecordsQueryParams
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.DerakkumaBest
import com.tunjid.heron.data.core.models.DerakkumaCircle
import com.tunjid.heron.data.core.models.DerakkumaCircleMember
import com.tunjid.heron.data.core.models.DerakkumaFavoriteSong
import com.tunjid.heron.data.core.models.DerakkumaFriend
import com.tunjid.heron.data.core.models.DerakkumaPlay
import com.tunjid.heron.data.core.models.DerakkumaProfile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.emptyCursorList
import com.tunjid.heron.data.core.models.value
import com.tunjid.heron.data.core.types.DerakkumaBestUri
import com.tunjid.heron.data.core.types.DerakkumaCircleMemberUri
import com.tunjid.heron.data.core.types.DerakkumaCircleUri
import com.tunjid.heron.data.core.types.DerakkumaFavoriteSongUri
import com.tunjid.heron.data.core.types.DerakkumaFriendUri
import com.tunjid.heron.data.core.types.DerakkumaPlayUri
import com.tunjid.heron.data.core.types.DerakkumaProfileUri
import com.tunjid.heron.data.core.types.DerakkumaRecordId
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.di.IODispatcher
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.network.PdsResolver
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.repository.SavedStateDataSource
import com.tunjid.heron.data.repository.singleSessionFlow
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

interface DerakkumaRecordOperations {
    fun derakkumaProfiles(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaProfile>>
    fun derakkumaPlays(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaPlay>>
    fun derakkumaBests(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaBest>>
    fun derakkumaFriends(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaFriend>>
    fun derakkumaFavoriteSongs(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaFavoriteSong>>
    fun derakkumaCircle(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaCircle>>
    fun derakkumaCircleMembers(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaCircleMember>>
}

internal class OfflineFirstDerakkumaRecordOperations @Inject constructor(
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val pdsResolver: PdsResolver,
    private val savedStateDataSource: SavedStateDataSource,
) : DerakkumaRecordOperations {
    override fun derakkumaProfiles(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaProfileUri.NAMESPACE, ::profile)
    override fun derakkumaPlays(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaPlayUri.NAMESPACE, ::play)
    override fun derakkumaBests(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaBestUri.NAMESPACE, ::best)
    override fun derakkumaFriends(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaFriendUri.NAMESPACE, ::friend)
    override fun derakkumaFavoriteSongs(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaFavoriteSongUri.NAMESPACE, ::favoriteSong)
    override fun derakkumaCircle(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaCircleUri.NAMESPACE, ::circle)
    override fun derakkumaCircleMembers(query: ProfilesQuery, cursor: Cursor) = derakkumaRecords(query, cursor, DerakkumaCircleMemberUri.NAMESPACE, ::circleMember)

    private fun <T : Record> derakkumaRecords(query: ProfilesQuery, cursor: Cursor, collection: String, mapper: suspend (RawDerakkumaRecord) -> T?): Flow<CursorList<T>> = savedStateDataSource.singleSessionFlow {
        if (cursor is Cursor.Pending || cursor is Cursor.Final) return@singleSessionFlow flowOf(emptyCursorList())
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            listRecords(ListRecordsQueryParams(repo = Did(query.profileId.id), collection = Nsid(collection), limit = query.data.limit.coerceIn(1, 100), cursor = cursor.value, reverse = true))
        }.getOrElse { return@singleSessionFlow flowOf(emptyCursorList()) }

        flow {
            val rawRecords = response.records.mapNotNull { record ->
                RawDerakkumaRecord(
                    uri = record.uri.atUri.asRecordUriOrNull() ?: return@mapNotNull null,
                    cid = DerakkumaRecordId(record.cid.cid),
                    value = record.value.decodeAs<JsonObject>(),
                )
            }
            val nextCursor = response.cursor?.let(Cursor::Next) ?: Cursor.Final
            val fastItems = rawRecords.mapNotNull { it.fastMapper(collection) as? T }
            if (fastItems.isNotEmpty()) {
                emit(CursorList(items = fastItems, nextCursor = nextCursor))
            }
            val enrichedItems = coroutineScope {
                rawRecords.map { raw -> async { mapper(raw) } }.awaitAll().filterNotNull()
            }
            emit(CursorList(items = enrichedItems, nextCursor = nextCursor))
        }
    }.flowOn(ioDispatcher)

    private suspend fun RawDerakkumaRecord.fastMapper(collection: String): Record? = when (collection) {
        DerakkumaPlayUri.NAMESPACE -> playWithoutRefs(this)
        DerakkumaBestUri.NAMESPACE -> bestWithoutRefs(this)
        DerakkumaFavoriteSongUri.NAMESPACE -> favoriteSongWithoutRefs(this)
        else -> null
    }

    private suspend fun profile(raw: RawDerakkumaRecord): DerakkumaProfile? = DerakkumaProfile(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaProfileUri ?: return null,
        playerName = raw.string("playerName"),
        title = raw.string("title"),
        titleRarity = raw.string("titleRarity"),
        rating = raw.long("rating"),
        stars = raw.long("stars"),
        comment = raw.string("comment"),
        friendCode = raw.string("friendCode"),
        profileImage = raw.blobImage("profileImage"),
        ratingPlateImage = raw.blobImage("ratingPlateImage"),
        trophyPlateImage = raw.blobImage("trophyPlateImage"),
        partnerImage = raw.blobImage("partnerImage"),
        courseImage = raw.blobImage("courseImage"),
        classImage = raw.blobImage("classImage"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun play(raw: RawDerakkumaRecord): DerakkumaPlay? {
        val chart = raw.resolveRef("chart")
        val song = chart?.resolveRef("song")
        return DerakkumaPlay(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaPlayUri ?: return null,
            songName = song?.string("title") ?: chart?.string("songId") ?: raw.chartSongName(),
            difficulty = chart?.string("difficulty") ?: raw.chartString("difficulty"),
            level = chart?.string("level") ?: raw.chartString("level"),
            type = chart?.string("type").orEmpty(),
            artist = song?.string("artist").orEmpty(),
            coverArt = song?.blobImage("coverArt"),
            achievement = raw.string("achievement"),
            scoreRank = raw.string("scoreRank"),
            fcStatus = raw.string("fcStatus"),
            syncStatus = raw.string("syncStatus"),
            dxScore = raw.value["dxScore"].fractionString(),
            trackNum = raw.long("trackNum"),
            rating = raw.long("rating"),
            ratingDelta = raw.long("ratingDelta"),
            playedAt = raw.string("playedAt"),
            createdAt = raw.string("createdAt"),
        )
    }

    private fun playWithoutRefs(raw: RawDerakkumaRecord): DerakkumaPlay? = DerakkumaPlay(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaPlayUri ?: return null,
        songName = raw.chartSongName(),
        difficulty = raw.chartString("difficulty"),
        level = raw.chartString("level"),
        type = raw.chartString("type"),
        artist = "",
        coverArt = null,
        achievement = raw.string("achievement"),
        scoreRank = raw.string("scoreRank"),
        fcStatus = raw.string("fcStatus"),
        syncStatus = raw.string("syncStatus"),
        dxScore = raw.value["dxScore"].fractionString(),
        trackNum = raw.long("trackNum"),
        rating = raw.long("rating"),
        ratingDelta = raw.long("ratingDelta"),
        playedAt = raw.string("playedAt"),
        createdAt = raw.string("createdAt"),
    )

    private suspend fun best(raw: RawDerakkumaRecord): DerakkumaBest? {
        val chart = raw.resolveRef("chart")
        val song = chart?.resolveRef("song")
        return DerakkumaBest(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaBestUri ?: return null,
            songName = song?.string("title") ?: chart?.string("songId") ?: raw.chartSongName(),
            difficulty = chart?.string("difficulty") ?: raw.chartString("difficulty"),
            level = chart?.string("level") ?: raw.chartString("level"),
            type = chart?.string("type").orEmpty(),
            artist = song?.string("artist").orEmpty(),
            coverArt = song?.blobImage("coverArt"),
            achievement = raw.string("achievement"),
            scoreRank = raw.string("scoreRank"),
            fcStatus = raw.string("fcStatus"),
            syncStatus = raw.string("syncStatus"),
            playCount = raw.long("playCount"),
            updatedAt = raw.string("updatedAt", raw.string("createdAt")),
        )
    }

    private fun bestWithoutRefs(raw: RawDerakkumaRecord): DerakkumaBest? = DerakkumaBest(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaBestUri ?: return null,
        songName = raw.chartSongName(),
        difficulty = raw.chartString("difficulty"),
        level = raw.chartString("level"),
        type = raw.chartString("type"),
        artist = "",
        coverArt = null,
        achievement = raw.string("achievement"),
        scoreRank = raw.string("scoreRank"),
        fcStatus = raw.string("fcStatus"),
        syncStatus = raw.string("syncStatus"),
        playCount = raw.long("playCount"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun friend(raw: RawDerakkumaRecord): DerakkumaFriend? = DerakkumaFriend(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaFriendUri ?: return null,
        subject = raw.string("subject").takeIf { it.startsWith("did:") }?.let(::ProfileId),
        displayName = raw.string("displayName", raw.string("name")),
        title = raw.string("title"),
        rating = raw.long("rating"),
        stars = raw.long("stars"),
        comment = raw.string("comment"),
        icon = raw.blobImage("icon"),
        courseImage = raw.blobImage("courseImage"),
        classImage = raw.blobImage("classImage"),
        favorite = raw.boolean("favorite", raw.boolean("isFavorite")),
        rival = raw.boolean("rival", raw.boolean("isRival")),
        status = raw.string("status"),
        createdAt = raw.string("createdAt"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun favoriteSong(raw: RawDerakkumaRecord): DerakkumaFavoriteSong? {
        val song = raw.resolveRef("song")
        return DerakkumaFavoriteSong(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaFavoriteSongUri ?: return null,
            songName = song?.string("title") ?: raw.songName(),
            artist = song?.string("artist").orEmpty(),
            coverArt = song?.blobImage("coverArt"),
            orderId = raw.long("orderId"),
            createdAt = raw.string("createdAt"),
            updatedAt = raw.string("updatedAt", raw.string("createdAt")),
        )
    }

    private fun favoriteSongWithoutRefs(raw: RawDerakkumaRecord): DerakkumaFavoriteSong? = DerakkumaFavoriteSong(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaFavoriteSongUri ?: return null,
        songName = raw.songName(),
        artist = "",
        coverArt = null,
        orderId = raw.long("orderId"),
        createdAt = raw.string("createdAt"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun circle(raw: RawDerakkumaRecord): DerakkumaCircle? = DerakkumaCircle(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaCircleUri ?: return null,
        name = raw.string("name"),
        comment = raw.string("comment"),
        rank = raw.long("rank"),
        totalPoints = raw.long("totalPoints"),
        circleCode = raw.string("circleCode"),
        ownerName = raw.string("ownerName"),
        month = raw.string("month"),
        daysUntilReset = raw.long("daysUntilReset"),
        nextRewardPoints = raw.long("nextRewardPoints"),
        characterImage = raw.blobImage("characterImage"),
        backgroundImage = raw.blobImage("backgroundImage"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun circleMember(raw: RawDerakkumaRecord): DerakkumaCircleMember? = DerakkumaCircleMember(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaCircleMemberUri ?: return null,
        displayName = raw.string("displayName", raw.string("name")),
        role = raw.string("role", raw.string("status")),
        title = raw.string("title"),
        rating = raw.long("rating"),
        status = raw.string("status"),
        icon = raw.blobImage("icon"),
        points = raw.long("points"),
        rank = raw.long("rank"),
        updatedAt = raw.string("updatedAt", raw.string("createdAt")),
    )

    private suspend fun RawDerakkumaRecord.resolveRef(key: String): RawDerakkumaRecord? {
        val uri = value[key]?.objectOrNull()?.string("uri")?.asRecordUriOrNull() ?: return null
        val components = uri.rawAtUriComponents() ?: return null
        val response = networkService.runCatchingWithMonitoredNetworkRetry {
            getRecord(
                GetRecordQueryParams(
                    repo = Did(components.repo),
                    collection = Nsid(components.collection),
                    rkey = RKey(components.rkey),
                ),
            )
        }.getOrNull() ?: return null
        return RawDerakkumaRecord(uri, DerakkumaRecordId(response.cid?.cid.orEmpty()), response.value.decodeAs<JsonObject>())
    }

    private suspend fun RawDerakkumaRecord.blobImage(key: String): ImageUri? = value.blobImage(key, uri.profileId(), pdsResolver)
}

private data class RawDerakkumaRecord(val uri: RecordUri, val cid: DerakkumaRecordId, val value: JsonObject)
private data class AtUriComponents(val repo: String, val collection: String, val rkey: String)

private fun RecordUri.rawAtUriComponents(): AtUriComponents? {
    val withoutPrefix = uri.removePrefix("at://")
    val firstSlash = withoutPrefix.indexOf('/')
    if (firstSlash <= 0) return null
    val secondSlash = withoutPrefix.indexOf('/', startIndex = firstSlash + 1)
    if (secondSlash <= firstSlash + 1 || secondSlash >= withoutPrefix.lastIndex) return null
    return AtUriComponents(
        repo = withoutPrefix.substring(0, firstSlash),
        collection = withoutPrefix.substring(firstSlash + 1, secondSlash),
        rkey = withoutPrefix.substring(secondSlash + 1),
    )
}

private suspend fun JsonObject.blobImage(key: String, profileId: com.tunjid.heron.data.core.types.ProfileId, pdsResolver: PdsResolver): ImageUri? {
    val cid = this[key]?.blobCid() ?: return null
    val pdsUrl = pdsResolver.resolve(Did(profileId.id))?.toString()?.trimEnd('/') ?: return null
    return ImageUri("$pdsUrl/xrpc/com.atproto.sync.getBlob?did=${profileId.id}&cid=$cid")
}
private fun JsonElement?.blobCid(): String? = when (this) {
    is JsonObject -> string("ref") ?: (this["ref"] as? JsonObject)?.string("$" + "link") ?: string("cid")
    else -> null
}
private fun RawDerakkumaRecord.string(key: String, default: String = ""): String = value[key].stringOrNull() ?: default
private fun RawDerakkumaRecord.long(key: String): Long = value[key].longOrNull() ?: 0
private fun RawDerakkumaRecord.boolean(key: String, default: Boolean = false): Boolean = value[key]?.jsonPrimitive?.booleanOrNull ?: default
private fun RawDerakkumaRecord.chartString(key: String): String = value["chart"]?.objectOrNull()?.string(key).orEmpty()
private fun RawDerakkumaRecord.chartSongName(): String = value["songName"].stringOrNull() ?: value["chart"]?.objectOrNull()?.string("songName") ?: value["chart"]?.objectOrNull()?.string("songId") ?: value["chart"]?.objectOrNull()?.string("uri") ?: "Unknown song"
private fun RawDerakkumaRecord.songName(): String = value["songName"].stringOrNull() ?: value["name"].stringOrNull() ?: value["song"]?.objectOrNull()?.string("title") ?: value["song"]?.objectOrNull()?.string("songId") ?: value["song"]?.objectOrNull()?.string("uri") ?: "Unknown song"
private fun JsonElement?.stringOrNull(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> string("uri") ?: string("cid") ?: string("$" + "link")
    is JsonArray -> joinToString { it.stringOrNull().orEmpty() }.takeIf(String::isNotBlank)
    else -> null
}
private fun JsonElement?.longOrNull(): Long? = stringOrNull()?.toLongOrNull()
private fun JsonElement?.fractionString(): String = objectOrNull()?.let { obj -> listOf(obj.long("achieved"), obj.long("total")).takeIf { it.any { n -> n > 0 } }?.joinToString("/") }.orEmpty()
private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject
private fun JsonObject.string(key: String): String? = this[key].stringOrNull()
private fun JsonObject.long(key: String): Long = this[key].longOrNull() ?: 0
