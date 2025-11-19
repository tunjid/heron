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

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

fun Record.avatarSharedElementKey(
    prefix: String?,
    quotingPostUri: PostUri? = null,
): String {
    val finalPrefix = quotingPostUri
        ?.let { "$prefix-$it" }
        ?: prefix
    val creator = when (this) {
        is Labeler -> creator
        is Post -> author
        is FeedGenerator -> creator
        is FeedList -> creator
        is StarterPack -> creator
    }
    return "$finalPrefix-${reference.uri.uri}-${creator.did.id}-avatar"
}

fun RecordUri.collectionShape() = when (this) {
    is FeedGeneratorUri -> FeedGeneratorCollectionShape
    is LabelerUri -> LabelerCollectionShape
    is ListUri -> ListCollectionShape
    is PostUri -> RoundedPolygonShape.Circle
    is StarterPackUri -> StarterPackCollectionShape
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val FeedGeneratorCollectionShape by lazy {
    RoundedPolygonShape.Custom(
        polygon = MaterialShapes.Square,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ListCollectionShape by lazy {
    RoundedPolygonShape.Custom(
        polygon = MaterialShapes.Pill,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val StarterPackCollectionShape by lazy {
    RoundedPolygonShape.Custom(
        polygon = MaterialShapes.Cookie9Sided,
    )
}

internal val LabelerCollectionShape by lazy {
    RoundedPolygonShape.Custom(
        polygon = RoundedPolygon(
            numVertices = 4,
            perVertexRounding = floatArrayOf(1f, 1f, 0.2f, 0.2f)
                .map(::CornerRounding),
        ).transformed(Matrix().apply { rotateZ(degrees = 45f) }),
    )
}

internal val BlueskyClouds =
    ImageUri("https://cdn.bsky.app/img/banner/plain/did:plc:z72i7hdynmk6r22z27h6tvur/bafkreichzyovokfzmymz36p5jibbjrhsur6n7hjnzxrpbt5jaydp2szvna@jpeg")

private fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon = transformed { x, y ->
    val transformedPoint = matrix.map(Offset(x, y))
    TransformResult(transformedPoint.x, transformedPoint.y)
}
