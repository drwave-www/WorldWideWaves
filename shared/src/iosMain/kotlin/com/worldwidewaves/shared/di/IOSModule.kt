package com.worldwidewaves.shared.di

import com.worldwidewaves.shared.choreographies.SoundPlayer
import com.worldwidewaves.shared.choreographies.IOSSoundPlayer

val IOSModule = module {
    single<SoundPlayer> { IOSSoundPlayer() }
}