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

package com.tunjid.heron.profile

import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.brand.BackerKit
import com.tunjid.heron.ui.icons.brand.BigCartel
import com.tunjid.heron.ui.icons.brand.Boosty
import com.tunjid.heron.ui.icons.brand.Booth
import com.tunjid.heron.ui.icons.brand.BuyMeACoffee
import com.tunjid.heron.ui.icons.brand.CashApp
import com.tunjid.heron.ui.icons.brand.Chuffed
import com.tunjid.heron.ui.icons.brand.Crowdfundr
import com.tunjid.heron.ui.icons.brand.Donorbox
import com.tunjid.heron.ui.icons.brand.Etsy
import com.tunjid.heron.ui.icons.brand.Fantia
import com.tunjid.heron.ui.icons.brand.Fourthwall
import com.tunjid.heron.ui.icons.brand.GiveSendGo
import com.tunjid.heron.ui.icons.brand.Givebutter
import com.tunjid.heron.ui.icons.brand.GoFundMe
import com.tunjid.heron.ui.icons.brand.Gumroad
import com.tunjid.heron.ui.icons.brand.Indiegogo
import com.tunjid.heron.ui.icons.brand.Inprnt
import com.tunjid.heron.ui.icons.brand.ItchIo
import com.tunjid.heron.ui.icons.brand.JustGiving
import com.tunjid.heron.ui.icons.brand.Kickstarter
import com.tunjid.heron.ui.icons.brand.KoFi
import com.tunjid.heron.ui.icons.brand.Liberapay
import com.tunjid.heron.ui.icons.brand.OpenCollective
import com.tunjid.heron.ui.icons.brand.Patreon
import com.tunjid.heron.ui.icons.brand.PayPal
import com.tunjid.heron.ui.icons.brand.PixivFanbox
import com.tunjid.heron.ui.icons.brand.Redbubble
import com.tunjid.heron.ui.icons.brand.Skeb
import com.tunjid.heron.ui.icons.brand.Stripe
import com.tunjid.heron.ui.icons.brand.SubscribeStar
import com.tunjid.heron.ui.icons.brand.Substack
import com.tunjid.heron.ui.icons.brand.Throne
import com.tunjid.heron.ui.icons.brand.Venmo
import com.tunjid.heron.ui.icons.brand.Vgen
import com.tunjid.heron.ui.icons.brand.Zeffy
import com.tunjid.heron.ui.icons.regular.Public
import com.tunjid.heron.ui.shapes.Circle

@Composable
fun ProfileLinkIcon(
    modifier: Modifier = Modifier,
    url: GenericUri,
    favicon: ImageUri?,
) {
    val siteIcon = remember(url) {
        url.siteIcon()
    }
    when {
        siteIcon != null -> Icon(
            modifier = modifier,
            imageVector = siteIcon,
            contentDescription = null,
        )
        favicon != null -> AsyncImage(
            modifier = modifier,
            args = remember(favicon) {
                ImageArgs(
                    url = favicon.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    shape = PolygonShape.Circle,
                )
            },
        )
        else -> Icon(
            modifier = modifier,
            imageVector = HeronIcons.Regular.Public,
            contentDescription = null,
        )
    }
}

private fun GenericUri.siteIcon(): ImageVector? {
    val host = runCatching { domain.lowercase() }.getOrNull() ?: return null
    return SiteIcons.firstNotNullOfOrNull { (domain, icon) ->
        if (host == domain || host.endsWith(".$domain")) icon()
        else null
    }
}

private val SiteIcons: List<Pair<String, () -> ImageVector>> = listOf(
    "backerkit.com" to { HeronIcons.Brand.BackerKit },
    "bigcartel.com" to { HeronIcons.Brand.BigCartel },
    "boosty.to" to { HeronIcons.Brand.Boosty },
    "booth.pm" to { HeronIcons.Brand.Booth },
    "buymeacoffee.com" to { HeronIcons.Brand.BuyMeACoffee },
    "cash.app" to { HeronIcons.Brand.CashApp },
    "chuffed.org" to { HeronIcons.Brand.Chuffed },
    "crowdfundr.com" to { HeronIcons.Brand.Crowdfundr },
    "donorbox.org" to { HeronIcons.Brand.Donorbox },
    "etsy.com" to { HeronIcons.Brand.Etsy },
    "fantia.jp" to { HeronIcons.Brand.Fantia },
    "fourthwall.com" to { HeronIcons.Brand.Fourthwall },
    "givebutter.com" to { HeronIcons.Brand.Givebutter },
    "givesendgo.com" to { HeronIcons.Brand.GiveSendGo },
    "gofundme.com" to { HeronIcons.Brand.GoFundMe },
    "gumroad.com" to { HeronIcons.Brand.Gumroad },
    "indiegogo.com" to { HeronIcons.Brand.Indiegogo },
    "inprnt.com" to { HeronIcons.Brand.Inprnt },
    "itch.io" to { HeronIcons.Brand.ItchIo },
    "justgiving.com" to { HeronIcons.Brand.JustGiving },
    "kickstarter.com" to { HeronIcons.Brand.Kickstarter },
    "ko-fi.com" to { HeronIcons.Brand.KoFi },
    "liberapay.com" to { HeronIcons.Brand.Liberapay },
    "opencollective.com" to { HeronIcons.Brand.OpenCollective },
    "patreon.com" to { HeronIcons.Brand.Patreon },
    "paypal.com" to { HeronIcons.Brand.PayPal },
    "paypal.me" to { HeronIcons.Brand.PayPal },
    "fanbox.cc" to { HeronIcons.Brand.PixivFanbox },
    "redbubble.com" to { HeronIcons.Brand.Redbubble },
    "skeb.jp" to { HeronIcons.Brand.Skeb },
    "stripe.com" to { HeronIcons.Brand.Stripe },
    "subscribestar.com" to { HeronIcons.Brand.SubscribeStar },
    "substack.com" to { HeronIcons.Brand.Substack },
    "throne.com" to { HeronIcons.Brand.Throne },
    "venmo.com" to { HeronIcons.Brand.Venmo },
    "vgen.co" to { HeronIcons.Brand.Vgen },
    "zeffy.com" to { HeronIcons.Brand.Zeffy },
)
