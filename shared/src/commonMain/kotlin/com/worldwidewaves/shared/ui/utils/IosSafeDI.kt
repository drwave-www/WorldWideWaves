package com.worldwidewaves.shared.ui.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.localization.LocalizationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS-safe dependency injection singleton for Compose components.
 *
 * ## The iOS Deadlock Problem
 * On iOS (Kotlin/Native), creating objects with dependency injection **inside @Composable functions**
 * causes immediate deadlocks due to threading restrictions:
 *
 * ```kotlin
 * // ❌ CAUSES iOS DEADLOCK
 * @Composable
 * fun MyScreen() {
 *     val deps = object : KoinComponent {
 *         val clock by inject()  // DEADLOCKS on iOS!
 *     }
 * }
 * ```
 *
 * ## The Solution: File-Level Singleton
 * This singleton is declared at file level (not inside @Composable), resolving dependencies
 * ONCE when the object is first accessed, then caching them:
 *
 * ```kotlin
 * // ✅ SAFE: File-level object
 * object IosSafeDI : KoinComponent {
 *     val platform: WWWPlatform by inject()  // Resolved once, cached
 *     val clock: IClock by inject()          // Resolved once, cached
 * }
 * ```
 *
 * ## Usage in Compose
 * Use the helper functions instead of direct Koin DI:
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     // ✅ CORRECT: Use pre-resolved dependencies
 *     val platform = getIosSafePlatform()
 *     val clock = getIosSafeClock()
 *
 *     // ❌ WRONG: Don't create new DI objects
 *     // val vm = object : KoinComponent { val clock by inject() }
 * }
 * ```
 *
 * ## Why File-Level Works
 * - **Composition phase**: Running on specific thread, can't safely access DI
 * - **File-level initialization**: Happens during class loading, before composition
 * - **Singleton**: Dependencies resolved once, reused safely across all compositions
 *
 * ## Adding New Dependencies
 * When you need a new dependency in Compose components:
 *
 * ```kotlin
 * object IosSafeDI : KoinComponent {
 *     val platform: WWWPlatform by inject()
 *     val clock: IClock by inject()
 *     val newDependency: MyService by inject()  // Add here
 * }
 *
 * // Add helper function
 * fun getIosSafeMyService(): MyService = IosSafeDI.newDependency
 * ```
 *
 * ## Testing
 * For tests, initialize Koin before accessing IosSafeDI:
 *
 * ```kotlin
 * @BeforeTest
 * fun setup() {
 *     startKoin {
 *         modules(testModule {
 *             single<WWWPlatform> { MockPlatform() }
 *             single<IClock> { MockClock() }
 *         })
 *     }
 *     // Now IosSafeDI can be safely accessed
 * }
 * ```
 *
 * ## Related Documentation
 * - See CLAUDE_iOS.md for complete iOS deadlock prevention guide
 * - See iOS_VIOLATION_TRACKER.md for historical violations and fixes
 * - Run `./scripts/verify-ios-safety.sh` to detect violations
 *
 * @see RootController for iOS ViewController integration
 * @see SystemClock for another iOS-safe DI pattern (lazy initialization)
 */
object IosSafeDI : KoinComponent {
    /**
     * Pre-resolved WWWPlatform instance.
     *
     * Resolved once when IosSafeDI is first accessed, then cached.
     * Safe to access from @Composable functions via [getIosSafePlatform].
     */
    val platform: WWWPlatform by inject()

    /**
     * Pre-resolved IClock instance.
     *
     * Resolved once when IosSafeDI is first accessed, then cached.
     * Safe to access from @Composable functions via [getIosSafeClock].
     */
    val clock: IClock by inject()

    /**
     * Pre-resolved LocalizationManager instance.
     *
     * Resolved once when IosSafeDI is first accessed, then cached.
     * Safe to access from @Composable functions via [getIosSafeLocalizationManager].
     *
     * Use this to observe runtime locale changes and trigger UI recomposition:
     * ```kotlin
     * @Composable
     * fun MyScreen() {
     *     val localizationManager = getIosSafeLocalizationManager()
     *     val currentLocale by localizationManager.localeChanges.collectAsState()
     *
     *     key(currentLocale) {
     *         // UI that needs to recompose on locale change
     *     }
     * }
     * ```
     */
    val localizationManager: LocalizationManager by inject()
}

/**
 * Returns the iOS-safe platform instance.
 *
 * Safe to call from @Composable functions as it accesses pre-resolved
 * dependency from [IosSafeDI] singleton.
 *
 * @return Cached WWWPlatform instance
 * @throws UninitializedPropertyAccessException if Koin not initialized before IosSafeDI access
 */
fun getIosSafePlatform(): WWWPlatform = IosSafeDI.platform

/**
 * Returns the iOS-safe clock instance.
 *
 * Safe to call from @Composable functions as it accesses pre-resolved
 * dependency from [IosSafeDI] singleton.
 *
 * @return Cached IClock instance
 * @throws UninitializedPropertyAccessException if Koin not initialized before IosSafeDI access
 */
fun getIosSafeClock(): IClock = IosSafeDI.clock

/**
 * Returns the iOS-safe LocalizationManager instance.
 *
 * Safe to call from @Composable functions as it accesses pre-resolved
 * dependency from [IosSafeDI] singleton.
 *
 * Use this to observe locale changes and trigger UI recomposition:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val localizationManager = getIosSafeLocalizationManager()
 *     val currentLocale by localizationManager.localeChanges.collectAsState()
 *
 *     key(currentLocale) {
 *         // Your UI components that need recomposition on locale change
 *     }
 * }
 * ```
 *
 * @return Cached LocalizationManager instance
 * @throws UninitializedPropertyAccessException if Koin not initialized before IosSafeDI access
 */
fun getIosSafeLocalizationManager(): LocalizationManager = IosSafeDI.localizationManager
