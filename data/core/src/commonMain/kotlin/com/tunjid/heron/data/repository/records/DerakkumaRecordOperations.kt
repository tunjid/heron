package com.tunjid.heron.data.repository.records

import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.ListRecordsQueryParams
import com.atproto.repo.StrongRef
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
import com.tunjid.heron.data.utilities.safeDecodeAs
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.Blob
import sh.christian.ozone.api.model.JsonContent

interface DerakkumaRecordOperations {
    fun derakkumaProfiles(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaProfile>>
    fun derakkumaPlays(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaPlay>>
    fun derakkumaBests(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaBest>>
    fun derakkumaFriends(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaFriend>>
    fun derakkumaFavoriteSongs(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaFavoriteSong>>
    fun derakkumaCircle(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaCircle>>
    fun derakkumaCircleMembers(query: ProfilesQuery, cursor: Cursor): Flow<CursorList<DerakkumaCircleMember>>
}

internal class RemoteDerakkumaRecordOperations @Inject constructor(
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val pdsResolver: PdsResolver,
    private val savedStateDataSource: SavedStateDataSource,
) : DerakkumaRecordOperations {
    private val resolvedRecordsMutex = Mutex()
    private val resolvedRecords = mutableMapOf<RecordUri, RawDerakkumaRecord>()

    override fun derakkumaProfiles(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaProfileUri.NAMESPACE,
        mapper = ::profile,
    )

    override fun derakkumaPlays(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaPlayUri.NAMESPACE,
        fetchAllPages = true,
    ) { play(it, includeCoverArt = true) }

    override fun derakkumaBests(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaBestUri.NAMESPACE,
    ) { best(it, includeCoverArt = true) }

    override fun derakkumaFriends(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaFriendUri.NAMESPACE,
        mapper = ::friend,
    )

    override fun derakkumaFavoriteSongs(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaFavoriteSongUri.NAMESPACE,
    ) { favoriteSong(it, includeCoverArt = true) }

    override fun derakkumaCircle(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaCircleUri.NAMESPACE,
        mapper = ::circle,
    )

    override fun derakkumaCircleMembers(
        query: ProfilesQuery,
        cursor: Cursor,
    ) = derakkumaRecords(
        query = query,
        cursor = cursor,
        collection = DerakkumaCircleMemberUri.NAMESPACE,
        mapper = ::circleMember,
    )

    private fun <T : Record> derakkumaRecords(
        query: ProfilesQuery,
        cursor: Cursor,
        collection: String,
        fetchAllPages: Boolean = false,
        mapper: suspend (RawDerakkumaRecord) -> T?,
    ): Flow<CursorList<T>> = savedStateDataSource.singleSessionFlow {
        if (cursor is Cursor.Pending || cursor is Cursor.Final) return@singleSessionFlow flowOf(emptyCursorList())
        val rawRecords = mutableListOf<RawDerakkumaRecord>()
        val fetchedCursors = mutableSetOf<String?>()
        var requestCursor = cursor.value
        do {
            if (!fetchedCursors.add(requestCursor)) break
            val response = networkService.runCatchingWithMonitoredNetworkRetry {
                listRecords(
                    ListRecordsQueryParams(
                        repo = Did(query.profileId.id),
                        collection = Nsid(collection),
                        limit = if (fetchAllPages) 100 else query.data.limit.coerceIn(1, 100),
                        cursor = requestCursor,
                        reverse = false,
                    ),
                )
            }.getOrElse { return@singleSessionFlow flowOf(emptyCursorList()) }
            rawRecords += response.records.mapNotNull { record ->
                decodeDerakkumaRecord(
                    collection = collection,
                    uri = record.uri.atUri.asRecordUriOrNull() ?: return@mapNotNull null,
                    cid = DerakkumaRecordId(record.cid.cid),
                    value = record.value,
                )
            }
            requestCursor = response.cursor
        } while (fetchAllPages && requestCursor != null)

        val sortedRecords = rawRecords.sortDerakkumaRecords(collection)
        val nextCursor = if (fetchAllPages) Cursor.Final else requestCursor?.let(Cursor::Next) ?: Cursor.Final

        flow {
            val fastItems = sortedRecords.mapNotNull { it.fastMapper(collection) as? T }
            if (fastItems.isNotEmpty()) {
                emit(CursorList(items = fastItems, nextCursor = nextCursor))
            }
            val metadataItems = sortedRecords.mapAsyncNotNull { raw ->
                raw.metadataMapper(collection) as? T
            }
            if (metadataItems.isNotEmpty()) {
                emit(CursorList(items = metadataItems, nextCursor = nextCursor))
            }
            val enrichedItems = sortedRecords.mapAsyncNotNull(mapper)
            emit(CursorList(items = enrichedItems, nextCursor = nextCursor))
        }
    }.flowOn(ioDispatcher)

    private suspend fun <T> List<RawDerakkumaRecord>.mapAsyncNotNull(
        mapper: suspend (RawDerakkumaRecord) -> T?,
    ): List<T> = chunked(size = 30).flatMap { records ->
        coroutineScope {
            records
                .map { record -> async { mapper(record) } }
                .awaitAll()
                .filterNotNull()
        }
    }

    private suspend fun RawDerakkumaRecord.fastMapper(collection: String): Record? = when (collection) {
        DerakkumaPlayUri.NAMESPACE -> playWithoutRefs(this)
        DerakkumaBestUri.NAMESPACE -> bestWithoutRefs(this)
        DerakkumaFavoriteSongUri.NAMESPACE -> favoriteSongWithoutRefs(this)
        else -> null
    }

    private suspend fun RawDerakkumaRecord.metadataMapper(collection: String): Record? = when (collection) {
        DerakkumaPlayUri.NAMESPACE -> play(this, includeCoverArt = false)
        DerakkumaBestUri.NAMESPACE -> best(this, includeCoverArt = false)
        DerakkumaFavoriteSongUri.NAMESPACE -> favoriteSong(this, includeCoverArt = false)
        else -> null
    }

    private suspend fun profile(raw: RawDerakkumaRecord): DerakkumaProfile? = DerakkumaProfile(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaProfileUri ?: return null,
        playerName = raw.profile?.playerName.orEmpty(),
        title = raw.profile?.title.orEmpty(),
        titleRarity = raw.profile?.titleRarity.orEmpty(),
        rating = raw.profile?.rating ?: 0,
        stars = raw.profile?.stars ?: 0,
        comment = raw.profile?.comment.orEmpty(),
        friendCode = raw.profile?.friendCode.orEmpty(),
        profileImage = raw.blobImage(raw.profile?.profileImage),
        ratingPlateImage = raw.blobImage(raw.profile?.ratingPlateImage),
        trophyPlateImage = raw.blobImage(raw.profile?.trophyPlateImage),
        partnerImage = raw.blobImage(raw.profile?.partnerImage),
        courseImage = raw.blobImage(raw.profile?.courseImage),
        classImage = raw.blobImage(raw.profile?.classImage),
        updatedAt = raw.profile?.updatedAt ?: raw.profile?.createdAt.orEmpty(),
    )

    private suspend fun play(raw: RawDerakkumaRecord, includeCoverArt: Boolean): DerakkumaPlay? {
        val play = raw.play ?: return null
        val chart = raw.resolveRef(play.chart)
        val chartRecord = chart?.chart
        val song = chart?.resolveRef(chartRecord?.song)
        val songRecord = song?.song
        return DerakkumaPlay(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaPlayUri ?: return null,
            chart = play.chart.recordUriOrNull(),
            songName = songRecord?.title ?: chartRecord?.songId ?: play.chartSongName(),
            difficulty = chartRecord?.difficulty ?: play.chartString("difficulty"),
            level = chartRecord?.level ?: play.chartString("level"),
            type = chartRecord?.type.orEmpty(),
            artist = songRecord?.artist.orEmpty(),
            coverArt = if (includeCoverArt) song?.blobImage(songRecord?.coverArt) else null,
            achievement = play.achievement.orEmpty(),
            scoreRank = play.scoreRank.orEmpty(),
            fcStatus = play.fcStatus.orEmpty(),
            syncStatus = play.syncStatus.orEmpty(),
            dxScore = play.dxScore.fractionString(),
            dxStar = play.dxStar ?: 0,
            trackNum = play.trackNum ?: 0,
            rating = play.rating ?: 0,
            ratingDelta = play.ratingDelta ?: 0,
            playedAt = play.playedAt.orEmpty(),
            createdAt = play.createdAt.orEmpty(),
        )
    }

    private fun playWithoutRefs(raw: RawDerakkumaRecord): DerakkumaPlay? {
        val play = raw.play ?: return null
        return DerakkumaPlay(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaPlayUri ?: return null,
            chart = play.chart.recordUriOrNull(),
            songName = play.chartSongName(),
            difficulty = play.chartString("difficulty"),
            level = play.chartString("level"),
            type = play.chartString("type"),
            artist = "",
            coverArt = null,
            achievement = play.achievement.orEmpty(),
            scoreRank = play.scoreRank.orEmpty(),
            fcStatus = play.fcStatus.orEmpty(),
            syncStatus = play.syncStatus.orEmpty(),
            dxScore = play.dxScore.fractionString(),
            dxStar = play.dxStar ?: 0,
            trackNum = play.trackNum ?: 0,
            rating = play.rating ?: 0,
            ratingDelta = play.ratingDelta ?: 0,
            playedAt = play.playedAt.orEmpty(),
            createdAt = play.createdAt.orEmpty(),
        )
    }

    private suspend fun best(raw: RawDerakkumaRecord, includeCoverArt: Boolean): DerakkumaBest? {
        val best = raw.best ?: return null
        val chart = raw.resolveRef(best.chart)
        val chartRecord = chart?.chart
        val song = chart?.resolveRef(chartRecord?.song)
        val songRecord = song?.song
        return DerakkumaBest(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaBestUri ?: return null,
            chart = best.chart.recordUriOrNull(),
            songName = songRecord?.title ?: chartRecord?.songId ?: best.chartSongName(),
            difficulty = chartRecord?.difficulty ?: best.chartString("difficulty"),
            level = chartRecord?.level ?: best.chartString("level"),
            type = chartRecord?.type.orEmpty(),
            artist = songRecord?.artist.orEmpty(),
            coverArt = if (includeCoverArt) song?.blobImage(songRecord?.coverArt) else null,
            achievement = best.achievement.orEmpty(),
            scoreRank = best.scoreRank.orEmpty(),
            fcStatus = best.fcStatus.orEmpty(),
            syncStatus = best.syncStatus.orEmpty(),
            dxScore = best.dxScore.fractionString(),
            dxStar = best.dxStar ?: 0,
            playCount = best.playCount ?: 0,
            lastPlayed = best.lastPlayed.orEmpty(),
            updatedAt = best.updatedAt ?: best.createdAt.orEmpty(),
        )
    }

    private fun bestWithoutRefs(raw: RawDerakkumaRecord): DerakkumaBest? {
        val best = raw.best ?: return null
        return DerakkumaBest(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaBestUri ?: return null,
            chart = best.chart.recordUriOrNull(),
            songName = best.chartSongName(),
            difficulty = best.chartString("difficulty"),
            level = best.chartString("level"),
            type = best.chartString("type"),
            artist = "",
            coverArt = null,
            achievement = best.achievement.orEmpty(),
            scoreRank = best.scoreRank.orEmpty(),
            fcStatus = best.fcStatus.orEmpty(),
            syncStatus = best.syncStatus.orEmpty(),
            dxScore = best.dxScore.fractionString(),
            dxStar = best.dxStar ?: 0,
            playCount = best.playCount ?: 0,
            lastPlayed = best.lastPlayed.orEmpty(),
            updatedAt = best.updatedAt ?: best.createdAt.orEmpty(),
        )
    }

    private suspend fun friend(raw: RawDerakkumaRecord): DerakkumaFriend? = DerakkumaFriend(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaFriendUri ?: return null,
        subject = raw.friend?.subject?.takeIf { it.startsWith("did:") }?.let(::ProfileId),
        displayName = raw.friend?.displayName ?: raw.friend?.name.orEmpty(),
        title = raw.friend?.title.orEmpty(),
        rating = raw.friend?.rating ?: 0,
        stars = raw.friend?.stars ?: 0,
        comment = raw.friend?.comment.orEmpty(),
        icon = raw.blobImage(raw.friend?.icon),
        courseImage = raw.blobImage(raw.friend?.courseImage),
        classImage = raw.blobImage(raw.friend?.classImage),
        favorite = raw.friend?.favorite ?: raw.friend?.isFavorite ?: false,
        rival = raw.friend?.rival ?: raw.friend?.isRival ?: false,
        status = raw.friend?.status.orEmpty(),
        createdAt = raw.friend?.createdAt.orEmpty(),
        updatedAt = raw.friend?.updatedAt ?: raw.friend?.createdAt.orEmpty(),
    )

    private suspend fun favoriteSong(raw: RawDerakkumaRecord, includeCoverArt: Boolean): DerakkumaFavoriteSong? {
        val favoriteSong = raw.favoriteSong ?: return null
        val song = raw.resolveRef(favoriteSong.song)
        val songRecord = song?.song
        return DerakkumaFavoriteSong(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaFavoriteSongUri ?: return null,
            song = favoriteSong.song.recordUriOrNull(),
            songName = songRecord?.title ?: favoriteSong.songName(),
            artist = songRecord?.artist.orEmpty(),
            coverArt = if (includeCoverArt) song?.blobImage(songRecord?.coverArt) else null,
            orderId = favoriteSong.orderId ?: 0,
            observedAt = favoriteSong.observedAt.orEmpty(),
            createdAt = favoriteSong.createdAt.orEmpty(),
            updatedAt = favoriteSong.updatedAt ?: favoriteSong.createdAt.orEmpty(),
        )
    }

    private fun favoriteSongWithoutRefs(raw: RawDerakkumaRecord): DerakkumaFavoriteSong? {
        val favoriteSong = raw.favoriteSong ?: return null
        return DerakkumaFavoriteSong(
            cid = raw.cid,
            uri = raw.uri as? DerakkumaFavoriteSongUri ?: return null,
            song = favoriteSong.song.recordUriOrNull(),
            songName = favoriteSong.songName(),
            artist = "",
            coverArt = null,
            orderId = favoriteSong.orderId ?: 0,
            observedAt = favoriteSong.observedAt.orEmpty(),
            createdAt = favoriteSong.createdAt.orEmpty(),
            updatedAt = favoriteSong.updatedAt ?: favoriteSong.createdAt.orEmpty(),
        )
    }

    private suspend fun circle(raw: RawDerakkumaRecord): DerakkumaCircle? = DerakkumaCircle(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaCircleUri ?: return null,
        name = raw.circle?.name.orEmpty(),
        comment = raw.circle?.comment.orEmpty(),
        rank = raw.circle?.rank ?: 0,
        totalPoints = raw.circle?.totalPoints ?: 0,
        circleCode = raw.circle?.circleCode.orEmpty(),
        ownerName = raw.circle?.ownerName.orEmpty(),
        month = raw.circle?.month.orEmpty(),
        daysUntilReset = raw.circle?.daysUntilReset ?: 0,
        nextRewardPoints = raw.circle?.nextRewardPoints ?: 0,
        characterImage = raw.blobImage(raw.circle?.characterImage),
        backgroundImage = raw.blobImage(raw.circle?.backgroundImage),
        updatedAt = raw.circle?.updatedAt ?: raw.circle?.createdAt.orEmpty(),
    )

    private suspend fun circleMember(raw: RawDerakkumaRecord): DerakkumaCircleMember? = DerakkumaCircleMember(
        cid = raw.cid,
        uri = raw.uri as? DerakkumaCircleMemberUri ?: return null,
        displayName = raw.circleMember?.displayName ?: raw.circleMember?.name.orEmpty(),
        role = raw.circleMember?.role ?: raw.circleMember?.status.orEmpty(),
        title = raw.circleMember?.title.orEmpty(),
        rating = raw.circleMember?.rating ?: 0,
        status = raw.circleMember?.status.orEmpty(),
        icon = raw.blobImage(raw.circleMember?.icon),
        points = raw.circleMember?.points ?: 0,
        rank = raw.circleMember?.rank ?: 0,
        updatedAt = raw.circleMember?.updatedAt ?: raw.circleMember?.createdAt.orEmpty(),
    )

    private suspend fun RawDerakkumaRecord.resolveRef(ref: StrongRef?): RawDerakkumaRecord? {
        val uri = ref?.uri?.atUri?.asRecordUriOrNull() ?: return null
        resolvedRecordsMutex.withLock {
            resolvedRecords[uri]?.let { return it }
        }
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
        val decodedRecord = decodeDerakkumaRecord(
            collection = components.collection,
            uri = uri,
            cid = DerakkumaRecordId(response.cid?.cid ?: ref.cid.cid),
            value = response.value,
        ) ?: return null
        return decodedRecord
            .also { resolved ->
                resolvedRecordsMutex.withLock {
                    resolvedRecords[uri] = resolved
                }
            }
    }

    private suspend fun RawDerakkumaRecord.blobImage(blob: Blob?): ImageUri? = blob.blobImage(uri.profileId(), pdsResolver)
}

private fun decodeDerakkumaRecord(collection: String, uri: RecordUri, cid: DerakkumaRecordId, value: JsonContent): RawDerakkumaRecord? = when (collection) {
    DerakkumaProfileUri.NAMESPACE -> value.safeDecodeAs<DerakkumaProfileRecord>()
    DerakkumaPlayUri.NAMESPACE -> value.safeDecodeAs<DerakkumaPlayRecord>()
    DerakkumaBestUri.NAMESPACE -> value.safeDecodeAs<DerakkumaBestRecord>()
    DerakkumaFriendUri.NAMESPACE -> value.safeDecodeAs<DerakkumaFriendRecord>()
    DerakkumaFavoriteSongUri.NAMESPACE -> value.safeDecodeAs<DerakkumaFavoriteSongRecord>()
    DerakkumaCircleUri.NAMESPACE -> value.safeDecodeAs<DerakkumaCircleRecord>()
    DerakkumaCircleMemberUri.NAMESPACE -> value.safeDecodeAs<DerakkumaCircleMemberRecord>()
    "com.derakkuma.chart" -> value.safeDecodeAs<DerakkumaChartRecord>()
    "com.derakkuma.song" -> value.safeDecodeAs<DerakkumaSongRecord>()
    else -> null
}?.let { RawDerakkumaRecord(uri = uri, cid = cid, value = it) }

private data class RawDerakkumaRecord(val uri: RecordUri, val cid: DerakkumaRecordId, val value: Any)
private data class AtUriComponents(val repo: String, val collection: String, val rkey: String)

private val RawDerakkumaRecord.profile get() = value as? DerakkumaProfileRecord
private val RawDerakkumaRecord.play get() = value as? DerakkumaPlayRecord
private val RawDerakkumaRecord.best get() = value as? DerakkumaBestRecord
private val RawDerakkumaRecord.friend get() = value as? DerakkumaFriendRecord
private val RawDerakkumaRecord.favoriteSong get() = value as? DerakkumaFavoriteSongRecord
private val RawDerakkumaRecord.circle get() = value as? DerakkumaCircleRecord
private val RawDerakkumaRecord.circleMember get() = value as? DerakkumaCircleMemberRecord
private val RawDerakkumaRecord.chart get() = value as? DerakkumaChartRecord
private val RawDerakkumaRecord.song get() = value as? DerakkumaSongRecord

private fun List<RawDerakkumaRecord>.sortDerakkumaRecords(collection: String): List<RawDerakkumaRecord> = when (collection) {
    DerakkumaPlayUri.NAMESPACE -> sortedWith(
        compareByDescending<RawDerakkumaRecord> { it.play?.playedAt.derakkumaDateSortKey() }
            .thenByDescending { it.play?.createdAt.derakkumaDateSortKey() }
            .thenByDescending { it.uri.uri },
    )

    else -> this
}

private fun String?.derakkumaDateSortKey(): Long = this
    ?.let(derakkumaDateRegex::find)
    ?.groupValues
    ?.drop(1)
    ?.map(String::toLongOrNull)
    ?.takeIf { parts -> parts.size == 5 && parts.all { it != null } }
    ?.let { parts ->
        val (year, month, day, hour, minute) = parts.map { it ?: 0 }
        year * 100_000_000L + month * 1_000_000L + day * 10_000L + hour * 100L + minute
    } ?: 0L

private val derakkumaDateRegex = Regex("""(\d{4})\D+(\d{1,2})\D+(\d{1,2})\D+(\d{1,2})\D+(\d{1,2})""")

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

private suspend fun Blob?.blobImage(profileId: com.tunjid.heron.data.core.types.ProfileId, pdsResolver: PdsResolver): ImageUri? {
    val cid = blobCid() ?: return null
    val pdsUrl = pdsResolver.resolve(Did(profileId.id))?.toString()?.trimEnd('/') ?: return null
    return ImageUri("$pdsUrl/xrpc/com.atproto.sync.getBlob?did=${profileId.id}&cid=$cid")
}
private fun Blob?.blobCid(): String? = when (this) {
    is Blob.StandardBlob -> ref.link.toString()
    is Blob.LegacyBlob -> cid
    else -> null
}

private fun DerakkumaPlayRecord.chartString(key: String): String = when (key) {
    "difficulty" -> chartDifficulty
    "level" -> chartLevel
    "type" -> chartType
    else -> null
}.orEmpty()
private fun DerakkumaBestRecord.chartString(key: String): String = when (key) {
    "difficulty" -> chartDifficulty
    "level" -> chartLevel
    "type" -> chartType
    else -> null
}.orEmpty()
private fun DerakkumaPlayRecord.chartSongName(): String = songName ?: chartSongName ?: chartSongId ?: chart?.uri?.atUri ?: "Unknown song"
private fun DerakkumaBestRecord.chartSongName(): String = songName ?: chartSongName ?: chartSongId ?: chart?.uri?.atUri ?: "Unknown song"
private fun DerakkumaFavoriteSongRecord.songName(): String = songName ?: name ?: song?.uri?.atUri ?: "Unknown song"
private fun StrongRef?.recordUriOrNull(): RecordUri? = this?.uri?.atUri?.asRecordUriOrNull()
private fun DerakkumaDxScoreRecord?.fractionString(): String = this?.let { score -> listOf(score.achieved, score.total).takeIf { it.any { n -> n > 0 } }?.joinToString("/") }.orEmpty()

@Serializable
private data class DerakkumaProfileRecord(
    val playerName: String? = null,
    val title: String? = null,
    val titleRarity: String? = null,
    val rating: Long? = null,
    val stars: Long? = null,
    val comment: String? = null,
    val friendCode: String? = null,
    val profileImage: Blob? = null,
    val ratingPlateImage: Blob? = null,
    val trophyPlateImage: Blob? = null,
    val partnerImage: Blob? = null,
    val courseImage: Blob? = null,
    val classImage: Blob? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaPlayRecord(
    val chart: StrongRef? = null,
    val songName: String? = null,
    val chartSongName: String? = null,
    val chartSongId: String? = null,
    val chartDifficulty: String? = null,
    val chartLevel: String? = null,
    val chartType: String? = null,
    val achievement: String? = null,
    val scoreRank: String? = null,
    val fcStatus: String? = null,
    val syncStatus: String? = null,
    val dxScore: DerakkumaDxScoreRecord? = null,
    val dxStar: Long? = null,
    val trackNum: Long? = null,
    val rating: Long? = null,
    val ratingDelta: Long? = null,
    val playedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
private data class DerakkumaBestRecord(
    val chart: StrongRef? = null,
    val songName: String? = null,
    val chartSongName: String? = null,
    val chartSongId: String? = null,
    val chartDifficulty: String? = null,
    val chartLevel: String? = null,
    val chartType: String? = null,
    val achievement: String? = null,
    val scoreRank: String? = null,
    val fcStatus: String? = null,
    val syncStatus: String? = null,
    val dxScore: DerakkumaDxScoreRecord? = null,
    val dxStar: Long? = null,
    val playCount: Long? = null,
    val lastPlayed: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaFriendRecord(
    val subject: String? = null,
    val displayName: String? = null,
    val name: String? = null,
    val title: String? = null,
    val rating: Long? = null,
    val stars: Long? = null,
    val comment: String? = null,
    val icon: Blob? = null,
    val courseImage: Blob? = null,
    val classImage: Blob? = null,
    val favorite: Boolean? = null,
    val isFavorite: Boolean? = null,
    val rival: Boolean? = null,
    val isRival: Boolean? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaFavoriteSongRecord(
    val song: StrongRef? = null,
    val songName: String? = null,
    val name: String? = null,
    val orderId: Long? = null,
    val observedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaCircleRecord(
    val name: String? = null,
    val comment: String? = null,
    val rank: Long? = null,
    val totalPoints: Long? = null,
    val circleCode: String? = null,
    val ownerName: String? = null,
    val month: String? = null,
    val daysUntilReset: Long? = null,
    val nextRewardPoints: Long? = null,
    val characterImage: Blob? = null,
    val backgroundImage: Blob? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaCircleMemberRecord(
    val displayName: String? = null,
    val name: String? = null,
    val role: String? = null,
    val title: String? = null,
    val rating: Long? = null,
    val status: String? = null,
    val icon: Blob? = null,
    val points: Long? = null,
    val rank: Long? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
private data class DerakkumaChartRecord(
    val song: StrongRef? = null,
    val songId: String? = null,
    val type: String? = null,
    val difficulty: String? = null,
    val level: String? = null,
)

@Serializable
private data class DerakkumaSongRecord(
    val title: String? = null,
    val artist: String? = null,
    val coverArt: Blob? = null,
)

@Serializable
private data class DerakkumaDxScoreRecord(
    val achieved: Long = 0,
    val total: Long = 0,
)
