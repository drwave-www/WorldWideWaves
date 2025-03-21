package com.worldwidewaves.shared.di

import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.choreographies.IOSSoundPlayer

val IOSModule = module {
    single<SoundPlayer> { IOSSoundPlayer() }
}