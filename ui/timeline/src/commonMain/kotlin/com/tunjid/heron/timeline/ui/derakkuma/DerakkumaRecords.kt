package com.tunjid.heron.timeline.ui.derakkuma

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.DerakkumaBest
import com.tunjid.heron.data.core.models.DerakkumaCircle
import com.tunjid.heron.data.core.models.DerakkumaCircleMember
import com.tunjid.heron.data.core.models.DerakkumaFavoriteSong
import com.tunjid.heron.data.core.models.DerakkumaFriend
import com.tunjid.heron.data.core.models.DerakkumaPlay
import com.tunjid.heron.data.core.models.DerakkumaProfile
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.derakkuma_best_subtitle
import heron.ui.timeline.generated.resources.derakkuma_circle_member_subtitle
import heron.ui.timeline.generated.resources.derakkuma_circle_subtitle
import heron.ui.timeline.generated.resources.derakkuma_favorite_subtitle
import heron.ui.timeline.generated.resources.derakkuma_friend_subtitle
import heron.ui.timeline.generated.resources.derakkuma_play_subtitle
import heron.ui.timeline.generated.resources.derakkuma_profile_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun DerakkumaProfile(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    profile: DerakkumaProfile,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = profile.playerName,
        subtitle = stringResource(Res.string.derakkuma_profile_subtitle),
        description = null,
        blurb = null,
        image = profile.profileImage,
        secondaryImages = listOf(profile.ratingPlateImage, profile.trophyPlateImage, profile.courseImage, profile.classImage, profile.partnerImage),
        sharedElementPrefix = sharedElementPrefix,
        uri = profile.uri,
        extraContent = {
            DerakkumaProfileDetails(profile)
        },
    )
}

@Composable
fun DerakkumaPlay(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    play: DerakkumaPlay,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = play.songName,
        subtitle = stringResource(Res.string.derakkuma_play_subtitle, play.difficulty, play.level, play.achievement),
        description = listOf(play.artist, play.scoreRank.uppercase(), play.fcStatus.uppercase(), play.syncStatus.uppercase(), play.dxScore.takeIf(String::isNotBlank)?.let { "DX $it" }).filterNotNull().filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = play.playedAt.ifBlank { play.createdAt },
        image = play.coverArt,
        sharedElementPrefix = sharedElementPrefix,
        uri = play.uri,
    )
}

@Composable
fun DerakkumaBest(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    best: DerakkumaBest,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = best.songName,
        subtitle = stringResource(Res.string.derakkuma_best_subtitle, best.difficulty, best.level, best.achievement),
        description = listOf(best.artist, best.scoreRank.uppercase(), best.fcStatus.uppercase(), best.syncStatus.uppercase()).filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = if (best.playCount > 0) "${best.playCount} plays" else best.updatedAt,
        image = best.coverArt,
        sharedElementPrefix = sharedElementPrefix,
        uri = best.uri,
    )
}

@Composable
fun DerakkumaFriend(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    friend: DerakkumaFriend,
    onProfileClick: (Profile, String?) -> Unit = { _, _ -> },
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = friend.displayName,
        subtitle = stringResource(Res.string.derakkuma_friend_subtitle, friend.rating),
        description = listOf(friend.title, friend.comment, friend.status).filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = listOfNotNull("Rating ${friend.rating}".takeIf { friend.rating > 0 }, "★×${friend.stars}".takeIf { friend.stars > 0 }, "Favorite".takeIf { friend.favorite }, "Rival".takeIf { friend.rival }).joinToString(" · ").ifBlank { null },
        image = friend.icon,
        secondaryImages = listOf(friend.courseImage, friend.classImage),
        onClick = friend.subject?.let { subject ->
            {
                onProfileClick(
                    stubProfile(
                        did = subject,
                        handle = ProfileHandle(subject.id),
                        displayName = friend.displayName.ifBlank { null },
                        avatar = friend.icon,
                    ),
                    friend.uri.avatarSharedElementKey(sharedElementPrefix),
                )
            }
        },
        sharedElementPrefix = sharedElementPrefix,
        uri = friend.uri,
    )
}

@Composable
fun DerakkumaFavoriteSong(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    favoriteSong: DerakkumaFavoriteSong,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = favoriteSong.songName,
        subtitle = stringResource(Res.string.derakkuma_favorite_subtitle, favoriteSong.orderId),
        description = listOf(favoriteSong.artist, favoriteSong.updatedAt.ifBlank { favoriteSong.createdAt }).filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = null,
        image = favoriteSong.coverArt,
        sharedElementPrefix = sharedElementPrefix,
        uri = favoriteSong.uri,
    )
}

