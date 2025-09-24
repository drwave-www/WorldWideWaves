package com.worldwidewaves.shared.di

import com.worldwidewaves.shared.map.IOSMapLibreAdapter
import com.worldwidewaves.shared.map.IOSPlatformMapManager
import com.worldwidewaves.shared.map.IOSWWWLocationProvider
import com.worldwidewaves.shared.map.MapLibreAdapter
import com.worldwidewaves.shared.map.MapStateManager
import com.worldwidewaves.shared.map.PlatformMapManager
import com.worldwidewaves.shared.map.WWWLocationProvider
import com.worldwidewaves.shared.sound.IOSSoundPlayer
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.utils.IOSImageResolver
import com.worldwidewaves.shared.utils.ImageResolver
import org.koin.dsl.module
import platform.MapLibre.MLNMapView
import platform.UIKit.UIImage

val IOSModule =
    module {
        single<SoundPlayer> { IOSSoundPlayer() }
        single<ImageResolver<UIImage>> { IOSImageResolver() }
        single<WWWLocationProvider> { IOSWWWLocationProvider() }

        // Map services
        single<PlatformMapManager> { IOSPlatformMapManager() }
        single<MapLibreAdapter<MLNMapView>> { IOSMapLibreAdapter() }
        single { MapStateManager(get()) }
    }
