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
import androidx.compose.runtime.mutableStateMapOf
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

    override var isMuted: Boolean by mutableStateOf(false)

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

    /**
     * A map of ids registered in this [VideoPlayerController] and its associated
     * [ExoPlayerState]s. Note that the presence of an id in this map is _not_ an indication
     * that the backing media is available to play. For that information, reference [currentPlaylistIds].
     */
    private val idsToStates = mutableStateMapOf<String, ExoPlayerState>()

    /**
     * The ids of media available to play in the available [player] instance.
     */
    private var currentPlaylistIds by mutableStateOf(emptySet<String>())

    /**
     * The [MediaItem] currently in focus in the [player].
     */
    private var currentMediaItem by mutableStateOf<MediaItem?>(null)

    private var player: ExoPlayer? by mutableStateOf(null)

    private var activeVideoId: String by mutableStateOf("")

    // TODO: Revisit this. The Coroutine should be launched lazily instead of in an init block.
    init {
        player = exoPlayer(context = context).apply {
            // The first listener should always be the ExoplayerManager
            addListener(this@ExoplayerController)
            // Bind active video to restore its properties and attach its listener
            val hasActiveVideo = idsToStates[activeVideoId]?.let(::bind) != null
            // Restore the players playlist if it was previously saved
            val restoredMediaItems = idsToStates.values.map { it.toMediaItem() }
            addMediaItems(restoredMediaItems)
            currentPlaylistIds = restoredMediaItems.map(MediaItem::mediaId).toSet()
            playWhenReady = getVideoStateById(activeVideoId)?.autoplay == true
            if (playWhenReady) {
                prepare()
                if (playWhenReady && hasActiveVideo) play(activeVideoId)
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
        snapshotFlow { idsToStates[activeVideoId]?.status }
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
        idsToStates[videoId]?.let {
            it.autoplay = autoplay
        }
        if (videoId != activeVideoId) return

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
        val playerIdToPlay = videoId ?: activeVideoId

        // Video has not been previously registered
        val stateToPlay = idsToStates[playerIdToPlay] ?: return

        setActiveVideo(playerIdToPlay)

        // Already playing and not seeking, do nothing
        if (stateToPlay.status is PlayerStatus.Play.Confirmed && seekToMs == null) return

        // Diffing is async. Suspend until the video to play is registered in the player
        playAsync(playerIdToPlay, seekToMs)
    }

    override fun pauseActiveVideo() {
        activeVideoId.let(idsToStates::get)?.apply {
            status = PlayerStatus.Pause.Requested
        }
        player?.pause()
    }

    private fun setActiveVideo(videoId: String) {
        // Video has not been previously registered
        val activeState = idsToStates[videoId] ?: return

        val previousId = activeVideoId
        activeVideoId = videoId

        if (previousId == activeVideoId) return

        player?.apply {
            idsToStates[previousId]?.let(::unbind)
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

    override fun getVideoStateById(videoId: String): VideoPlayerState? = idsToStates[videoId]

    override fun retry(videoId: String) {
        setActiveVideo(videoId)
        player?.prepare()
    }

    override fun seekTo(position: Long) {
        play(videoId = activeVideoId, seekToMs = position)
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
        idsToStates[videoId]?.let { return it }

        trim()
        val videoPlayerState = ExoPlayerState(
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

        idsToStates[videoId] = videoPlayerState
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

    override fun unregisterAll(retainedVideoIds: Set<String>): Set<String> {
        idsToStates
            .filterNot { retainedVideoIds.contains(it.key) }
            .forEach { (id, videoState) ->
                if (activeVideoId == id) {
                    player?.pause()
                }
                player?.unbind(videoState)
                idsToStates.remove(id)
            }
        // Remove all videos that have not been retained from the playlist.In
        scope.launch {
            mediaItemMutationsChannel.send { existingItems ->
                existingItems.filter { retainedVideoIds.contains(it.mediaId) }
            }
        }
        return retainedVideoIds - idsToStates.keys
    }

    internal fun teardown() {
        diffingJob?.cancel()
        player?.apply {
            removeListener(this@ExoplayerController)
            idsToStates[activeVideoId]?.let {
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
            snapshotFlow { activeVideoId == playerIdToPlay }
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

    private fun trim() {
        val size = idsToStates.size
        if (size >= MaxVideoStates) idsToStates.keys.filter {
            val state = idsToStates[it]
            state?.status is PlayerStatus.Idle.Evicted
        }
            .take(size - MaxVideoStates)
            .forEach(idsToStates::remove)
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
        activeVideoId == videoId && currentMediaItem?.mediaId == videoId

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

private const val MaxVideoStates = 30
