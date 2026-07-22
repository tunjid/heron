package com.tunjid.heron.sheets.inference

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.model.PlatformUnavailableReason
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.inference_sheet_translation_title
import heron.ui.timeline.generated.resources.inference_sheet_vibe_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Stable
@Snapshottable
interface InferenceState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        // Inference state is ephemeral runtime state mirroring the shared engine; never persisted.
        @Transient
        val engineState: EngineState? = null,
        @Transient
        val kind: InferenceKind = InferenceKind.Translation,
        // Translation writes a single outcome; vibe reads a profile through two lenses, so its posts
        // and replies each stream into their own slot behind the sheet's tabs.
        @Transient
        val translationOutcome: InferenceOutcome? = null,
        // The profile the vibe outcomes below describe; a change clears them so a new profile is
        // vibed afresh rather than showing another profile's cached lenses.
        @Transient
        val vibeProfileId: ProfileId? = null,
        @Transient
        val postsOutcome: InferenceOutcome? = null,
        @Transient
        val repliesOutcome: InferenceOutcome? = null,
    ) : InferenceState
}

internal fun InferenceState.SnapshotMutable.vibeOutcome(
    type: Timeline.Profile.Type,
): InferenceOutcome? = when (type) {
    Timeline.Profile.Type.Posts -> postsOutcome
    Timeline.Profile.Type.Replies -> repliesOutcome
    else -> null
}

internal fun InferenceState.SnapshotMutable.setVibeOutcome(
    type: Timeline.Profile.Type,
    outcome: InferenceOutcome?,
) = when (type) {
    Timeline.Profile.Type.Posts -> postsOutcome = outcome
    Timeline.Profile.Type.Replies -> repliesOutcome = outcome
    else -> Unit
}

sealed class InferenceAction(
    val key: String,
) {
    data class Translate(
        val post: Post,
        val sourceLanguage: String,
        val targetLanguage: String,
    ) : InferenceAction(key = "Translate")

    data class Vibe(
        val profileId: ProfileId,
        val type: Timeline.Profile.Type,
    ) : InferenceAction(key = "Vibe")

    sealed class Navigate :
        InferenceAction(key = "Navigate"),
        NavigationAction {

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}

@Stable
sealed interface InferenceOutcome {
    /** The text produced so far; empty until the first usable token arrives. */
    val text: String

    data class Loading(
        override val text: String = "",
    ) : InferenceOutcome

    data class Success(
        override val text: String,
    ) : InferenceOutcome

    data class Error(
        val memo: Memo,
        override val text: String = "",
    ) : InferenceOutcome

    /** No on-device model is available; the UI should prompt the user to download one. */
    data object NoModel : InferenceOutcome {
        override val text: String = ""
    }

    data class Unavailable(
        val reason: PlatformUnavailableReason,
    ) : InferenceOutcome {
        override val text: String = ""
    }
}

/** What the engine is currently inferring; drives the inference sheet's title. */
enum class InferenceKind(
    val titleRes: StringResource,
) {
    Translation(titleRes = Res.string.inference_sheet_translation_title),
    Vibe(titleRes = Res.string.inference_sheet_vibe_title),
}
