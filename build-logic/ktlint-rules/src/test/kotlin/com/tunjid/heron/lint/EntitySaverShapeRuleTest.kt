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

package com.tunjid.heron.lint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class EntitySaverShapeRuleTest {

    private val assertThat = assertThatRule { EntitySaverShapeRule() }

    @Test
    fun allowsAddRemoveAndFlush() {
        assertThat(
            """
            internal class MultipleEntitySaver(
                private val eventDao: EventDao,
            ) {
                private val rsvpEntities = LazyList<RsvpEntity>()
                private val rsvpDeletions = LazyList<RsvpEntity.Deletion>()
                private val embedEntities = LazyList<EmbedEntity>()

                internal suspend fun flushPendingOperations() {
                    eventDao.upsertRsvps(rsvpEntities.list)
                }

                fun MultipleEntitySaver.associatePostEmbeds(
                    embedEntity: EmbedEntity,
                ) {
                    add(embedEntity)
                }

                fun add(entity: RsvpEntity) = rsvpEntities.add(entity)

                fun remove(entity: RsvpEntity.Deletion) = rsvpDeletions.add(entity)

                private fun add(entity: EmbedEntity) = embedEntities.add(entity)

                private fun partition() = Unit
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun flagsNewOperations() {
        assertThat(
            """
            internal class MultipleEntitySaver(
                private val labelDao: LabelDao,
            ) {
                fun replaceLabels(uri: String) = Unit
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 9,
            detail = "MultipleEntitySaver only exposes add, remove and flushPendingOperations; " +
                "express intent with entities (e.g. stubbed sentinels), not new operations",
        )
    }

    @Test
    fun flagsVerbsWithLogic() {
        assertThat(
            """
            internal class MultipleEntitySaver(
                private val labelDao: LabelDao,
            ) {
                private val labelEntities = LazyList<LabelEntity>()

                fun add(entity: LabelEntity) {
                    if (entity.value.isNotBlank()) labelEntities.add(entity)
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = "MultipleEntitySaver.add must take one entity and only append it to a LazyList; " +
                "triage belongs in flushPendingOperations",
        )
    }

    @Test
    fun flagsNonLazyListState() {
        assertThat(
            """
            internal class MultipleEntitySaver(
                private val labelDao: LabelDao,
                private val clock: Clock,
            ) {
                private val labelUrisToClear = mutableSetOf<String>()
            }
            """.trimIndent(),
        ).hasLintViolationsWithoutAutoCorrect(
            LintViolation(
                line = 3,
                col = 17,
                detail = "MultipleEntitySaver constructor properties must be DAOs",
            ),
            LintViolation(
                line = 5,
                col = 17,
                detail = "MultipleEntitySaver may only buffer entities in private LazyList properties",
            ),
        )
    }

    @Test
    fun ignoresOtherClasses() {
        assertThat(
            """
            internal class TransactionalMultipleEntitySaverProvider(
                private val transactionWriter: TransactionWriter,
            ) {
                fun saveInTransaction() = Unit
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
