package com.worldwidewaves.shared

import com.worldwidewaves.shared.di.sharedModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule())
    }
}