@Composable
fun DerakkumaCircle(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    circle: DerakkumaCircle,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = circle.name,
        subtitle = stringResource(Res.string.derakkuma_circle_subtitle, circle.rank, circle.totalPoints),
        description = listOf(circle.ownerName.takeIf(String::isNotBlank)?.let { "Owner $it" }, circle.month, circle.comment).filterNotNull().filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = listOf("${circle.totalPoints} pts", circle.daysUntilReset.takeIf { it > 0 }?.let { "$it days left" }).filterNotNull().joinToString(" · "),
        image = circle.characterImage ?: circle.backgroundImage,
        sharedElementPrefix = sharedElementPrefix,
        uri = circle.uri,
    )
}

@Composable
fun DerakkumaCircleMember(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    member: DerakkumaCircleMember,
) = with(paneTransitionScope) {
    DerakkumaRecordLayout(
        modifier = modifier,
        paneTransitionScope = paneTransitionScope,
        title = member.displayName,
        subtitle = stringResource(Res.string.derakkuma_circle_member_subtitle, member.role.ifBlank { "Member" }, member.rank, member.points),
        description = listOf(member.title, member.rating.takeIf { it > 0 }?.let { "Rating $it" }, member.updatedAt).filterNotNull().filter(String::isNotBlank).joinToString(" · ").ifBlank { null },
        blurb = null,
        image = member.icon,
        sharedElementPrefix = sharedElementPrefix,
        uri = member.uri,
    )
}

@Composable
private fun DerakkumaRecordLayout(
    modifier: Modifier,
    paneTransitionScope: PaneTransitionScope,
    title: String,
    subtitle: String,
    description: CharSequence?,
    blurb: String?,
    image: ImageUri?,
    secondaryImages: List<ImageUri?> = emptyList(),
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    sharedElementPrefix: String,
    uri: com.tunjid.heron.data.core.types.RecordUri,
) = RecordLayout(
    modifier = modifier,
    paneTransitionScope = paneTransitionScope,
    title = title.ifBlank { "Derakkuma" },
    subtitle = subtitle,
    description = description,
    blurb = blurb,
    sharedElementPrefix = sharedElementPrefix,
    sharedElementType = uri,
    onClick = onClick,
    avatar = {
        paneTransitionScope.DerakkumaAvatar(
            image = image ?: secondaryImages.firstOrNull { it != null },
            uri = uri,
            sharedElementPrefix = sharedElementPrefix,
        )
    },
    extraContent = extraContent,
)

@Composable
private fun DerakkumaProfileDetails(
    profile: DerakkumaProfile,
) {
    if (profile.ratingPlateImage == null &&
        profile.trophyPlateImage == null &&
        profile.courseImage == null &&
        profile.classImage == null &&
        profile.partnerImage == null
    ) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            profile.trophyPlateImage?.let { image ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DerakkumaInlineImage(
                        image = image,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Text(
                        text = profile.title,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = DerakkumaTextShadow,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile.ratingPlateImage?.let { image ->
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(96.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        DerakkumaInlineImage(
                            image = image,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                        Text(
                            text = "${profile.rating} ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.courseImage?.let { image ->
                        DerakkumaInlineImage(
                            image = image,
                            modifier = Modifier.height(24.dp).width(44.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (profile.stars > 0) {
                        Text(
                            text = "⭐×${profile.stars}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    profile.classImage?.let { image ->
                        DerakkumaInlineImage(
                            image = image,
                            modifier = Modifier.height(24.dp).width(44.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }

        profile.partnerImage?.let { image ->
            DerakkumaInlineImage(
                image = image,
                modifier = Modifier
                    .height(76.dp)
                    .width(64.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private val DerakkumaTextShadow = Shadow(
    color = Color.Black,
    blurRadius = 4f,
)

@Composable
private fun DerakkumaInlineImage(
    image: ImageUri,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    AsyncImage(
        modifier = modifier,
        args = remember(image.uri, contentScale) {
            ImageArgs(
                url = image.uri,
                contentScale = contentScale,
                contentDescription = null,
                shape = RoundedPolygonShape.Rectangle,
            )
        },
    )
}
