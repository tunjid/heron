package com.tunjid.heron.data.utilities.multipleEntitysaver

import com.atproto.label.LabelValueDefinition
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.LabelDefinitionEntity

internal fun MultipleEntitySaver.add(
    creatorId: ProfileId,
    labelValueDefinition: LabelValueDefinition,
) {
    val localeList = Labeler.LocaleInfoList(
        list = labelValueDefinition.locales.map {
            Labeler.LocaleInfo(
                lang = it.lang.tag,
                name = it.name,
                description = it.description,
            )
        },
    )

    val localeCbor = localeList.toUrlEncodedBase64()

    add(
        LabelDefinitionEntity(
            creatorId = creatorId,
            identifier = labelValueDefinition.identifier,
            adultOnly = labelValueDefinition.adultOnly ?: false,
            blurs = labelValueDefinition.blurs.value.lowercase(),
            defaultSetting = labelValueDefinition.defaultSetting?.value?.lowercase() ?: Label.Visibility.Warn.value,
            severity = labelValueDefinition.severity.value.lowercase(),
            localeInfoCbor = localeCbor,
        ),
    )
}
