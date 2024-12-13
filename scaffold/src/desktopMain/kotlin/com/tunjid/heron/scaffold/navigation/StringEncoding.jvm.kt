package com.tunjid.heron.scaffold.navigation

import java.net.URLDecoder
import java.net.URLEncoder


internal actual fun String.encodeUrl(): String = URLEncoder.encode(this)

internal actual fun String.decodeUrl(): String = URLDecoder.decode(this)