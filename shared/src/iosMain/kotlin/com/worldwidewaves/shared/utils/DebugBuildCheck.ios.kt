/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.worldwidewaves.shared.utils

/**
 * iOS implementation of debug build detection.
 * Uses LogConfig.ENABLE_DEBUG_LOGGING as proxy for debug state since
 * iOS doesn't have direct BuildConfig equivalent in shared code.
 */
actual fun isDebugBuild(): Boolean = LogConfig.ENABLE_DEBUG_LOGGING
