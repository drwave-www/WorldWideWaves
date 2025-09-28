package com.worldwidewaves.shared.ios

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
fun installKNHook() {
    setUnhandledExceptionHook { t ->
        println("K/N Unhandled: ${t::class.qualifiedName}: ${t.message}")
        t.printStackTrace()
    }
}
