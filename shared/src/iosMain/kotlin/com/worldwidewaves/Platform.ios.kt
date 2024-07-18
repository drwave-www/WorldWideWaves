package com.worldwidewaves

import com.worldwidewaves.shared.WWWPlatform
import platform.UIKit.UIDevice

class IOSPlatform: WWWPlatform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun getContext(): Any {
        TODO("Not yet implemented")
    }
}

