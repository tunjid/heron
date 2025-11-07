package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.labeler.LabelerViewDetailed
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.LabelerEntity

internal fun MultipleEntitySaver.add(
    labeler: LabelerViewDetailed,
) {
    val creator = labeler.creator
    val labelValueDefinitions = labeler.policies.labelValueDefinitions

    add(
        LabelerEntity(
            cid = labeler.cid.cid,
            uri = GenericUri(labeler.uri.atUri),
            creatorId = ProfileId(creator.did.did),
            likeCount = labeler.likeCount,
        ),
    )

    labelValueDefinitions.forEach { def ->
        add(
            creatorId = ProfileId(creator.did.did),
            labelValueDefinition = def,
        )
    }
}
