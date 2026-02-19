package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.labeler.LabelerView
import app.bsky.labeler.LabelerViewDetailed
import com.tunjid.heron.data.core.types.LabelerId
import com.tunjid.heron.data.core.types.LabelerUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.LabelerEntity

internal fun MultipleEntitySaver.add(viewingProfileId: ProfileId?, labeler: LabelerViewDetailed) {
    val creator = labeler.creator
    val creatorId = ProfileId(creator.did.did)
    val labelValueDefinitions = labeler.policies.labelValueDefinitions

    add(
        LabelerEntity(
            cid = LabelerId(labeler.cid.cid),
            uri = LabelerUri(labeler.uri.atUri),
            creatorId = creatorId,
            likeCount = labeler.likeCount,
        )
    )

    add(viewingProfileId = viewingProfileId, profileView = creator)

    labelValueDefinitions?.forEach { def -> add(creatorId = creatorId, labelValueDefinition = def) }
}

internal fun MultipleEntitySaver.add(viewingProfileId: ProfileId?, labeler: LabelerView) {
    val creator = labeler.creator
    val creatorId = ProfileId(creator.did.did)

    add(
        LabelerEntity(
            cid = LabelerId(labeler.cid.cid),
            uri = LabelerUri(labeler.uri.atUri),
            creatorId = creatorId,
            likeCount = labeler.likeCount,
        )
    )

    add(viewingProfileId = viewingProfileId, profileView = creator)
}
