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

package com.tunjid.heron.media.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.andrewbailey.diff.differenceOf
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Stable
class ExoplayerController(
    context: Context,
    private val scope: CoroutineScope,
    private val diffingDispatcher: CoroutineDispatcher,
) : VideoPlayerController,
    Player.Listener {

    /**
     * A [Job] for diffing the [ExoPlayer] playlist such that changing the active video does not discard buffered
     * videos.
     */
    private var diffingJob: Job? = null

    /**
     * A channel for passing changes to the items in the [ExoPlayer] playlist so they may be diffed without
     * causing [ExoPlayer] to reload them.
     */
    private val mediaItemMutationsChannel = Channel<(List<MediaItem>) -> List<MediaItem>>()

    private val states = VideoPlayerStates<ExoPlayerState>()

    override var isMuted: Boolean by states::isMuted

    /**
     * The ids of media available to play in the available [player] instance.
     */
    private var currentPlaylistIds by mutableStateOf(emptySet<String>())

    /**
     * The [MediaItem] currently in focus in the [player].
     */
    private var currentMediaItem by mutableStateOf<MediaItem?>(null)

    private var player: ExoPlayer? by mutableStateOf(null)

    // TODO: Revisit this. The Coroutine should be launched lazily instead of in an init block.
    init {
        player = exoPlayer(context = context).apply {
            // The first listener should always be the ExoplayerManager
            addListener(this@ExoplayerController)
            // Bind active video to restore its properties and attach its listener
            val hasActiveVideo = states.activeState?.let(::bind) != null
            playWhenReady = states.activeState?.autoplay == true
            if (playWhenReady) {
                prepare()
                if (playWhenReady && hasActiveVideo) play(states.activeVideoId)
            }
        }
        diffingJob?.cancel()
        // Launch a coroutine that lasts from setup -> teardown that sequentially processes each change to media
        // items in the ExoPlayer one after the other. The changes are diffed such that the ExoPlayer maintains
        // a single playlist, and changes in the active video does not clear the playlist.
        diffingJob = scope.launch {
            // sequentially process each change to media items one after the other
            mediaItemMutationsChannel.consumeAsFlow().collect { mutation ->
                // Await player
                val player = snapshotFlow { player }.filterNotNull().first()
                val updatedItems = mutation(player.currentMediaItems)
                player.update(newMediaItems = updatedItems)
                currentPlaylistIds = updatedItems.map(MediaItem::mediaId).toSet()
            }
        }

        // TODO this should also be governed by coroutine launch semantics
        //  as the one used for list diffing
        // Pause playback when nothing is visible to play
        snapshotFlow { states.activeState?.status }
            .map { it == null || it is PlayerStatus.Idle }
            .filter(true::equals)
            .onEach { player?.pause() }
            .launchIn(scope + Dispatchers.Main)

        snapshotFlow { isMuted }
            .onEach { player?.isMuted = it }
            .launchIn(scope + Dispatchers.Main)
    }

    internal fun setAutoplay(
        videoId: String,
        autoplay: Boolean,
    ) {
        states[videoId]?.let {
            it.autoplay = autoplay
        }
        if (videoId != states.activeVideoId) return

        if (!autoplay) {
            player?.pause()
        } else {
            player?.play()
        }
    }

    override fun play(
        videoId: String?,
        seekToMs: Long?,
    ) {
        val playerIdToPlay = videoId ?: states.activeVideoId

        // Video has not been previously registered
        val stateToPlay = states[playerIdToPlay] ?: return

        setActiveVideo(playerIdToPlay)

        // Already playing and not seeking, do nothing
        if (stateToPlay.status is PlayerStatus.Play.Confirmed && seekToMs == null) return

        // Diffing is async. Suspend until the video to play is registered in the player
        playAsync(
            playerIdToPlay = playerIdToPlay,
            seekToMs = seekToMs,
        )
    }

    override fun pauseActiveVideo() {
        states.activeState?.apply {
            status = PlayerStatus.Pause.Requested
        }
        player?.pause()
    }

    private fun setActiveVideo(videoId: String) {
        // Video has not been previously registered
        val activeState = states[videoId] ?: return

        val previousId = states.activeVideoId
        states.activeVideoId = videoId

        if (previousId == states.activeVideoId) return

        player?.apply {
            states[previousId]?.let(::unbind)
            bind(activeState)
        }
        // NOTE: Play must be called on the manager and not on the exoplayer instance itself.
        when (activeState.status) {
            is PlayerStatus.Idle -> if (activeState.autoplay) playAsync(
                playerIdToPlay = videoId,
                seekToMs = activeState.seekPositionOnPlayMs(seekToMs = null),
            ) else player?.pause()

            is PlayerStatus.Pause -> player?.pause()
            is PlayerStatus.Play -> playAsync(
                playerIdToPlay = videoId,
                seekToMs = activeState.seekPositionOnPlayMs(seekToMs = null),
            )
        }
    }

    override fun getVideoStateById(videoId: String): VideoPlayerState? = states[videoId]

    override fun retry(videoId: String) {
        setActiveVideo(videoId)
        player?.prepare()
    }

    override fun seekTo(position: Long) {
        play(
            videoId = states.activeVideoId,
            seekToMs = position,
        )
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        player?.currentMediaItem?.let(::currentMediaItem::set)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        player?.currentMediaItem?.let(::currentMediaItem::set)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        player?.currentMediaItem?.let(::currentMediaItem::set)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        currentMediaItem = mediaItem
    }

    override fun registerVideo(
        videoUrl: String,
        videoId: String,
        thumbnail: String?,
        isLooping: Boolean,
        autoplay: Boolean,
    ): VideoPlayerState {
        states[videoId]?.let { return it }

        val videoPlayerState = states.registerOrGet(videoId = videoId) {
            ExoPlayerState(
                videoUrl = videoUrl,
                videoId = videoId,
                thumbnail = thumbnail,
                autoplay = autoplay,
                isLooping = isLooping,
                isMuted = derivedStateOf {
                    isMuted
                },
                exoPlayerState = derivedStateOf {
                    player.takeIf { isCurrentMediaItem(videoId) }
                },
            )
        }

        val mediaItem = videoPlayerState.toMediaItem()

        // Add the new media item to the ExoPlayer playlist.
        // ExoPlayer buffers every item in its playlist consecutively.
        scope.launch {
            mediaItemMutationsChannel.send { existingItems ->
                // Do not duplicate items in the playlist.
                if (existingItems.any { it.mediaId == mediaItem.mediaId }) {
                    existingItems
                } else {
                    // Simply append the new video to the playlist.
                    existingItems + mediaItem
                }
            }
        }
        return videoPlayerState
    }

    internal fun teardown() {
        diffingJob?.cancel()
        player?.apply {
            removeListener(this@ExoplayerController)
            states.activeState?.let {
                removeListener(it.playerListener)
                it.updateFromPlayer()
            }
        }
        player?.release()
        player = null
    }

    override fun onPlayerError(error: PlaybackException) {
    }

    private fun playAsync(
        playerIdToPlay: String,
        seekToMs: Long?,
    ) {
        scope.launch {
            // Do this only while playerId is the activeVideo
            snapshotFlow { states.activeVideoId == playerIdToPlay }
                .flatMapLatest { isActiveVideo ->
                    if (isActiveVideo) {
                        snapshotFlow { currentPlaylistIds.contains(playerIdToPlay) }
                    } else {
                        // Terminate
                        emptyFlow()
                    }
                }.first(true::equals)
            getVideoStateById(playerIdToPlay)?.apply state@{
                this@ExoplayerController.player?.apply {
                    seekTo(
                        // mediaItemIndex =
                        currentMediaItems.indexOfFirst { it.mediaId == playerIdToPlay },
                        // positionMs =
                        seekPositionOnPlayMs(seekToMs),
                    )
                    prepare()
                    play()
                }
            }
        }
    }

    /**
     * Diffs media items and inserts them in the ExoPlayer playlist.
     * This ensures that a previously buffered video does not need to re-buffer when it is swapped
     * from active, to inactive, then back.
     */
    private suspend fun Player.update(newMediaItems: List<MediaItem>) {
        val oldMediaItems = currentMediaItems
        // Run this on a CoroutineContext that handles computation well, e.g. Dispatchers.Default
        val diff = withContext(diffingDispatcher) {
            differenceOf(
                original = oldMediaItems,
                updated = newMediaItems,
                detectMoves = true,
            )
        }

        diff.applyDiff(
            remove = ::removeMediaItem,
            insert = { item: MediaItem, index: Int ->
                addMediaItem(index, item)
            },
            move = ::moveMediaItem,
        )
    }

    private fun isCurrentMediaItem(videoId: String) =
        states.activeVideoId == videoId && currentMediaItem?.mediaId == videoId

    private val Player.currentMediaItems: List<MediaItem>
        get() = List(mediaItemCount, ::getMediaItemAt)
}

@OptIn(UnstableApi::class)
private fun exoPlayer(
    context: Context,
): ExoPlayer {
    val client = OkHttpClient()
    val cache = SimpleCache(
        /* cacheDir = */
        File(context.cacheDir, "homepagesimplecache"),
        /* evictor = */
        LeastRecentlyUsedCacheEvictor((10 * 1024 * 1024).toLong()),
        /* databaseProvider = */
        StandaloneDatabaseProvider(context),
    )

    val okHttpDataSourceFactory = OkHttpDataSource.Factory(client).also {
        it.setUserAgent(Util.getUserAgent(context, "Heron"))
    }

    val dataSourceFactory = CacheDataSource
        .Factory()
        .setCache(cache)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
        .let {
            // DefaultDataSource allows support of both http and local streams
            DefaultDataSource.Factory(context, it)
        }

    val audioAttributes = AudioAttributes
        .Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    return ExoPlayer
        .Builder(context, DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(audioAttributes, false)
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        .build()
}

private fun VideoPlayerState.toMediaItem() =
    MediaItem
        .Builder()
        .setUri(videoUrl)
        .setMediaId(videoId)
        .build()
