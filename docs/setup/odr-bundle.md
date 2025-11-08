# iOS On-Demand Resources (ODR) Maps Implementation Plan

## 🎯 Project Overview

This document outlines the comprehensive plan to implement iOS On-Demand Resources (ODR) for map delivery, providing equivalent functionality to Android's Dynamic Feature Modules. The goal is to enable on-demand map downloads for iOS while maintaining UI/UX parity with Android.

## 📊 Current State Analysis

### ✅ **Existing iOS ODR Infrastructure**

- **`IOSPlatformMapManager`**: Complete ODR download/progress management using `NSBundleResourceRequest`
- **`IOSMapAvailabilityChecker`**: StateFlow-based availability tracking with reactive UI updates
- **`IOSMapViewModel`**: iOS UI lifecycle integration with platform-specific error handling
- **Comprehensive logging and error handling** throughout the stack

### ✅ **Android Play Features Reference Architecture**

- **44 dynamic feature modules** in `maps/android/` (e.g., `paris_france`, `new_york_usa`)
- **Two-file map data structure per city:**
  - `city_name.geojson` - Vector geometry data for wave calculations
  - `city_name.mbtiles` - Raster/vector tiles for MapLibre rendering
- **SplitInstallManager-based** download/availability system
- **Individual Gradle modules** with `android-dynamic-feature` plugin

### ❌ **Missing iOS Implementation Components**

- No ODR resource tagging in Xcode project configuration
- No iOS bundle configuration for map resources
- Map data not packaged for iOS ODR delivery
- Info.plist missing `NSBundleResourceRequestTags` configuration

## 🏗️ Implementation Strategy

### **Phase 1: Bundle Configuration and Resource Setup**

#### **1.1 iOS Bundle Structure**

```
iosApp/worldwidewaves/Resources/Maps/
├── paris_france/
│   ├── paris_france.geojson    # Vector geometry
│   └── paris_france.mbtiles    # Map tiles
├── new_york_usa/
│   ├── new_york_usa.geojson
│   └── new_york_usa.mbtiles
├── london_england/
│   ├── london_england.geojson
│   └── london_england.mbtiles
└── [... all 44 cities]
```

#### **1.2 Xcode ODR Configuration**

**Resource Tags in `project.pbxproj`:**

- Each city folder tagged with event ID (e.g., "paris_france")
- Resources marked as "On Demand" with appropriate categories
- Download priorities: Essential cities = High, Others = Normal

**Build Phases:**

- Pre-build script to sync map data from `maps/android/`
- Post-build validation of ODR tag integrity
- Bundle size optimization for app store submission

#### **1.3 Info.plist ODR Declaration**

```xml
<key>NSBundleResourceRequestTags</key>
<dict>
    <key>paris_france</key>
    <array>
        <string>paris_france.geojson</string>
        <string>paris_france.mbtiles</string>
    </array>
    <key>new_york_usa</key>
    <array>
        <string>new_york_usa.geojson</string>
        <string>new_york_usa.mbtiles</string>
    </array>
    <!-- Automatically generated for all 44 cities -->
</dict>
```

### **Phase 2: Build System Automation**

#### **2.1 Enhanced `40-generate-modules.sh`**

**Responsibilities:**

- Copy both `.geojson` AND `.mbtiles` from Android modules to iOS bundle
- Generate complete Info.plist ODR entries idempotently
- Handle Xcode's "Generate Info.plist File = Yes" setting
- Validate resource integrity and bundle consistency

**Key Features:**

- **Idempotent execution** - safe to run multiple times
- **Two-file support** - handles both geojson and mbtiles
- **Plist merging** - respects existing Xcode-generated entries
- **Validation** - ensures all resources are properly tagged

#### **2.2 Gradle Integration**

```kotlin
task syncMapsToIOS {
    group = "iOS ODR"
    description = "Sync map resources from Android modules to iOS ODR bundle"

    doLast {
        exec {
            workingDir rootProject.projectDir
            commandLine "bash", "scripts/40-generate-modules.sh"
        }
    }
}

// Integrate with iOS build
tasks.named("embedAndSignAppleFrameworkForXcode") {
    dependsOn("syncMapsToIOS")
}
```

### **Phase 3: ODR Resource Management**

#### **3.1 Resource Loading Strategy**

```kotlin
// IOSPlatformMapManager enhanced functionality
class IOSPlatformMapManager {
    fun downloadMap(mapId: String) {
        val request = NSBundleResourceRequest(tags = setOf(mapId))
        request.loadingPriority = NSBundleResourceRequestLoadingPriorityUrgent

        request.beginAccessingResourcesWithCompletionHandler { error ->
            if (error == null) {
                // Both geojson and mbtiles now available
                val geojsonPath = NSBundle.mainBundle.pathForResource(mapId, ofType = "geojson")
                val mbtilesPath = NSBundle.mainBundle.pathForResource(mapId, ofType = "mbtiles")
                onMapResourcesAvailable(geojsonPath, mbtilesPath)
            }
        }
    }
}
```

