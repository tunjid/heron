package com.tunjid.heron.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tunjid.heron.search.Action
import com.tunjid.heron.search.ScreenLayout
import heron.feature_search.generated.resources.Res
import heron.feature_search.generated.resources.close_search
import heron.feature_search.generated.resources.search
import kotlinx.coroutines.flow.debounce
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchBar(
    searchQuery: String,
    layout: ScreenLayout,
    onSearchAction: (Action.Search) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    OutlinedTextField(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                hasFocus = focusState.isFocused && focusState.hasFocus
            },
        value = searchQuery,
        onValueChange = { query ->
            onSearchAction(Action.Search.OnSearchQueryChanged(query))
        },
        placeholder = {
            Text(stringResource(Res.string.search))
        },
        trailingIcon = {
            AnimatedVisibility(
                modifier = Modifier.size(48.dp),
                visible = layout == ScreenLayout.GeneralSearchResults,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = {
                        onSearchAction(
                            Action.Search.CloseGeneralResults(reset = true)
                        )
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(Res.string.close_search),
                        )
                    }
                )
            }
        },
        textStyle = MaterialTheme.typography.labelLarge,
        singleLine = true,
        shape = RoundedCornerShape(36.dp),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions {
            onSearchAction(Action.Search.OnSearchQueryConfirmed(isLocalOnly = false))
        },
    )

    LaunchedEffect(Unit) {
        snapshotFlow { hasFocus }
            .debounce(350)
            .collect { focused ->
                if (focused) onSearchAction(
                    Action.Search.CloseGeneralResults(reset = false)
                )
            }
    }
}