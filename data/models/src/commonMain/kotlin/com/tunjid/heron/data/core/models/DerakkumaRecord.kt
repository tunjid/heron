package com.tunjid.heron.data.core.models

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
import kotlinx.serialization.Serializable

@Serializable
data class DerakkumaProfile(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaProfileUri,
    val playerName: String,
    val title: String,
    val titleRarity: String,
    val rating: Long,
    val stars: Long,
    val comment: String,
    val friendCode: String,
    val profileImage: ImageUri?,
    val ratingPlateImage: ImageUri?,
    val trophyPlateImage: ImageUri?,
    val partnerImage: ImageUri?,
    val courseImage: ImageUri?,
    val classImage: ImageUri?,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaPlay(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaPlayUri,
    val difficulty: String,
    val level: String,
    val type: String,
    val songName: String,
    val artist: String,
    val coverArt: ImageUri?,
    val achievement: String,
    val scoreRank: String,
    val fcStatus: String,
    val syncStatus: String,
    val dxScore: String,
    val trackNum: Long,
    val rating: Long,
    val ratingDelta: Long,
    val playedAt: String,
    val createdAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaBest(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaBestUri,
    val songName: String,
    val difficulty: String,
    val level: String,
    val type: String,
    val artist: String,
    val coverArt: ImageUri?,
    val achievement: String,
    val scoreRank: String,
    val fcStatus: String,
    val syncStatus: String,
    val playCount: Long,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaFriend(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaFriendUri,
    val subject: ProfileId?,
    val displayName: String,
    val title: String,
    val rating: Long,
    val stars: Long,
    val comment: String,
    val icon: ImageUri?,
    val courseImage: ImageUri?,
    val classImage: ImageUri?,
    val favorite: Boolean,
    val rival: Boolean,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaFavoriteSong(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaFavoriteSongUri,
    val songName: String,
    val artist: String,
    val coverArt: ImageUri?,
    val orderId: Long,
    val createdAt: String,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaCircle(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaCircleUri,
    val name: String,
    val comment: String,
    val rank: Long,
    val totalPoints: Long,
    val circleCode: String,
    val ownerName: String,
    val month: String,
    val daysUntilReset: Long,
    val nextRewardPoints: Long,
    val characterImage: ImageUri?,
    val backgroundImage: ImageUri?,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}

@Serializable
data class DerakkumaCircleMember(
    val cid: DerakkumaRecordId,
    val uri: DerakkumaCircleMemberUri,
    val displayName: String,
    val role: String,
    val title: String,
    val rating: Long,
    val status: String,
    val icon: ImageUri?,
    val points: Long,
    val rank: Long,
    val updatedAt: String,
) : Record {
    override val reference = Record.Reference(id = cid, uri = uri)
}
