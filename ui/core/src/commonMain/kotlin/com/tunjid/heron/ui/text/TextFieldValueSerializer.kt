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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tunjid.heron.data.core.utilities.TextInputSnapshot
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A custom KSerializer for Jetpack Compose TextFieldValue.
 *
 * Logic:
 * - Text: Serialized as standard String.
 * - Selection/Composition: Flattened into integer primitives.
 * - AnnotatedString: Only preserves LinkAnnotation.Url (Links), ignoring color/weight/etc.
 */
object TextFieldValueSerializer : KSerializer<TextFieldValue> {
    override val descriptor: SerialDescriptor = TextInputSnapshot.serializer().descriptor

    override fun serialize(encoder: Encoder, value: TextFieldValue) {
        val selection = value.selection
        val composition = value.composition

        val surrogate = TextInputSnapshot(
            text = value.text,
            selectionStart = selection.start,
            selectionEnd = selection.end,
            compositionStart = composition?.start ?: -1,
            compositionEnd = composition?.end ?: -1,
            links = value.annotatedString.links(),
        )

        TextInputSnapshot.serializer().serialize(encoder, surrogate)
    }

    override fun deserialize(decoder: Decoder): TextFieldValue {
        val surrogate = TextInputSnapshot.serializer().deserialize(decoder)

        val annotatedString = formatTextPost(
            text = surrogate.text,
            textLinks = surrogate.links,
            textLinkStyles = null,
        )

        // Reconstruct Selection
        val selection = TextRange(surrogate.selectionStart, surrogate.selectionEnd)

        // Reconstruct Composition (restore null if -1)
        val composition = if (surrogate.compositionStart != -1 && surrogate.compositionEnd != -1) {
            TextRange(surrogate.compositionStart, surrogate.compositionEnd)
        } else {
            null
        }

        return TextFieldValue(
            annotatedString = annotatedString,
            selection = selection,
            composition = composition,
        )
    }
}
