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

package com.tunjid.heron.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HeronIcons.Atmospheric.Leaflet: ImageVector
    get() {
        if (_leaflet != null) {
            return _leaflet!!
        }
        _leaflet = ImageVector.Builder(
            name = "Leaflet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(2.3511f, 16.4351f)
                curveTo(1.5327f, 18.0156f, 0.9195f, 19.0434f, 0.3271f, 20.5184f)
                curveTo(-0.1355f, 21.67f, 0.4231f, 22.9785f, 1.5747f, 23.441f)
                curveTo(2.7262f, 23.9035f, 4.0347f, 23.345f, 4.4973f, 22.1934f)
                curveTo(4.6144f, 21.9019f, 4.7272f, 21.6147f, 4.8362f, 21.3373f)
                lineTo(4.8372f, 21.3347f)
                curveTo(5.2524f, 20.2783f, 5.6104f, 19.3743f, 6.0436f, 18.5602f)
                curveTo(6.5315f, 18.5337f, 7.0892f, 18.5296f, 7.7182f, 18.5261f)
                lineTo(7.7973f, 18.5256f)
                curveTo(8.9443f, 18.5193f, 10.3263f, 18.5117f, 11.518f, 18.2705f)
                curveTo(12.7104f, 18.0292f, 14.186f, 17.4586f, 14.8383f, 15.9305f)
                curveTo(15.0149f, 15.5168f, 15.194f, 14.9061f, 15.0372f, 14.2859f)
                curveTo(15.0842f, 14.2858f, 15.1324f, 14.2858f, 15.1819f, 14.2858f)
                curveTo(15.2084f, 14.2858f, 15.2354f, 14.2858f, 15.2629f, 14.2858f)
                curveTo(15.8921f, 14.2862f, 16.7777f, 14.2868f, 17.5693f, 14.0715f)
                curveTo(18.493f, 13.8202f, 19.4642f, 13.2252f, 19.8904f, 11.9719f)
                curveTo(20.0443f, 11.519f, 20.0729f, 11.0434f, 19.9399f, 10.5939f)
                curveTo(19.9807f, 10.5867f, 20.0224f, 10.5794f, 20.0653f, 10.5719f)
                curveTo(20.0859f, 10.5683f, 20.1068f, 10.5646f, 20.128f, 10.5609f)
                curveTo(20.6689f, 10.4664f, 21.3929f, 10.3399f, 22.0114f, 10.1085f)
                curveTo(22.6131f, 9.8834f, 23.6017f, 9.3806f, 23.8979f, 8.2281f)
                curveTo(24.0668f, 7.5712f, 24.0491f, 6.8758f, 23.6698f, 6.2478f)
                curveTo(23.5121f, 5.9867f, 23.3114f, 5.7811f, 23.1334f, 5.6295f)
                curveTo(23.2992f, 5.422f, 23.51f, 5.1333f, 23.6553f, 4.8149f)
                curveTo(23.9072f, 4.2624f, 24.0371f, 3.4506f, 23.5217f, 2.6564f)
                curveTo(22.9368f, 1.7549f, 21.9338f, 1.5975f, 21.4376f, 1.5567f)
                curveTo(20.9216f, 1.5142f, 20.367f, 1.5595f, 19.9684f, 1.5956f)
                curveTo(19.9143f, 1.463f, 19.8398f, 1.3236f, 19.7368f, 1.186f)
                curveTo(19.1835f, 0.447f, 18.3252f, 0.3849f, 17.8421f, 0.3979f)
                curveTo(16.8661f, 0.4242f, 16.1027f, 0.8572f, 15.5509f, 1.3064f)
                curveTo(15.2613f, 1.5421f, 14.5722f, 2.5059f, 14.3467f, 2.7566f)
                curveTo(14.3467f, 1.8577f, 13.4807f, 1.4002f, 12.8718f, 1.3064f)
                curveTo(11.6897f, 1.1242f, 10.9575f, 1.8854f, 10.2768f, 2.7566f)
                curveTo(9.4283f, 3.8426f, 8.7715f, 5.2405f, 7.9953f, 6.2907f)
                curveTo(7.7278f, 6.1167f, 7.4111f, 5.7095f, 6.9047f, 5.733f)
                curveTo(6.0702f, 5.7718f, 5.5159f, 6.3374f, 5.1572f, 6.7245f)
                curveTo(4.5329f, 7.3981f, 4.1358f, 8.6218f, 3.8654f, 9.5329f)
                curveTo(3.5715f, 10.8512f, 3.371f, 11.4071f, 3.1329f, 12.8524f)
                curveTo(2.9913f, 13.7119f, 2.9195f, 14.3901f, 2.9195f, 15.35f)
                curveTo(2.757f, 15.6683f, 2.571f, 16.0103f, 2.3511f, 16.4351f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(7.0752f, 7.2816f)
                curveTo(7.3164f, 7.4935f, 7.5991f, 7.7419f, 8.209f, 7.5297f)
                curveTo(8.6078f, 7.391f, 9.1231f, 6.4871f, 9.6982f, 5.4786f)
                curveTo(10.5387f, 4.0044f, 11.5067f, 2.3066f, 12.4245f, 2.448f)
                curveTo(13.5315f, 2.6186f, 13.4634f, 3.2315f, 13.4109f, 3.7029f)
                curveTo(13.3742f, 4.0335f, 13.3452f, 4.2945f, 13.7348f, 4.2845f)
                curveTo(14.1318f, 4.2743f, 14.5181f, 3.8078f, 14.9664f, 3.2664f)
                curveTo(15.5856f, 2.5186f, 16.3232f, 1.628f, 17.3704f, 1.5998f)
                curveTo(18.1088f, 1.58f, 18.0971f, 1.8981f, 18.0853f, 2.2208f)
                curveTo(18.0777f, 2.427f, 18.0701f, 2.6351f, 18.2582f, 2.7579f)
                curveTo(18.4436f, 2.8789f, 18.9096f, 2.8366f, 19.4453f, 2.788f)
                curveTo(20.3042f, 2.7101f, 21.3423f, 2.616f, 21.6902f, 3.1521f)
                curveTo(21.9961f, 3.6235f, 21.6064f, 4.092f, 21.2329f, 4.5411f)
                curveTo(20.9165f, 4.9215f, 20.6117f, 5.2879f, 20.7511f, 5.6303f)
                curveTo(20.8647f, 5.9096f, 21.0981f, 6.067f, 21.3269f, 6.2213f)
                curveTo(21.701f, 6.4736f, 22.0629f, 6.7176f, 21.8686f, 7.4733f)
                curveTo(21.6632f, 8.2724f, 20.4275f, 8.4888f, 19.2691f, 8.6917f)
                curveTo(18.2988f, 8.8616f, 17.3827f, 9.0221f, 17.1723f, 9.5076f)
                curveTo(17.0403f, 9.8121f, 17.2593f, 9.9539f, 17.5045f, 10.1127f)
                curveTo(17.7976f, 10.3026f, 18.1283f, 10.5168f, 17.9429f, 11.0623f)
                curveTo(17.5094f, 12.3374f, 16.0617f, 12.3374f, 14.703f, 12.3374f)
                curveTo(13.5367f, 12.3374f, 12.4361f, 12.3375f, 12.0986f, 13.1438f)
                curveTo(11.9038f, 13.6094f, 12.1987f, 13.6915f, 12.5222f, 13.7817f)
                curveTo(12.9183f, 13.8921f, 13.3573f, 14.0145f, 12.9935f, 14.8667f)
                curveTo(12.2974f, 16.4975f, 9.7004f, 16.5121f, 7.323f, 16.5254f)
                curveTo(6.4513f, 16.5303f, 5.6092f, 16.535f, 4.901f, 16.6192f)
                curveTo(4.8202f, 16.6288f, 4.7485f, 16.6752f, 4.7062f, 16.7447f)
                curveTo(4.0546f, 17.8139f, 3.5924f, 18.9898f, 3.0995f, 20.2442f)
                curveTo(2.9912f, 20.5197f, 2.8814f, 20.7991f, 2.7678f, 21.0819f)
                curveTo(2.6177f, 21.4558f, 2.1928f, 21.6372f, 1.8189f, 21.487f)
                curveTo(1.445f, 21.3368f, 1.2637f, 20.912f, 1.4139f, 20.5381f)
                curveTo(1.9732f, 19.1455f, 2.6185f, 17.865f, 3.324f, 16.6895f)
                curveTo(4.4755f, 14.569f, 6.7454f, 11.9673f, 9.1458f, 10.0129f)
                curveTo(11.2816f, 8.2347f, 13.3212f, 6.9037f, 15.695f, 5.8938f)
                curveTo(15.9921f, 5.7674f, 15.9039f, 5.4199f, 15.5937f, 5.5095f)
                curveTo(13.4884f, 6.1179f, 11.6273f, 7.3247f, 9.6982f, 8.5409f)
                curveTo(7.6f, 9.8637f, 5.8417f, 11.6524f, 4.7597f, 12.953f)
                curveTo(4.5689f, 13.1823f, 4.1517f, 12.9937f, 4.2118f, 12.7015f)
                curveTo(4.6817f, 10.4158f, 5.1667f, 8.3181f, 5.8776f, 7.5509f)
                curveTo(6.5526f, 6.8225f, 6.7836f, 7.0254f, 7.0752f, 7.2816f)
                close()
            }
        }.build()

        return _leaflet!!
    }

@Suppress("ObjectPropertyName")
private var _leaflet: ImageVector? = null
