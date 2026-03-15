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

package com.tunjid.heron.timeline.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Pckt: ImageVector
    get() {
        if (_Pckt != null) {
            return _Pckt!!
        }
        _Pckt = ImageVector.Builder(
            name = "Pckt",
            defaultWidth = 93.dp,
            defaultHeight = 107.dp,
            viewportWidth = 93f,
            viewportHeight = 107f,
        ).apply {
            // Oval
            path(fill = SolidColor(Color.Black)) {
                moveTo(43f, 28.4316f)
                curveTo(49.0182f, 28.4316f, 50.9999f, 32.9208f, 51f, 40.4316f)
                curveTo(51f, 47.9427f, 49.0183f, 52.4316f, 43f, 52.4316f)
                curveTo(36.9817f, 52.4316f, 35f, 47.9427f, 35f, 40.4316f)
                curveTo(35.0001f, 32.9208f, 36.9818f, 28.4316f, 43f, 28.4316f)
                close()
            }
            // Outline with cutouts
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(51.3115f, 0f)
                curveTo(61.4858f, 0f, 70.1235f, 3.411f, 76.0547f, 10.7363f)
                curveTo(76.6352f, 11.4533f, 77.1811f, 12.2001f, 77.6963f, 12.9736f)
                curveTo(80.0589f, 14.5183f, 82.1905f, 16.4339f, 84.0547f, 18.7363f)
                curveTo(89.8115f, 25.8465f, 92.3203f, 35.7949f, 92.3203f, 47.5361f)
                curveTo(92.3203f, 56.8683f, 90.7323f, 65.0802f, 87.1973f, 71.6416f)
                curveTo(87.555f, 71.8872f, 87.8993f, 72.1614f, 88.2275f, 72.4639f)
                curveTo(89.7962f, 73.9093f, 90.7073f, 75.7198f, 91.2803f, 77.3252f)
                lineTo(91.3135f, 77.4189f)
                lineTo(91.3447f, 77.5146f)
                curveTo(92.0855f, 79.8314f, 92.5794f, 83.2003f, 90.6436f, 86.3193f)
                curveTo(90.2445f, 86.9622f, 89.7874f, 87.5179f, 89.2939f, 88f)
                curveTo(89.3984f, 88.6569f, 89.4353f, 89.3495f, 89.3809f, 90.0762f)
                curveTo(89.105f, 93.7548f, 86.7034f, 96.2192f, 84.6309f, 97.6953f)
                lineTo(84.627f, 97.6982f)
                curveTo(83.0852f, 98.7944f, 80.886f, 99.9999f, 78.124f, 100f)
                curveTo(76.524f, 99.9999f, 75.153f, 99.6078f, 73.9863f, 98.9902f)
                curveTo(72.8052f, 99.6106f, 71.4261f, 100f, 69.832f, 100f)
                curveTo(67.1912f, 99.9998f, 64.9313f, 98.7943f, 63.3896f, 97.6982f)
                lineTo(63.3193f, 97.6475f)
                curveTo(61.6106f, 96.3947f, 59.5544f, 94.3932f, 58.8428f, 91.4941f)
                curveTo(57.4844f, 91.0415f, 56.3089f, 90.3518f, 55.3896f, 89.6982f)
                lineTo(55.3193f, 89.6475f)
                curveTo(54.2639f, 88.8736f, 53.0762f, 87.8138f, 52.1543f, 86.4336f)
                curveTo(51.3191f, 86.2498f, 50.4993f, 86.0361f, 49.6963f, 85.79f)
                verticalLineTo(96.6885f)
                curveTo(49.6962f, 98.8934f, 49.139f, 101.688f, 46.9102f, 103.862f)
                curveTo(44.7096f, 106.009f, 41.9236f, 106.528f, 39.7275f, 106.528f)
                horizontalLineTo(17.8398f)
                curveTo(15.6351f, 106.528f, 12.8798f, 105.972f, 10.7178f, 103.811f)
                curveTo(9.0909f, 102.184f, 8.3736f, 100.221f, 8.1182f, 98.4092f)
                curveTo(6.307f, 98.1536f, 4.3444f, 97.4371f, 2.7178f, 95.8105f)
                curveTo(0.5559f, 93.6486f, 0.0001f, 90.8933f, 0f, 88.6885f)
                verticalLineTo(11.1201f)
                curveTo(0f, 8.9153f, 0.5557f, 6.1592f, 2.7178f, 3.9971f)
                curveTo(4.8798f, 1.8353f, 7.6351f, 1.2803f, 9.8398f, 1.2803f)
                horizontalLineTo(31.7275f)
                curveTo(33.5137f, 1.2803f, 35.6607f, 1.6234f, 37.5752f, 2.8672f)
                curveTo(41.6669f, 0.9766f, 46.2905f, 0.0001f, 51.3115f, 0f)
                close()
                // Inner P shape
                moveTo(51.3115f, 6f)
                curveTo(45.0398f, 6.0001f, 39.7922f, 7.7923f, 35.6963f, 10.9922f)
                curveTo(35.6963f, 8.5602f, 34.4155f, 7.2803f, 31.7275f, 7.2803f)
                horizontalLineTo(9.8398f)
                curveTo(7.28f, 7.2803f, 6f, 8.5602f, 6f, 11.1201f)
                verticalLineTo(88.6885f)
                curveTo(6.0002f, 91.2481f, 7.2801f, 92.5283f, 9.8398f, 92.5283f)
                horizontalLineTo(31.7275f)
                curveTo(34.4154f, 92.5283f, 35.6961f, 91.2482f, 35.6963f, 88.6885f)
                verticalLineTo(68.0801f)
                curveTo(39.8495f, 71.2264f, 44.9925f, 73.1332f, 51.0039f, 73.1963f)
                curveTo(51.036f, 72.2441f, 51.2486f, 71.3016f, 51.5518f, 70.4229f)
                curveTo(51.9966f, 69.03f, 52.6684f, 67.6698f, 53.7695f, 66.6406f)
                curveTo(54.978f, 65.5113f, 56.4604f, 65.0049f, 58.0195f, 65.0049f)
                curveTo(58.2332f, 65.0049f, 58.4479f, 65.0174f, 58.6631f, 65.0381f)
                curveTo(58.8865f, 64.059f, 59.3111f, 63.1152f, 60.0332f, 62.2871f)
                curveTo(61.593f, 60.4986f, 63.8728f, 60f, 66.0088f, 60f)
                curveTo(68.1185f, 60f, 70.4052f, 60.5007f, 71.957f, 62.2988f)
                curveTo(72.1747f, 62.5511f, 72.3616f, 62.816f, 72.5264f, 63.0869f)
                curveTo(76.3427f, 57.4793f, 78.3203f, 49.5894f, 78.3203f, 39.5361f)
                curveTo(78.3203f, 17.5201f, 68.8475f, 6f, 51.3115f, 6f)
                close()
                // Star/asterisk cutout
                moveTo(66.0088f, 63f)
                curveTo(62.4983f, 63f, 61.1066f, 64.6691f, 61.5303f, 68.0654f)
                lineTo(61.7715f, 69.3164f)
                lineTo(60.6221f, 68.7207f)
                curveTo(59.6538f, 68.2441f, 58.7458f, 68.0059f, 58.0195f, 68.0059f)
                curveTo(56.2643f, 68.0059f, 55.114f, 69.0785f, 54.3877f, 71.4023f)
                curveTo(53.2985f, 74.5601f, 54.5092f, 76.4071f, 57.959f, 77.0029f)
                lineTo(59.2295f, 77.1816f)
                lineTo(58.3223f, 78.1943f)
                curveTo(55.8408f, 80.5777f, 56.0219f, 82.7232f, 58.8662f, 84.8086f)
                curveTo(59.9555f, 85.5831f, 60.9848f, 85.9999f, 61.832f, 86f)
                curveTo(63.224f, 86f, 64.435f, 85.0471f, 65.4033f, 83.2002f)
                lineTo(66.0088f, 82.0674f)
                lineTo(66.6143f, 83.2002f)
                curveTo(67.5825f, 85.0469f, 68.7322f, 85.9999f, 70.124f, 86f)
                curveTo(71.0924f, 86f, 72.061f, 85.5832f, 73.1504f, 84.8086f)
                curveTo(75.9951f, 82.7827f, 76.1163f, 80.5778f, 73.6953f, 78.1348f)
                lineTo(72.8477f, 77.1816f)
                lineTo(74.0586f, 77.0029f)
                curveTo(77.5687f, 76.4071f, 78.6585f, 74.56f, 77.6299f, 71.3428f)
                curveTo(76.8431f, 69.1382f, 75.6927f, 68.0655f, 74.0586f, 68.0654f)
                curveTo(73.2718f, 68.0654f, 72.4244f, 68.3036f, 71.3955f, 68.7803f)
                lineTo(70.1846f, 69.3164f)
                lineTo(70.3662f, 68.0654f)
                curveTo(70.9109f, 64.6691f, 69.4586f, 63.0001f, 66.0088f, 63f)
                close()
            }
        }.build()

        return _Pckt!!
    }

@Suppress("ObjectPropertyName")
private var _Pckt: ImageVector? = null
