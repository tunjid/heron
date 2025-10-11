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

package com.tunjid.heron.ui.text

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.jvm.JvmInline
import kotlinx.serialization.Transient

data class FormField(
    val id: Id,
    val value: String,
    val maxLines: Int,
    @Transient
    val errorMessage: Memo? = null,
    @Transient
    val leadingIcon: ImageVector? = null,
    @Transient
    val transformation: VisualTransformation = VisualTransformation.None,
    @Transient
    val contentType: ContentType? = null,
    @Transient
    val contentDescription: Memo? = null,
    @Transient
    val keyboardOptions: KeyboardOptions = KeyboardOptions(),
    @Transient
    val validator: Validator? = null,
) {
    @JvmInline
    value class Id(
        private val id: String,
    ) {
        override fun toString(): String = id
    }
}

fun List<FormField>.copyWithValidation(
    id: FormField.Id,
    text: String,
) = map { field ->
    if (field.id == id) field.copyWithValidation(text)
    else field
}

fun List<FormField>.valueFor(id: FormField.Id) =
    first { it.id == id }.value

fun FormField.copyWithValidation(
    text: String,
) = copy(
    value = text,
    errorMessage = validator
        ?.checks
        ?.firstNotNullOfOrNull { (check, errorRes) ->
            if (check(text)) null
            else errorRes
        },
)

fun FormField.validated() = copy(
    value = value,
    errorMessage = validator
        ?.checks
        ?.firstNotNullOfOrNull { (check, errorRes) ->
            if (check(value)) null
            else errorRes
        },
)

val FormField.isValid
    get() = validator
        ?.checks
        ?.all { (check) -> check(value) }
        ?: true

@JvmInline
value class Validator(
    val checks: List<Pair<(String) -> Boolean, Memo>>,
)

fun Validator(vararg pairs: Pair<(String) -> Boolean, Memo>) =
    Validator(
        checks = pairs.asList(),
    )

@Composable
inline fun FormField(
    modifier: Modifier = Modifier,
    field: FormField,
    crossinline onValueChange: (field: FormField, newValue: String) -> Unit,
    crossinline keyboardActions: KeyboardActionScope.(FormField) -> Unit,
) {
    var hasBeenFocused by remember { mutableStateOf(false) }
    val showError = hasBeenFocused && field.errorMessage != null
    OutlinedTextField(
        modifier = modifier
            .semantics {
                field.contentType?.let { contentType = it }
            }
            .onFocusChanged {
                if (!it.isFocused) hasBeenFocused = true
            },
        value = field.value,
        maxLines = field.maxLines,
        onValueChange = {
            onValueChange(field, it)
        },
        shape = MaterialTheme.shapes.large,
        visualTransformation = field.transformation,
        keyboardOptions = field.keyboardOptions,
        keyboardActions = KeyboardActions {
            keyboardActions(field)
        },
        label = field.contentDescription?.let{
             {
                Text(it.message)
            }
        },
        leadingIcon = field.leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = field.contentDescription?.message,
                )
            }
        },
        supportingText = if (showError) field.errorMessage?.let {
            {
                Text(
                    text = it.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        else null,
    )
}
