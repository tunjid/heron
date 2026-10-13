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

package com.tunjid.heron.ui.modifiers

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.min
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun verticalProgressiveBlurEffect(
    fractions: FloatArray,
    radiiPx: FloatArray,
    count: Int,
    lineStartY: Float,
    lineEndY: Float,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect? {
    var maxRadiusPx = 0f
    for (index in 0 until count) {
        maxRadiusPx = max(
            a = maxRadiusPx,
            b = radiiPx[index],
        )
    }

    // Normalize to a 0..1 intensity, then pad the tail with the last stop.
    val intensities = FloatArray(MaxBlurStops)
    val positions = FloatArray(MaxBlurStops)
    for (index in 0 until MaxBlurStops) {
        val isStop = index < count
        positions[index] = if (isStop) fractions[index] else 1f
        intensities[index] = radiiPx[if (isStop) index else count - 1] / maxRadiusPx
    }

    fun pass(
        effect: RuntimeEffect,
        input: ImageFilter?,
    ): ImageFilter {
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform(
            name = "blurRadius",
            value = min(
                a = maxRadiusPx,
                b = MaxProgressiveBlurRadiusPx,
            ),
        )
        builder.uniform(
            name = "crop",
            value1 = 0f,
            value2 = 0f,
            value3 = size.width,
            value4 = size.height,
        )
        builder.uniform(
            name = "unbounded",
            value = if (edgeTreatment == TileMode.Decal) 1f else 0f,
        )
        builder.uniform(
            name = "lineStartY",
            value = lineStartY,
        )
        builder.uniform(
            name = "lineEndY",
            value = lineEndY,
        )
        builder.uniform(
            name = "intensities",
            value = intensities,
        )
        builder.uniform(
            name = "positions",
            value = positions,
        )
        return ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderName = "content",
            input = input,
        )
    }

    // Separable blur: the horizontal pass feeds the vertical pass.
    return pass(
        effect = StopsBlurPasses.vertical,
        input = pass(
            effect = StopsBlurPasses.horizontal,
            input = null,
        ),
    ).asComposeRenderEffect()
}

private object StopsBlurPasses {
    val horizontal = RuntimeEffect.makeForShader(
        sksl = stopsBlurSkSl(isVertical = false),
    )
    val vertical = RuntimeEffect.makeForShader(
        sksl = stopsBlurSkSl(isVertical = true),
    )
}

// Adapted from androidx.compose.ui.graphics.blur.BlurShaders
// Copyright 2026 The Android Open Source Project,
// licensed under the Apache License, Version 2.0.
private fun stopsBlurSkSl(
    isVertical: Boolean,
): String {
    val last = MaxBlurStops - 1

    val header = """
        uniform shader content;
        uniform float blurRadius;
        uniform float4 crop;
        uniform float unbounded;
        const float maxRadius = $MaxProgressiveBlurRadiusPx;
        uniform float lineStartY;
        uniform float lineEndY;
        uniform float intensities[$MaxBlurStops];
        uniform float positions[$MaxBlurStops];

        float getIntensity(float t) {
            if (t <= positions[0]) return intensities[0];
            if (t >= positions[$last]) return intensities[$last];

            for (int i = 0; i < $last; i++) {
                float p0 = positions[i];
                float p1 = positions[i + 1];

                if (t >= p0 && t <= p1) {
                    float fraction = (t - p0) / max(p1 - p0, 0.0001);
                    return mix(intensities[i], intensities[i + 1], clamp(fraction, 0.0, 1.0));
                }
            }
            return intensities[$last];
        }
    """.trimIndent()

    val main = """
        half4 main(float2 coord) {
            float span = lineEndY - lineStartY;
            float t = span > 0.0 ? (coord.y - lineStartY) / span : 0.0;
            t = clamp(t, 0.0, 1.0);

            float radius = blurRadius * clamp(getIntensity(t), 0.0, 1.0);

            return half4(blur(coord, radius));
        }
    """.trimIndent()

    return header + "\n\n" + blurCoreSkSl(isVertical) + "\n\n" + main
}

private fun blurCoreSkSl(
    isVertical: Boolean,
): String {
    val loopOffset =
        if (isVertical) "vec2(0.0, i + weightH / weight)"
        else "vec2(i + weightH / weight, 0.0)"

    val oddOffset =
        if (isVertical) "vec2(0.0, r)"
        else "vec2(r, 0.0)"

    val boundsBody =
        if (isVertical) "return step(bounds.y, sampleCoord.y) * (1.0 - step(bounds.w, sampleCoord.y));"
        else "return step(bounds.x, sampleCoord.x) * (1.0 - step(bounds.z, sampleCoord.x));"

    return """
        float gaussian(float x, float sigma) {
            return exp(-(x * x) / (2.0 * sigma * sigma));
        }

        float inBoundsOnMovedAxis(vec2 sampleCoord, float4 bounds) {
            $boundsBody
        }

        vec4 blur(vec2 coord, float radius) {
            float r = floor(radius);

            if (r < 1.0) { return content.eval(coord); }

            float sigma = max(radius / 2.0, 1.0);
            float weightSum = 1.0;
            vec4 result = content.eval(coord);

            for (float i = 1.0; i < maxRadius; i += 2.0) {
                if (i >= r) { break; }

                float weightL = gaussian(i, sigma);
                float weightH = gaussian(i + 1.0, sigma);
                float weight = weightL + weightH;
                vec2 offset = $loopOffset;

                // Decal (unbounded == 1.0): the denominator stays full and out-of-bounds
                // fetches are skipped, so edges fade to transparent.
                // Clamp (unbounded == 0.0): the denominator shrinks to the in-bounds weight,
                // renormalizing interior samples.
                vec2 newCoord1 = coord - offset;
                float mask1 = inBoundsOnMovedAxis(newCoord1, crop);
                weightSum += weight * max(mask1, unbounded);
                if (mask1 > 0.0) {
                    result += weight * content.eval(newCoord1);
                }

                vec2 newCoord2 = coord + offset;
                float mask2 = inBoundsOnMovedAxis(newCoord2, crop);
                weightSum += weight * max(mask2, unbounded);
                if (mask2 > 0.0) {
                    result += weight * content.eval(newCoord2);
                }
            }

            float oddMask = mod(r, 2.0) * (1.0 - step(maxRadius, r));
            float oddWeight = gaussian(r, sigma) * oddMask;
            vec2 oddOffset = $oddOffset;

            vec2 oddCoord1 = coord - oddOffset;
            float oddBounds1 = inBoundsOnMovedAxis(oddCoord1, crop);
            weightSum += oddWeight * max(oddBounds1, unbounded);
            if (oddBounds1 > 0.0) {
                result += oddWeight * content.eval(oddCoord1);
            }

            vec2 oddCoord2 = coord + oddOffset;
            float oddBounds2 = inBoundsOnMovedAxis(oddCoord2, crop);
            weightSum += oddWeight * max(oddBounds2, unbounded);
            if (oddBounds2 > 0.0) {
                result += oddWeight * content.eval(oddCoord2);
            }

            return result / weightSum;
        }
    """.trimIndent()
}
