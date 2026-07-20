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

package com.tunjid.heron.timeline.utilities

import androidx.collection.MutableObjectLongMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.registerOnLayoutRectChanged
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.unit.IntOffset
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Identifies the most visible video within the modified node's own layout bounds.
 *
 * Visibility is measured against this node's layout rectangle, in the compose root's coordinate
 * space, so chrome that occupies real layout space — e.g. a collapsing header the content is placed
 * beneath, or the pane's own bounds on a multi-pane layout — is already accounted for without any
 * inset. Only chrome *drawn over* the content (the status bar, an overlaying app bar or tab strip,
 * the bottom navigation bar) needs to be subtracted via the inset lambdas.
 *
 * @param topLeftInset inset, in pixels, of chrome drawn over the top-left of the node (status bar +
 *   an overlaying toolbar/tabs, and a navigation rail on wide layouts), excluded so a video behind
 *   it is not considered visible. Sampled on every visibility report, so it may return a live,
 *   animating value (e.g. a collapsing app bar's offset).
 * @param bottomRightInset inset, in pixels, of chrome drawn over the bottom-right (e.g. the bottom
 *   navigation bar), likewise excluded and likewise sampled live.
 * @param isEnabled gate observed alongside the most visible video; while it returns false,
 *   [onIdChanged] is not invoked (so an off-screen pager page does not fight for
 *   playback), and it is re-invoked with the current winner as soon as it returns true again.
 * @param onIdChanged invoked with the most visible video's id, or null to pause.
 */
fun Modifier.onDominantVideoChange(
    topLeftInset: () -> IntOffset = { IntOffset.Zero },
    bottomRightInset: () -> IntOffset = { IntOffset.Zero },
    isEnabled: () -> Boolean = { true },
    onIdChanged: (String?) -> Unit,
): Modifier = this then DominantVideoElement(
    topLeftInset = topLeftInset,
    bottomRightInset = bottomRightInset,
    isEnabled = isEnabled,
    onIdChanged = onIdChanged,
)

/**
 * Reports the visibility of the video identified by [videoId] into the nearest
 * [onDominantVideoChange] ancestor as this node moves through the viewport. A null [videoId], or the
 * absence of a [onDominantVideoChange] ancestor, makes this a no-op, so it is safe to apply
 * unconditionally.
 */
internal fun Modifier.reportVideoVisibility(
    videoId: String?,
): Modifier =
    if (videoId == null) this
    else this then ReportVideoVisibilityElement(videoId)

/**
 * Tracks how visible each timeline video is, so the single most visible one can be auto-played.
 * Owned by [DominantVideoNode]; not part of the public surface.
 *
 * Visibility is held in a primitive [MutableObjectLongMap] (video id -> packed [VideoVisibility])
 * rather than a `SnapshotStateMap`, so the hot reporting path allocates nothing and touches no
 * snapshot records. A single [version] snapshot int is bumped per mutation to drive recomputation;
 * all access is on the main thread (layout callbacks and the owner's flow), so the plain map is
 * safe.
 */
internal class DominantVideoVisibilityState {

    private val visibilities = MutableObjectLongMap<String>()

    private var version by mutableIntStateOf(0)

    fun onVideoVisibilityChanged(
        videoId: String,
        fraction: Float,
        top: Int,
    ) {
        visibilities[videoId] = VideoVisibility(
            fraction = fraction,
            top = top,
        ).packed
        version++
    }

    fun onVideoDisposed(
        videoId: String,
    ) {
        visibilities.remove(videoId)
        version++
    }

    /**
     * The id of the video currently most visible within the viewport, or null if none meets
     * [MinVisibleFraction]. Ties are broken in favour of the topmost video. Reads [version], so a
     * snapshotFlow calling this recomputes whenever a video reports or is disposed.
     */
    fun mostVisibleVideoId(): String? {
        var bestId: String? = null
        var bestFraction = 0f
        var bestTop = Int.MAX_VALUE
        // The `version >= 0` guard is always true; it exists to read [version] (a snapshot state)
        // so the enclosing snapshot observer is subscribed to mutations of the plain map.
        if (version >= 0) {
            visibilities.forEach { videoId, packed ->
                val visibility = VideoVisibility(packed)
                val fraction = visibility.fraction
                val isBetter = when {
                    // NaN can arise if the viewport collapses to empty area (e.g. an off-screen
                    // pager page whose bounds clip away); treat it as not visible rather than
                    // letting it slip past the comparisons below into the `bestId == null` branch.
                    fraction.isNaN() -> false
                    fraction < MinVisibleFraction -> false
                    bestId == null -> true
                    fraction > bestFraction -> true
                    fraction == bestFraction -> visibility.top < bestTop
                    else -> false
                }
                if (isBetter) {
                    bestId = videoId
                    bestFraction = fraction
                    bestTop = visibility.top
                }
            }
        }
        return bestId
    }
}

private data class DominantVideoElement(
    private val topLeftInset: () -> IntOffset,
    private val bottomRightInset: () -> IntOffset,
    private val isEnabled: () -> Boolean,
    private val onIdChanged: (String?) -> Unit,
) : ModifierNodeElement<DominantVideoNode>() {

    override fun create() = DominantVideoNode(
        topLeftInset = topLeftInset,
        bottomRightInset = bottomRightInset,
        isEnabled = isEnabled,
        onIdChanged = onIdChanged,
    )

    override fun update(node: DominantVideoNode) {
        node.topLeftInset = topLeftInset
        node.bottomRightInset = bottomRightInset
        node.isEnabled = isEnabled
        node.onIdChanged = onIdChanged
    }
}

private class DominantVideoNode(
    var topLeftInset: () -> IntOffset,
    var bottomRightInset: () -> IntOffset,
    var isEnabled: () -> Boolean,
    var onIdChanged: (String?) -> Unit,
) : Modifier.Node(),
    TraversableNode {

    override val traverseKey: Any = DominantVideoTraversalKey

    val state = DominantVideoVisibilityState()

    private var job: Job? = null

    override fun onAttach() {
        job?.cancel()
        job = coroutineScope.launch {
            snapshotFlow {
                // Short-circuit while disabled: emit the sentinel without scanning for a winner and,
                // crucially, without reading [version], so an inactive pager page stops re-running
                // this block on every report. The sentinel is distinct from any real id and from null
                // (which pauses), so a disabled page leaves playback untouched.
                if (isEnabled()) state.mostVisibleVideoId() else DisabledSentinel
            }
                .distinctUntilChanged()
                .collect { videoId ->
                    if (videoId != DisabledSentinel) onIdChanged(videoId)
                }
        }
    }

    override fun onDetach() {
        job?.cancel()
        job = null
    }
}

private data class ReportVideoVisibilityElement(
    private val videoId: String,
) : ModifierNodeElement<ReportVideoVisibilityNode>() {

    override fun create() = ReportVideoVisibilityNode(videoId)

    override fun update(node: ReportVideoVisibilityNode) {
        node.updateVideoId(videoId)
    }
}

private class ReportVideoVisibilityNode(
    private var videoId: String,
) : Modifier.Node() {

    private var root: DominantVideoNode? = null
    private var handle: RegistrationHandle? = null

    override fun onAttach() {
        val root = findRoot()
        this.root = root
        if (root != null) register(root)
    }

    override fun onDetach() {
        handle?.unregister()
        handle = null
        root?.state?.onVideoDisposed(videoId)
        root = null
    }

    fun updateVideoId(
        videoId: String,
    ) {
        if (videoId == this.videoId) return
        // The running callback reads [videoId] on each report, so dropping the stale entry is all
        // that is needed to rebind to the new id.
        root?.state?.onVideoDisposed(this.videoId)
        this.videoId = videoId
    }

    private fun findRoot(): DominantVideoNode? {
        var found: DominantVideoNode? = null
        traverseAncestors(DominantVideoTraversalKey) { ancestor ->
            if (ancestor is DominantVideoNode) {
                found = ancestor
                false
            } else {
                true
            }
        }
        return found
    }

    private fun register(
        root: DominantVideoNode,
    ) {
        handle?.unregister()
        handle = registerOnLayoutRectChanged(
            throttleMillis = VisibilityThrottleMs,
            debounceMillis = 0L,
        ) { bounds ->
            // The viewport is the root's own layout rectangle, in the compose root's coordinate
            // space (matching `bounds.positionInRoot`), so chrome laid out around the content — a
            // collapsing header the content sits beneath, the pane's bounds on a multi-pane
            // layout — is excluded for free. The inset lambdas then carve out only the chrome drawn
            // *over* the content (status bar, an overlaying app bar/tab strip, bottom navigation).
            val viewport = root.requireLayoutCoordinates().boundsInRoot()
            val topLeftInset = root.topLeftInset()
            val bottomRightInset = root.bottomRightInset()
            root.state.onVideoVisibilityChanged(
                videoId = videoId,
                fraction = bounds.fractionVisibleInRect(
                    left = viewport.left.roundToInt() + topLeftInset.x,
                    top = viewport.top.roundToInt() + topLeftInset.y,
                    right = viewport.right.roundToInt() - bottomRightInset.x,
                    bottom = viewport.bottom.roundToInt() - bottomRightInset.y,
                ),
                top = bounds.positionInRoot.y,
            )
        }
    }
}

private object DominantVideoTraversalKey

@JvmInline
private value class VideoVisibility(
    val packed: Long,
) {
    val fraction: Float
        get() = Float.fromBits((packed ushr 32).toInt())

    val top: Int
        get() = packed.toInt()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun VideoVisibility(
    fraction: Float,
    top: Int,
): VideoVisibility = VideoVisibility(
    (top.toLong() and 0xFFFFFFFFL) or ((fraction.toRawBits().toLong() and 0xFFFFFFFFL) shl 32),
)

private const val VisibilityThrottleMs = 64L

private const val MinVisibleFraction = 0.5f

/**
 * Emitted by a disabled [onDominantVideoChange] to mean "leave playback alone" (e.g. a non-owning
 * pager page), as distinct from null which means "pause". The leading control character guarantees
 * it can never equal a real video id.
 */
private const val DisabledSentinel = "\u0000video-visibility-disabled"
