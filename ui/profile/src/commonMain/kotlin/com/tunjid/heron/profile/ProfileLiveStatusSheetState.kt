package com.tunjid.heron.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.LiveChip
import com.tunjid.heron.ui.UiTokens.LiveBorderWidth
import com.tunjid.heron.ui.UiTokens.LiveStatusColor
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.action_cancel
import heron.ui.core.generated.resources.action_close
import heron.ui.core.generated.resources.bluecast_live_platform
import heron.ui.core.generated.resources.live_status_clear_url
import heron.ui.core.generated.resources.live_status_enabled_services
import heron.ui.core.generated.resources.live_status_end_live
import heron.ui.core.generated.resources.live_status_expires_in
import heron.ui.core.generated.resources.live_status_go_live_for
import heron.ui.core.generated.resources.live_status_go_live_subtitle
import heron.ui.core.generated.resources.live_status_go_live_title
import heron.ui.core.generated.resources.live_status_reset_url
import heron.ui.core.generated.resources.live_status_url_not_supported
import heron.ui.core.generated.resources.live_status_url_placeholder
import heron.ui.core.generated.resources.live_status_you_are_live
import heron.ui.core.generated.resources.streamplace_live_platform
import heron.ui.core.generated.resources.twitch_live_platform
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class ProfileLiveStatusSheetState(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    override fun onHidden() = Unit
    companion object {
        @Composable
        fun rememberUpdatedProfileLiveStatusSheetState(
            profile: Profile,
            onGoLive: (url: String, duration: Int) -> Unit,
            onEndLive: () -> Unit,
        ): ProfileLiveStatusSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = true,
            ) { scope ->
                ProfileLiveStatusSheetState(scope = scope)
            }
            ProfileLiveStatusBottomSheet(
                state = state,
                profile = profile,
                onGoLive = onGoLive,
                onEndLive = onEndLive,
            )
            return state
        }
    }
}

@Composable
fun ProfileLiveStatusBottomSheet(
    state: ProfileLiveStatusSheetState,
    profile: Profile,
    onGoLive: (url: String, duration: Int) -> Unit,
    onEndLive: () -> Unit,
) {
    state.ModalBottomSheet {
        ProfileLiveStatusSheetContent(
            profile = profile,
            currentStatus = profile.status,
            onDismiss = { state.hide() },
            onGoLive = onGoLive,
            onEndLive = onEndLive,
        )
    }
}

@Composable
private fun ProfileLiveStatusSheetContent(
    profile: Profile,
    currentStatus: Profile.ProfileStatus?,
    onDismiss: () -> Unit,
    onGoLive: (String, Int) -> Unit,
    onEndLive: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp)
                .size(width = 32.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
        )

        when {
            currentStatus?.isLive == true -> EditLiveContent(
                status = currentStatus,
                onDismiss = onDismiss,
                onEndLive = onEndLive,
            )
            else -> GoLiveContent(
                profile = profile,
                onDismiss = onDismiss,
                onGoLive = onGoLive,
            )
        }
    }
}

