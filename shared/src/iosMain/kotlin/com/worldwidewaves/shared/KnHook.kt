package com.worldwidewaves.shared

import kotlin.native.setUnhandledExceptionHook

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun installKNHook() =
    setUnhandledExceptionHook { t ->
        println("🚨 K/N Unhandled: ${t::class.qualifiedName}: ${t.message}")
        t.printStackTrace()
    }
