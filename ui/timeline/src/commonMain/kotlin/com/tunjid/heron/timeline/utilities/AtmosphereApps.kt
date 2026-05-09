package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.AtmosphereApp
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.app_name_rocksky
import heron.ui.timeline.generated.resources.app_name_standard_site
import heron.ui.timeline.generated.resources.app_name_unknown
import org.jetbrains.compose.resources.stringResource

@Composable
fun AtmosphereApp?.displayName(): String = stringResource(
    when (this?.id) {
        AtmosphereApp.StandardSiteId -> Res.string.app_name_standard_site
        AtmosphereApp.RockskyId -> Res.string.app_name_rocksky
        else -> Res.string.app_name_unknown
    },
)