@Composable
private fun GoLiveContent(
    profile: Profile,
    onDismiss: () -> Unit,
    onGoLive: (url: String, selectedDuration: Int) -> Unit,
) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    val streamLink = remember(urlInput) { urlInput.toStreamLink() }
    val isUrlValid = streamLink != null
    var selectedDuration by rememberSaveable { mutableStateOf(LiveDuration.default) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(CommonStrings.live_status_go_live_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(CommonStrings.live_status_go_live_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LiveProfileRow(profile = profile)

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(CommonStrings.live_status_url_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            shape = RoundedCornerShape(size = 12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = urlInput.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(CommonStrings.live_status_clear_url),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlatformInfoCard(urlInput = urlInput, isUrlValid = isUrlValid)
            AnimatedVisibility(
                visible = streamLink != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                streamLink?.let { (link, platform) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StreamLinkCard(link = link, platform = platform)
                        DurationSelector(
                            selected = selectedDuration,
                            onSelected = { selectedDuration = it },
                        )
                    }
                }
            }
        }

        Button(
            onClick = { if (isUrlValid) onGoLive(urlInput, selectedDuration.minutes) else onDismiss() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = if (isUrlValid) ButtonDefaults.buttonColors()
            else ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = stringResource(
                    if (isUrlValid) CommonStrings.live_status_go_live_title
                    else CommonStrings.action_cancel,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun EditLiveContent(
    status: Profile.ProfileStatus,
    onDismiss: () -> Unit,
    onEndLive: () -> Unit,
) {
    var urlInput by rememberSaveable { mutableStateOf(status.embedUri) }
    val streamLink = remember(urlInput) { urlInput.toStreamLink() }

    val remainingMinutes by produceState(initialValue = status.remainingMinutes()) {
        while (true) {
            delay(60_000)
            value = status.remainingMinutes()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(CommonStrings.live_status_you_are_live),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        CommonStrings.live_status_expires_in,
                        remainingMinutes.toReadableRemaining(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = urlInput.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    IconButton(onClick = { urlInput = status.embedUri }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(CommonStrings.live_status_reset_url),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )

        streamLink?.let { (link, platform) ->
            StreamLinkCard(link = link, platform = platform)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(CommonStrings.action_close),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onEndLive,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(
                    text = stringResource(CommonStrings.live_status_end_live),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun LiveProfileRow(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveAvatarBadge(profile = profile)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = profile.displayName ?: profile.handle.id,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@" + profile.handle.id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiveAvatarBadge(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AsyncImage(
            args = ImageArgs(
                url = profile.avatar?.uri,
                contentDescription = profile.displayName ?: profile.handle.id,
                contentScale = ContentScale.Crop,
                shape = RoundedPolygonShape.Circle,
            ),
            modifier = Modifier
                .size(50.dp)
                .ifTrue(profile.status?.isLive == true) {
                    border(
                        width = LiveBorderWidth,
                        color = LiveStatusColor,
                        shape = CircleShape,
                    )
                },
        )
        LiveChip(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PlatformInfoCard(
    urlInput: String,
    isUrlValid: Boolean,
    modifier: Modifier = Modifier,
) {
    val showWarning = urlInput.isNotEmpty() && !isUrlValid
    val borderColor = when {
        showWarning -> MaterialTheme.colorScheme.error
        isUrlValid -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (showWarning) Icons.Rounded.Warning else Icons.Rounded.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (showWarning) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(CommonStrings.live_status_enabled_services),
            style = MaterialTheme.typography.bodySmall,
            color = if (showWarning) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = showWarning,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(CommonStrings.live_status_url_not_supported),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun StreamLinkCard(
    link: LinkTarget.ExternalLink,
    platform: LivePlatform,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(width = 1.dp, color = platform.brandColor.copy(alpha = 0.3f)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(platform.brandColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = platform.brandColor.copy(alpha = 0.6f),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(color = platform.brandColor, shape = CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(platform.displayName),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = platform.brandColor,
                        maxLines = 1,
                    )
                    Text(
                        text = link.uri.uri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationSelector(
    selected: LiveDuration,
    onSelected: (LiveDuration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(CommonStrings.live_status_go_live_for),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "· ${selected.endTimeLabel(now)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .heightIn(max = 280.dp)
                .exposedDropdownSize(),
        ) {
            LiveDuration.entries.forEach { duration ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = duration.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = duration.endTimeLabel(now),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    },
                    onClick = {
                        onSelected(duration)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private fun Profile.ProfileStatus.remainingMinutes(): Long {
    val expires = expiresAt ?: return 0L
    return (expires - Clock.System.now()).inWholeMinutes.coerceAtLeast(0L)
}

private fun Long.toReadableRemaining(): String = when {
    this <= 0L -> "less than a minute"
    this < 60L -> "${this}m"
    this % 60L == 0L -> "${this / 60}h"
    else -> "${this / 60}h ${this % 60}m"
}

internal enum class LivePlatform(
    val displayName: StringResource,
    val domains: List<String>,
    val brandColor: Color,
) {
    Twitch(
        displayName = CommonStrings.twitch_live_platform,
        domains = listOf("twitch.tv", "twitch.com"),
        brandColor = Color(0xFF9146FF),
    ),
    Streamplace(
        displayName = CommonStrings.streamplace_live_platform,
        domains = listOf("streamplace.tv", "stream.place"),
        brandColor = Color(0xFF0EA5E9),
    ),
    Bluecast(
        displayName = CommonStrings.bluecast_live_platform,
        domains = listOf("bluecast.tv", "bluecast.app"),
        brandColor = Color(0xFF1185FE),
    ),
}

internal fun String.toStreamLink(): Pair<LinkTarget.ExternalLink, LivePlatform>? {
    val normalized = trim().lowercase()
    if (normalized.isBlank()) return null
    val platform = LivePlatform.entries.firstOrNull { platform ->
        platform.domains.any { domain -> normalized.contains(domain) }
    } ?: return null
    return LinkTarget.ExternalLink(uri = GenericUri(trim())) to platform
}

internal enum class LiveDuration(val minutes: Int) {
    MIN_5(5),
    MIN_10(10),
    MIN_15(15),
    MIN_20(20),
    MIN_25(25),
    MIN_30(30),
    MIN_35(35),
    MIN_40(40),
    MIN_45(45),
    MIN_50(50),
    MIN_55(55),
    HOUR_1(60),
    MIN_70(70),
    MIN_75(75),
    MIN_80(80),
    MIN_85(85),
    MIN_90(90),
    MIN_95(95),
    MIN_100(100),
    MIN_105(105),
    MIN_110(110),
    MIN_115(115),
    HOUR_2(120),
    MIN_130(130),
    MIN_135(135),
    MIN_140(140),
    MIN_145(145),
    MIN_150(150),
    MIN_155(155),
    MIN_160(160),
    MIN_165(165),
    MIN_170(170),
    MIN_175(175),
    HOUR_3(180),
    MIN_190(190),
    MIN_195(195),
    MIN_200(200),
    MIN_205(205),
    MIN_210(210),
    MIN_215(215),
    MIN_220(220),
    MIN_225(225),
    MIN_230(230),
    MIN_235(235),
    HOUR_4(240), ;

    val label: String get() = when {
        minutes % 60 == 0 -> "${minutes / 60}h"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    fun endTimeLabel(from: LocalDateTime): String {
        val total = from.hour * 60 + from.minute + minutes
        val h = (total / 60) % 24
        val m = total % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$h12:${m.toString().padStart(2, '0')} $amPm"
    }

    companion object {
        val default = HOUR_1
    }
}
