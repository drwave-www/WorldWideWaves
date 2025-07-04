package com.worldwidewaves.shared.di

import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.choreographies.IOSSoundPlayer
import com.worldwidewaves.shared.utils.IOSImageResolver
import com.worldwidewaves.shared.utils.ImageResolver
import org.koin.dsl.module
import platform.UIKit.UIImage

val IOSModule = module {
    single<SoundPlayer> { IOSSoundPlayer() }
    single<ImageResolver<UIImage>> { IOSImageResolver() }
}