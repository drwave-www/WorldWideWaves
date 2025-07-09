# BUILD_STATUS.md
WorldWideWaves – `feature/ios-adaptation`  
_(Last updated: 2025-07-09)_

---

## 1. Compilation-Fix Summary
| Area | Fix |
|------|-----|
| **Koin Init** | Replaced ambiguous `GlobalContext` call with internal `koinApp` guard in `Helper.kt` (`doInitKoin()`). |
| **Platform Abstraction** | Removed invalid `getPlatform()` `actual` in `Platform.ios.kt`; iOS now uses common `WWWPlatform` via composition. |
| **Image Resolver** | Cleaned `IOSImageResolver`: removed faulty `override`, switched to `CGImage.cropping(to:)`. |
| **Sound Pipeline** | Moved `IOSSoundPlayer` to `sound` package; added `IOSAudioBuffer` + `AudioBufferFactory actual`. |
| **Reflection Removal** | Replaced JVM reflection in `WWWEventWaveWarming` with Napier debug logging. |
| **Opt-in & Imports** | Added `@OptIn(ExperimentalForeignApi::class)` where needed and fixed missing imports. |
| **Build Scripts** | Xcode “Compile Kotlin Framework” phase left as-is but documented below. |

---

## 2. Current Build Status
| Target | Result |
|--------|--------|
| Gradle `:shared:compileKotlinIosArm64` | ✅ Compiles (requires JDK 17+) |
| **Xcode** (Debug, Simulator iPhone 15) | ✅ Clean & build succeed after cache clean |
| Android debug build | ✅ Continues to build & run |

---

## 3. Fresh Build & Test Guide
1. **Clean caches**
   ```bash
   ./gradlew clean   # root
   ```
2. **iOS shared framework**
   ```bash
   ./gradlew :shared:podPublishReleaseXCFramework
   # or
   ./gradlew :shared:compileKotlinIosSimulatorArm64
   ```
3. **Open Xcode workspace**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
4. **Select simulator** (e.g. *iPhone 15*) → Build/Run.  
   The app should show SplashView, then MainView with 3 tabs.

---

## 4. Next Development Steps
Follow `NEXT_STEPS_ORDER.md`. Immediate tasks:
1. Splash artwork & adaptive logo sizing  
2. Persist selected tab state in `MainView`  
3. Hook Kotlin Flow into `EventsListView` refresh

_All new work stays on `feature/ios-adaptation` – small, focused commits + PRs._

---

## 5. Troubleshooting Tips
| Symptom | Resolution |
|---------|------------|
| **“Compile Kotlin Framework will run every build”** | In Xcode ➜ Build Phases ➜ *Compile Kotlin Framework* ➜ uncheck **Based on dependency analysis** or add output files (`${SRCROOT}/shared/build/…/Shared.framework`). |
| **`Unable to locate a Java Runtime`** | Install JDK 17 and ensure `JAVA_HOME` is set. |
| **`Unresolved reference GlobalContext` persists** | Xcode cache: `Shift+Cmd+K` (clean build folder) or delete `~/Library/Developer/Xcode/DerivedData`. |
| **AVFoundation permission errors** | Add `NSMicrophoneUsageDescription` to `Info.plist`. |
| **Gradle memory errors** | Add `org.gradle.jvmargs=-Xmx4g` in `gradle.properties`. |
| **Duplicate symbol when linking** | Run **Product ► Clean Build Folder**, ensure only one copy of `Shared.framework` is embedded. |

---

Happy coding!  
