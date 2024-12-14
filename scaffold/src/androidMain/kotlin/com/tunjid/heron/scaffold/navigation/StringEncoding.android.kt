package com.tunjid.heron.scaffold.navigation

import android.net.Uri

internal actual fun String.encodeUrl(): String = Uri.encode(this)

internal actual fun String.decodeUrl(): String = Uri.decode(this)