package com.worldwidewaves.shared

interface WWWPlatform {
    val name: String

    fun getContext(): Any
}

