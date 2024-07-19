package com.worldwidewaves.shared

import platform.UIKit.UIDevice

class IOSPlatform: WWWPlatform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun getContext(): Any {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): WWWPlatform = IOSPlatform()

actual fun getImage(type: String, id: String): Any? {
    TODO("Not yet implemented")
}

//actual fun readEventsConfig(): String {
//    return "" // TODO
//}