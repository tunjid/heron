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
class ThreadedVideoPositionStates<T>(
    private val id: T.() -> String,
) {
    private val itemIdsToStates = mutableStateMapOf<String, ThreadedVideoPositionState>()

    @Composable
    fun getOrCreateStateFor(item: T): ThreadedVideoPositionState {
        val state = itemIdsToStates.getOrPut(
            key = item.id(),
            defaultValue = ::ThreadedVideoPositionState
        )
        DisposableEffect(Unit) {
            onDispose { itemIdsToStates.remove(item.id()) }
        }
        return state
    }

    fun retrieveStateFor(item: T): ThreadedVideoPositionState? =
        itemIdsToStates[item.id()]
}

@Stable
class ThreadedVideoPositionState : State {
    private var node: ThreadedVideoPositionNode? = null

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
        ) = this.then(ThreadedVideoPositionElement(state))

        /**
         * Internal modifier for marking child positions in a threaded timeline item.
         */
        internal fun Modifier.childThreadNode(
            videoId: String?,
        ) = this.then(ThreadedVideoPositionElement(ChildState(videoId)))

        private data class ThreadedVideoPositionElement(
            private val state: State,
        ) : ModifierNodeElement<ThreadedVideoPositionNode>() {
            override fun create(): ThreadedVideoPositionNode =
                ThreadedVideoPositionNode(state = state)

            override fun update(node: ThreadedVideoPositionNode) {
                node.state = state
                when (val currentState = state) {
                    is ChildState -> Unit
                    is ThreadedVideoPositionState -> currentState.node = node
                }
            }
        }

        private class ThreadedVideoPositionNode(
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