#### **3.2 MapLibre Integration**

- **Geojson**: Used for wave polygon calculations and area detection
- **Mbtiles**: Used for MapLibre base map rendering and visual display
- **Coordination**: Both resources must be available for full map functionality

### **Phase 4: Testing and Validation**

#### **4.1 Unit Tests (Kotlin)**

```kotlin
// IOSMapAvailabilityCheckerTest.kt
@Test fun `ODR availability detection works correctly`()
@Test fun `track maps updates state flow properly`()
@Test fun `concurrent availability checks handled safely`()

// IOSPlatformMapManagerTest.kt
@Test fun `download both geojson and mbtiles resources`()
@Test fun `progress updates work throughout download`()
@Test fun `cancellation works during active downloads`()
@Test fun `error handling covers all ODR failure modes`()

// IOSMapViewModelTest.kt
@Test fun `UI state updates correctly during download lifecycle`()
@Test fun `platform adapter integration works end-to-end`()
```

#### **4.2 Integration Tests (Swift)**

```swift
// ODRIntegrationTests.swift
func testMapResourceAvailability()
func testMapDownloadAndCaching()
func testResourceCleanupOnMemoryPressure()
func testMultipleConcurrentDownloads()
```

#### **4.3 End-to-End Tests**

- **Wave Screen**: Map loads and displays wave progression
- **Full Map Screen**: Complete map functionality with ODR resources
- **Simulation**: Map availability blocks/enables simulation correctly
- **Memory Management**: Resources released appropriately

## 🔧 Technical Considerations

### **Bundle Size Optimization**

- **Initial Bundle**: Core cities only (~5-10 maps)
- **ODR Delivery**: Remaining maps downloaded on-demand
- **Compression**: Optimize geojson/mbtiles for smaller downloads
- **Caching Strategy**: Intelligent retention based on usage patterns

### **Xcode "Generate Info.plist = Yes" Handling**

**Challenge**: Xcode overwrites Info.plist on builds
**Solution**:

- Use Info-Additions.plist for ODR entries
- Merge during build phase before ODR processing
- Preserve Xcode-generated entries while adding ODR configuration

### **Error Handling and Fallbacks**

- **Network failures**: Graceful degradation with cached data
- **Storage constraints**: Intelligent resource cleanup
- **ODR unavailable**: Fallback to essential maps or offline mode
- **User communication**: Clear progress and error messaging

### **Performance Optimization**

- **Concurrent downloads**: Max 3 simultaneous (iOS best practice)
- **Progress throttling**: 50ms update intervals for smooth UI
- **Memory efficiency**: Release unused resources proactively
- **Background downloads**: Continue downloads when app backgrounded

## 📋 Implementation Checklist

### **Phase 1: Foundation**

- [ ] Verify current ODR implementation works
- [ ] Analyze mbtiles support in existing code
- [ ] Create test iOS ODR bundle with 5 cities
- [ ] Configure basic Xcode ODR tags manually

### **Phase 2: Automation**

- [ ] Enhance `40-generate-modules.sh` for two-file support
- [ ] Implement Info.plist ODR entry generation
- [ ] Add Gradle integration for automated sync
- [ ] Handle Xcode "Generate Info.plist = Yes" scenario

### **Phase 3: Integration**

- [ ] Test availability checker with real ODR resources
- [ ] Verify MapLibre integration with both resource types
- [ ] Test Wave/FullMap screens with ODR maps
- [ ] Validate simulation button respects ODR availability

### **Phase 4: Production Ready**

- [ ] Comprehensive test suite for all ODR components
- [ ] Bundle size optimization and app store preparation
- [ ] Performance testing and memory management
- [ ] Error handling and edge case coverage

## 🚨 Critical Success Factors

1. **Two-File Support**: Both `.geojson` and `.mbtiles` must be handled correctly
2. **Idempotent Scripts**: Build scripts must be safe to run repeatedly
3. **Xcode Compatibility**: Must work with generated Info.plist files
4. **Test Coverage**: Comprehensive testing of all ODR edge cases
5. **Performance**: No degradation compared to Android Dynamic Features

## 📈 Success Metrics

- **Functional Parity**: iOS maps work identically to Android
- **Performance**: Download speeds comparable to Android Dynamic Features
- **Reliability**: 99%+ success rate for map downloads and availability detection
- **Bundle Size**: Optimized initial download with effective ODR delivery
- **User Experience**: Seamless map access with appropriate loading states

## 🔄 Rollback Strategy

If issues arise during implementation:

1. **Graceful Degradation**: Fall back to bundled essential maps
2. **Feature Flags**: Disable ODR temporarily while maintaining core functionality
3. **Manual Override**: Allow manual map selection for testing
4. **Monitoring**: Comprehensive logging for issue diagnosis

---

**Next Steps**: Begin Phase 1 verification of current ODR implementation, then proceed systematically through each phase with proper testing at each stage.
