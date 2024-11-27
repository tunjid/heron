package com.tunjid.heron

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform