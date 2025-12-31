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

package com.tunjid.heron.data.repository

import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.entities.asExternalModel
import com.tunjid.heron.data.utilities.recordResolver.RecordResolver
import com.tunjid.heron.data.utilities.withRefresh
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

interface EmbeddableRecordRepository {

    val subscribedLabelers: Flow<List<Labeler>>

    fun embeddableRecord(
        uri: EmbeddableRecordUri,
    ): Flow<Record.Embeddable>
}

internal class OfflineEmbeddableRecordRepository @Inject constructor(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val labelDao: LabelDao,
    private val starterPackDao: StarterPackDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val savedStateDataSource: SavedStateDataSource,
    private val recordResolver: RecordResolver,
) : EmbeddableRecordRepository {

    override val subscribedLabelers: Flow<List<Labeler>> =
        recordResolver.subscribedLabelers

    override fun embeddableRecord(uri: EmbeddableRecordUri): Flow<Record.Embeddable> =
        when (uri) {
            is FeedGeneratorUri -> feedGeneratorDao.feedGenerators(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is ListUri -> listDao.lists(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is StarterPackUri -> starterPackDao.starterPacks(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }
            is LabelerUri -> labelDao.labelers(
                listOf(uri),
            )
                .map { it.firstOrNull()?.asExternalModel() }

            is PostUri ->
                savedStateDataSource
                    .singleSessionFlow { profileId ->
                        postDao.posts(
                            viewingProfileId = profileId?.id,
                            postUris = listOf(uri),
                        )
                    }
                    .map {
                        it.firstOrNull()?.asExternalModel(
                            embeddedRecord = null,
                        )
                    }
        }
            .filterNotNull()
            .withRefresh {
                recordResolver.resolve(uri)
            }
}
