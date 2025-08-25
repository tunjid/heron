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

package com.tunjid.heron.conversation.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.formatTextPost
import com.tunjid.heron.ui.text.links
import heron.feature.conversation.generated.resources.Res
import heron.feature.conversation.generated.resources.textfield_desc
import heron.feature.conversation.generated.resources.textfield_hint
import heron.feature.conversation.generated.resources.textfield_send
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaneScaffoldState.UserInput(
    sendMessage: (AnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    val onSendMessage = remember {
        { text: AnnotatedString ->
            sendMessage(text)
            // Reset text field and close keyboard
            textState = TextFieldValue()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserInputText(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = UserInputShape,
                )
                .padding(vertical = 16.dp)
                .weight(1f)
                .heightIn(max = 80.dp),
            textFieldValue = textState,
            onTextChanged = {
                textState = it.copy(
                    annotatedString = formatTextPost(
                        text = it.text,
                        textLinks = it.annotatedString.links(),
                        onLinkTargetClicked = {}
                    ),
                )
            },
            // Only show the keyboard if there's no input selector and text field has focus
            keyboardShown = textFieldFocusState,
            // Close extended selector if text field receives focus
            onTextFieldFocused = { focused ->
                textFieldFocusState = focused
            },
            onMessageSent = onSendMessage,
            focusState = textFieldFocusState,
        )
        SendButton(
            modifier = Modifier
                .height(36.dp),
            textFieldValue = textState,
            onMessageSent = onSendMessage,
        )
    }
}

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalFoundationApi
@Composable
private fun UserInputText(
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    onMessageSent: (AnnotatedString) -> Unit,
    focusState: Boolean,
) {
    val a11ylabel = stringResource(Res.string.textfield_desc)

    Box(
        modifier = modifier
    ) {
        UserInputTextField(
            textFieldValue = textFieldValue,
            onTextChanged = onTextChanged,
            onTextFieldFocused = onTextFieldFocused,
            keyboardType = keyboardType,
            focusState = focusState,
            onMessageSent = onMessageSent,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = a11ylabel
                keyboardShownProperty = keyboardShown
            },
        )
    }
}

@Composable
private fun BoxScope.UserInputTextField(
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    focusState: Boolean,
    onMessageSent: (AnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastFocusState by remember { mutableStateOf(false) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { onTextChanged(it) },
        modifier = modifier
            .padding(horizontal = 32.dp)
            .align(Alignment.CenterStart)
            .onFocusChanged { state ->
                if (lastFocusState != state.isFocused) {
                    onTextFieldFocused(state.isFocused)
                }
                lastFocusState = state.isFocused
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions {
            if (textFieldValue.text.isNotBlank()) onMessageSent(textFieldValue.annotatedString)
        },
        cursorBrush = SolidColor(LocalContentColor.current),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
    )

    val disableContentColor =
        MaterialTheme.colorScheme.onSurfaceVariant
    if (textFieldValue.text.isEmpty() && !focusState) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
            text = stringResource(Res.string.textfield_hint),
            style = MaterialTheme.typography.bodyLarge.copy(color = disableContentColor),
        )
    }
}

@Composable
fun PaneScaffoldState.SendButton(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onMessageSent: (AnnotatedString) -> Unit,
) {
    PaneFab(
        modifier = modifier
            .alpha(if (textFieldValue.text.isNotBlank()) 1f else 0.6f),
        expanded = true,
        text = stringResource(Res.string.textfield_send),
        icon = null,
        visible = true,
        onClick = onClick@{
            onMessageSent(textFieldValue.annotatedString)
        }
    )
}

private val UserInputShape = RoundedCornerShape(32.dp)