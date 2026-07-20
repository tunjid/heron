package com.tunjid.heron.sheets.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.timeline.ui.profile.ProfileSearchResults
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import heron.ui.core.generated.resources.done
import org.jetbrains.compose.resources.stringResource

@Stable
class ProfileSearchSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: ProfileSearchStateHolder,
) : BottomSheetState(scope) {

    val cachedProfiles
        get() = stateHolder.state.cachedProfiles

    fun show(
        title: String,
    ) {
        stateHolder(
            ProfileSearchAction.UpdateTitle(title),
        )
        show()
    }

    fun seedProfiles(
        ids: List<Id.Profile>,
    ) {
        stateHolder(ProfileSearchAction.Seed(ids))
    }

    override fun onHidden() {
        stateHolder(ProfileSearchAction.Query.Clear)
    }

    companion object {
        @Composable
        internal fun rememberUpdatedProfileSearchSheetState(
            stateHolder: ProfileSearchStateHolder,
            onProfileClicked: (Profile) -> Unit,
        ): ProfileSearchSheetState {
            val state = rememberBottomSheetState(
                stateHolder = stateHolder,
                block = ::ProfileSearchSheetState,
            )

            state.ModalBottomSheet {
                Results(
                    sheetState = state,
                    onProfileClicked = onProfileClicked,
                )
            }

            return state
        }
    }
}

@Composable
internal fun Results(
    sheetState: ProfileSearchSheetState,
    onProfileClicked: (Profile) -> Unit,

) {
    val state = sheetState.stateHolder.produceState()
    val focusRequester = remember {
        FocusRequester()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(vertical = 12.dp),
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = {
                sheetState.stateHolder(ProfileSearchAction.Query.Search(it))
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        ProfileSearchResults(
            modifier = Modifier
                .fillMaxWidth(),
            results = state.searchResults,
            onProfileClicked = { profile ->
                onProfileClicked(profile)
                sheetState.hide()
            },
        )

        Button(
            onClick = {
                sheetState.hide()
            },
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(text = stringResource(CommonStrings.done))
        }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }
}
