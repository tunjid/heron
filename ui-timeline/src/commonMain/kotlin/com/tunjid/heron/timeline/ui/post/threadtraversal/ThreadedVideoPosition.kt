package com.tunjid.heron.timeline.ui.post.threadtraversal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseDescendants
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import kotlin.jvm.JvmInline

/**
 * Class for inferring the position of a video in a column of [TimelineItem.Thread] [Post]s.
 */
@Stable
class ThreadedVideoPositionStates {
    private val itemIdsToStates = mutableStateMapOf<String, ThreadedVideoPositionState>()

    @Composable
    fun getOrCreateStateFor(item: TimelineItem): ThreadedVideoPositionState {
        val state = itemIdsToStates.getOrPut(
            key = item.id,
            defaultValue = ::ThreadedVideoPositionState
        )
        DisposableEffect(Unit) {
            onDispose { itemIdsToStates.remove(item.id) }
        }
        return state
    }

    fun retrieveStateFor(item: TimelineItem): ThreadedVideoPositionState? =
        itemIdsToStates[item.id]
}

@Stable
class ThreadedVideoPositionState : State {
    private var node: ThreadedNode? = null

    override fun videoIdAt(fraction: Float): String? {
        return node?.videoIdAt(fraction)
    }

    companion object {

        /**
         * Modifier for keeping track of the position of a video in a timeline item that may
         * be threaded.
         */
        fun Modifier.threadedVideoPosition(
            state: ThreadedVideoPositionState,
        ) = this.then(ThreadElement(state))

        /**
         * Internal modifier for marking child positions in a threaded timeline item.
         */
        internal fun Modifier.childThreadNode(
            videoId: String?,
        ) = this.then(ThreadElement(ChildState(videoId)))

        private data class ThreadElement(
            private val state: State,
        ) : ModifierNodeElement<ThreadedNode>() {
            override fun create(): ThreadedNode =
                ThreadedNode(state = state)

            override fun update(node: ThreadedNode) {
                node.state = state
            }
        }

        private class ThreadedNode(
            var state: State,
        ) :
            Modifier.Node(),
            TraversableNode,
            ThreadTraversalNode {

            override val traverseKey: Any = ThreadTraversalNode.Companion.ThreadTraversalKey

            override fun onAttach() {
                when (val currentState = state) {
                    is ThreadedVideoPositionState -> currentState.node = this
                    is ChildState -> Unit
                }
            }

            override fun onDetach() {
                when (val currentState = state) {
                    is ThreadedVideoPositionState -> currentState.node = null
                    is ChildState -> Unit
                }
            }

            override fun onReset() {
                when (val currentState = state) {
                    is ThreadedVideoPositionState -> currentState.node = null
                    is ChildState -> Unit
                }
            }

            override fun videoIdAt(fraction: Float): String? =
                if (!isAttached) null
                else when (val currentState = state) {
                    is ChildState -> currentState.videoId
                    is ThreadedVideoPositionState -> {
                        val total = requireLayoutCoordinates().size.height
                        val limit = total * fraction
                        var seen = 0f
                        var videoId: String? = null

                        traverseDescendants<ThreadTraversalNode> { currentNode ->
                            seen += currentNode.requireLayoutCoordinates().size.height
                            if (seen > limit) {
                                videoId = currentNode.videoIdAt(fraction)
                                if (videoId != null) CancelTraversal
                                else ContinueTraversal
                            } else {
                                ContinueTraversal
                            }
                        }
                        videoId
                    }
                }
        }
    }
}

private sealed interface State {
    fun videoIdAt(fraction: Float): String?
}

@JvmInline
value class ChildState(
    val videoId: String?,
) : State {
    override fun videoIdAt(fraction: Float): String? = videoId
}

internal sealed interface ThreadTraversalNode : TraversableNode {

    fun videoIdAt(fraction: Float): String?

    companion object {
        object ThreadTraversalKey
    }
}


internal val Post.videoId
    get() = when (val embed = embed) {
        null -> null
        is ExternalEmbed -> null
        is ImageList -> null
        is Video -> embed.playlist.uri
        UnknownEmbed -> null
    }
