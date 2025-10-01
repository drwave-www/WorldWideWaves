package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.makeMainViewController
import platform.UIKit.UIViewController
import platform.UIKit.navigationController

const val TAG = "IOS Helpers"

internal fun UIViewController.finishIOS() {
    // If presented modally, dismiss; otherwise try to pop from a navigation stack.
    when {
        this.presentingViewController != null -> {
            this.dismissViewControllerAnimated(true, completion = null)
            Log.d(TAG, "finishIOS: dismissed modal VC")
        }
        this.navigationController != null -> {
            this.navigationController?.popViewControllerAnimated(true)
            Log.d(TAG, "finishIOS: popped from navigation stack")
        }
        else -> {
            // You can't "close" an iOS app from code (App Store-rejected).
            // The right fallback when there's no presenter/nav is
            // to replace the root VC with your main screen.
            val newRoot = makeMainViewController()
            val win = this.view.window
            if (win != null) {
                win.rootViewController = newRoot
                win.makeKeyAndVisible()
                Log.i(TAG, "finishIOS: replaced root with main VC")
            } else {
                Log.w(TAG, "finishIOS: no window; cannot replace root")
            }
        }
    }
}
