package com.worldwidewaves.shared

interface WWWPlatform {
    val name: String

    fun getContext(): Any
}

expect fun getPlatform(): WWWPlatform

expect fun getImage(type: String, id: String): Any? // expect