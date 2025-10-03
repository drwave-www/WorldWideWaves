package com.worldwidewaves.shared.ui.theme

/* * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIApplication
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityMedium
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraSmall
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryMedium
import platform.UIKit.UIContentSizeCategorySmall

/**
 * Returns text scaling factor based on iOS Dynamic Type settings.
 *
 * Supports all 12 iOS text size categories:
 * - 7 standard sizes (Extra Small to XXX Large): 0.8x - 1.4x
 * - 5 accessibility sizes (Medium to XXX Large): 1.6x - 3.0x
 *
 * Users configure this via Settings → Accessibility → Display & Text Size → Larger Text
 */
@Composable
actual fun rememberDynamicTypeScale(): Float =
    remember {
        val category = UIApplication.sharedApplication.preferredContentSizeCategory
        when (category) {
            // Standard text size categories
            UIContentSizeCategoryExtraSmall -> 0.8f
            UIContentSizeCategorySmall -> 0.9f
            UIContentSizeCategoryMedium -> 1.0f
            UIContentSizeCategoryLarge -> 1.1f // iOS default
            UIContentSizeCategoryExtraLarge -> 1.2f
            UIContentSizeCategoryExtraExtraLarge -> 1.3f
            UIContentSizeCategoryExtraExtraExtraLarge -> 1.4f

            // Accessibility text size categories (larger increments)
            UIContentSizeCategoryAccessibilityMedium -> 1.6f
            UIContentSizeCategoryAccessibilityLarge -> 1.9f
            UIContentSizeCategoryAccessibilityExtraLarge -> 2.2f
            UIContentSizeCategoryAccessibilityExtraExtraLarge -> 2.6f
            UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 3.0f

            // Fallback for unknown categories
            else -> 1.0f
        }
    }
