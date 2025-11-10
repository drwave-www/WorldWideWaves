# CrashlyticsBridge Static Library Implementation Plan

**Status**: NOT YET IMPLEMENTED
**Priority**: HIGH - Blocks iOS Crashlytics functionality
**Estimated Time**: 2-3 hours for complete implementation
**Created**: 2025-11-10

---

## Executive Summary

iOS builds 35-38 crash on launch with:
```
symbol not found in flat namespace '_OBJC_CLASS_$_CrashlyticsBridge'
```

**Root Cause**: iOS app executables (MH_EXECUTE) cannot export Objective-C class symbols for dynamic frameworks (MH_DYLIB) to import at runtime. This is a fundamental Mach-O architecture limitation.

**Solution**: Create a static library (`.a` file) containing CrashlyticsBridge, link it to both Shared.framework (at framework build time) and iOS app (at app build time).

---

## Problem Analysis

### Current Architecture (Broken)

```
Shared.framework (Kotlin/Native)
  uses: linkerOpts("-Wl,-U,_OBJC_CLASS_\$_CrashlyticsBridge")
  expects: iOS app to provide this symbol at runtime
        ↓
iOS App (MH_EXECUTE)
  has: CrashlyticsBridge.m compiled into app
  symbol: 0000000100135150 s _OBJC_CLASS_$_CrashlyticsBridge
          ↑ lowercase 's' = PRIVATE (not exported)
        ↓
Runtime (dyld):
  Shared.framework tries to resolve symbol
  Can't find it (apps don't export symbols)
  CRASH: symbol not found
```

### What We Tried (All Failed)

1. **Swift @objc implementation** → Linker stripped Objective-C metadata
2. **Objective-C implementation** → Symbol exists but private
3. **Linker flags** (`-Wl,-exported_symbol`, `-rdynamic`, exported_symbols_list) → Ignored (apps don't export)
4. **DEAD_CODE_STRIPPING = NO** → Symbol exists but still private
5. **NSClassFromString reference** → Runtime-only, no link-time effect
6. **AppDelegate hard reference** → Uses Swift symbols, not ObjC symbols

**Conclusion**: No combination of linker flags or code patterns can make an iOS app export Objective-C symbols.

### Authoritative Confirmation

From ChatGPT (confirmed by iOS Mach-O documentation):

> "You can't export an Objective-C class from an iOS app executable in a way that a dynamic framework can reliably link to it at runtime. MH_EXECUTE binaries on iOS are consumers, not providers."

**Recommended solution**: Static library that both framework and app link against.

---

## Solution Architecture

### New Architecture (Working)

```
libCrashlyticsBridge.a (static library)
  contains: CrashlyticsBridge.m/.h + Firebase calls
  exports: _OBJC_CLASS_$_CrashlyticsBridge (public symbol)
        ↓ linked at framework build time
Shared.framework (Kotlin/Native)
  links: libCrashlyticsBridge.a
  resolves: _OBJC_CLASS_$_CrashlyticsBridge at link time (not runtime)
        ↓ linked at app build time
iOS App
  links: Shared.framework + libCrashlyticsBridge.a + Firebase
  result: All symbols consistent, no runtime lookup needed
```

### Why This Works

1. **Static library can be linked by Kotlin/Native** - konan linker accepts `.a` files
2. **Symbol resolved at framework build time** - no runtime lookup
3. **Firebase available** - static library project can depend on Firebase SPM
4. **Testable** - static library can have its own XCTest target
5. **No export needed** - symbol is internal to linked binary (framework)

---

## Implementation Steps

### Phase 1: Create Static Library Xcode Project

#### 1.1. Project Structure

```
iosApp/
  CrashlyticsBridge/
    CrashlyticsBridge.xcodeproj/
      project.pbxproj (generated)
      xcshareddata/
        xcschemes/
          CrashlyticsBridge.xcscheme
    CrashlyticsBridge/
      CrashlyticsBridge.h (moved from iosApp/worldwidewaves/Utils/)
      CrashlyticsBridge.m (moved from iosApp/worldwidewaves/Utils/)
    CrashlyticsBridgeTests/
      CrashlyticsBridgeTests.m (created)
      Info.plist (generated)
    build/
      libCrashlyticsBridge.a (build output)
```

#### 1.2. Generate project.pbxproj Programmatically

**Script**: `scripts/ios/generate-crashlytics-library-project.py`

**Key Components**:

1. **PBXProject** object:
   - Package dependency: Firebase iOS SDK (SPM)
   - Targets: CrashlyticsBridge (library), CrashlyticsBridgeTests (tests)
   - Configurations: Debug, Release

2. **PBXNativeTarget** (CrashlyticsBridge):
   - Product type: `com.apple.product-type.library.static`
   - Build phases: Sources, Frameworks
   - Dependencies: FirebaseCrashlytics (SPM)
   - Build settings:
     - `EXECUTABLE_PREFIX = "lib"`
     - `PRODUCT_NAME = "CrashlyticsBridge"`
     - `SKIP_INSTALL = YES`
     - `IPHONEOS_DEPLOYMENT_TARGET = 16.0`
     - `SUPPORTED_PLATFORMS = "iphoneos iphonesimulator"`

3. **PBXSourcesBuildPhase**:
   - CrashlyticsBridge.m (compile)
   - CrashlyticsBridge.h (headers, public visibility)

4. **XCRemoteSwiftPackageReference**:
   ```xml
   repositoryURL = "https://github.com/firebase/firebase-ios-sdk"
   requirement = { kind = upToNextMajorVersion; minimumVersion = 12.4.0; }
   ```

5. **PBXNativeTarget** (Tests):
   - Product type: `com.apple.product-type.bundle.unit-test`
   - Test host: None (library tests don't need host)
   - Dependencies: FirebaseCrashlytics

#### 1.3. Create CrashlyticsBridgeTests.m

```objc
/*
 * Copyright 2025 DrWave
 * Unit tests for CrashlyticsBridge static library
 */

#import <XCTest/XCTest.h>
#import "CrashlyticsBridge.h"
#import <FirebaseCrashlytics/FirebaseCrashlytics.h>

@interface CrashlyticsBridgeTests : XCTestCase
@end

@implementation CrashlyticsBridgeTests

- (void)setUp {
    [super setUp];
    // Note: Firebase not initialized in unit tests (would require app context)
}

- (void)testClassExists {
    // Verify class can be loaded
    Class bridgeClass = NSClassFromString(@"CrashlyticsBridge");
    XCTAssertNotNil(bridgeClass, @"CrashlyticsBridge class should exist");
}

- (void)testRecordExceptionMethod {
    // Verify method selector exists
    SEL selector = @selector(recordExceptionWithMessage:tag:stackTrace:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"recordExceptionWithMessage:tag:stackTrace: should exist");
}

- (void)testLogMethod {
    SEL selector = @selector(logWithMessage:tag:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"logWithMessage:tag: should exist");
}

- (void)testSetCustomKeyMethod {
    SEL selector = @selector(setCustomKeyWithKey:value:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setCustomKeyWithKey:value: should exist");
}

- (void)testSetUserIdMethod {
    SEL selector = @selector(setUserId:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setUserId: should exist");
}

- (void)testIsCrashlyticsCollectionEnabledMethod {
    SEL selector = @selector(isCrashlyticsCollectionEnabled);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"isCrashlyticsCollectionEnabled should exist");
}

- (void)testSetCrashlyticsCollectionEnabledMethod {
    SEL selector = @selector(setCrashlyticsCollectionEnabled:);
    XCTAssertTrue([CrashlyticsBridge respondsToSelector:selector],
                  @"setCrashlyticsCollectionEnabled: should exist");
}

// Integration tests (require Firebase initialization)
// These would need to be run in an app context or with mocked Firebase

@end
```

#### 1.4. Update CrashlyticsBridge.m Location

Move files from `iosApp/worldwidewaves/Utils/` to `iosApp/CrashlyticsBridge/CrashlyticsBridge/`:
- CrashlyticsBridge.h
- CrashlyticsBridge.m

Update imports in CrashlyticsBridge.m (no changes needed - already correct).

---

### Phase 2: Gradle Automation

#### 2.1. Add Gradle Task to Build Universal Static Library

**Location**: `shared/build.gradle.kts`

**After line 69** (after cinterop configuration), add:

```kotlin
/*
 * Gradle tasks for building CrashlyticsBridge static library
 *
 * This library must be built before Kotlin/Native framework linking because
 * the framework needs to resolve _OBJC_CLASS_$_CrashlyticsBridge at link time.
 *
 * Architecture:
 *   1. Build libCrashlyticsBridge.a for all iOS architectures
 *   2. Create universal binary with lipo
 *   3. Kotlin/Native links Shared.framework against .a
 *   4. Symbol resolved at framework link time (not app runtime)
 */

val crashlyticsBridgeProjectDir = File("$projectDir/../iosApp/CrashlyticsBridge")
val crashlyticsBridgeOutputDir = File("$projectDir/../iosApp/build/CrashlyticsBridge")

val buildCrashlyticsBridgeIosArm64 = tasks.register("buildCrashlyticsBridgeIosArm64") {
    group = "build"
    description = "Build CrashlyticsBridge static library for iOS devices (arm64)"

    inputs.files(
        fileTree("$crashlyticsBridgeProjectDir/CrashlyticsBridge") {
            include("**/*.h", "**/*.m")
        }
    )
    outputs.file("$crashlyticsBridgeOutputDir/Release-iphoneos/libCrashlyticsBridge.a")

    doLast {
        crashlyticsBridgeOutputDir.mkdirs()

        exec {
            workingDir = crashlyticsBridgeProjectDir
            commandLine(
                "xcodebuild",
                "-project", "CrashlyticsBridge.xcodeproj",
                "-target", "CrashlyticsBridge",
                "-configuration", "Release",
                "-sdk", "iphoneos",
                "-arch", "arm64",
                "BUILD_DIR=${crashlyticsBridgeOutputDir}",
                "OBJROOT=${crashlyticsBridgeOutputDir}/Intermediates",
                "SYMROOT=${crashlyticsBridgeOutputDir}",
                "ONLY_ACTIVE_ARCH=NO",
                "build"
            )
        }

        logger.lifecycle("✅ Built libCrashlyticsBridge.a for iOS arm64")
    }
}

val buildCrashlyticsBridgeSimulatorArm64 = tasks.register("buildCrashlyticsBridgeSimulatorArm64") {
    group = "build"
    description = "Build CrashlyticsBridge static library for iOS Simulator (arm64)"

    inputs.files(
        fileTree("$crashlyticsBridgeProjectDir/CrashlyticsBridge") {
            include("**/*.h", "**/*.m")
        }
    )
    outputs.file("$crashlyticsBridgeOutputDir/Release-iphonesimulator/libCrashlyticsBridge.a")

    doLast {
        crashlyticsBridgeOutputDir.mkdirs()

        exec {
            workingDir = crashlyticsBridgeProjectDir
            commandLine(
                "xcodebuild",
                "-project", "CrashlyticsBridge.xcodeproj",
                "-target", "CrashlyticsBridge",
                "-configuration", "Release",
                "-sdk", "iphonesimulator",
                "-arch", "arm64",
                "BUILD_DIR=${crashlyticsBridgeOutputDir}",
                "OBJROOT=${crashlyticsBridgeOutputDir}/Intermediates",
                "SYMROOT=${crashlyticsBridgeOutputDir}",
                "ONLY_ACTIVE_ARCH=NO",
                "build"
            )
        }

        logger.lifecycle("✅ Built libCrashlyticsBridge.a for iOS Simulator arm64")
    }
}

val buildCrashlyticsBridgeSimulatorX86 = tasks.register("buildCrashlyticsBridgeSimulatorX86") {
    group = "build"
    description = "Build CrashlyticsBridge static library for iOS Simulator (x86_64)"

    inputs.files(
        fileTree("$crashlyticsBridgeProjectDir/CrashlyticsBridge") {
            include("**/*.h", "**/*.m")
        }
    )
    outputs.file("$crashlyticsBridgeOutputDir/Release-iphonesimulator-x86/libCrashlyticsBridge.a")

    doLast {
        crashlyticsBridgeOutputDir.mkdirs()

        exec {
            workingDir = crashlyticsBridgeProjectDir
            commandLine(
                "xcodebuild",
                "-project", "CrashlyticsBridge.xcodeproj",
                "-target", "CrashlyticsBridge",
                "-configuration", "Release",
                "-sdk", "iphonesimulator",
                "-arch", "x86_64",
                "BUILD_DIR=${crashlyticsBridgeOutputDir}",
                "OBJROOT=${crashlyticsBridgeOutputDir}/Intermediates",
                "SYMROOT=${crashlyticsBridgeOutputDir}",
                "ONLY_ACTIVE_ARCH=NO",
                "build"
            )
        }

        logger.lifecycle("✅ Built libCrashlyticsBridge.a for iOS Simulator x86_64")
    }
}

val createUniversalCrashlyticsBridge = tasks.register("createUniversalCrashlyticsBridge") {
    group = "build"
    description = "Create universal libCrashlyticsBridge.a for all architectures"

    dependsOn(
        buildCrashlyticsBridgeIosArm64,
        buildCrashlyticsBridgeSimulatorArm64,
        buildCrashlyticsBridgeSimulatorX86
    )

    val outputFile = File("$crashlyticsBridgeOutputDir/libCrashlyticsBridge-universal.a")
    outputs.file(outputFile)

    doLast {
        val iphoneosLib = "$crashlyticsBridgeOutputDir/Release-iphoneos/libCrashlyticsBridge.a"
        val simulatorArm64Lib = "$crashlyticsBridgeOutputDir/Release-iphonesimulator/libCrashlyticsBridge.a"
        val simulatorX86Lib = "$crashlyticsBridgeOutputDir/Release-iphonesimulator-x86/libCrashlyticsBridge.a"

        // Create universal binary with lipo
        exec {
            commandLine(
                "lipo",
                "-create",
                iphoneosLib,
                simulatorArm64Lib,
                simulatorX86Lib,
                "-output", outputFile.absolutePath
            )
        }

        logger.lifecycle("✅ Created universal libCrashlyticsBridge.a")
        logger.lifecycle("   Output: ${outputFile.absolutePath}")

        // Verify architectures
        exec {
            commandLine("lipo", "-info", outputFile.absolutePath)
            standardOutput = System.out
        }
    }
}

// Make Kotlin/Native linking depend on static library build
listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64(),
).forEach { iosTarget ->
    // Update existing framework configuration (around line 53-59)
    iosTarget.binaries.framework {
        baseName = "Shared"
        isStatic = false

        // Link against CrashlyticsBridge static library
        // This resolves _OBJC_CLASS_$_CrashlyticsBridge at framework link time
        linkerOpts("-L$crashlyticsBridgeOutputDir")
        linkerOpts("$crashlyticsBridgeOutputDir/libCrashlyticsBridge-universal.a")

        // REMOVE the weak linking flag (no longer needed):
        // linkerOpts("-Wl,-U,_OBJC_CLASS_\$_CrashlyticsBridge")  // DELETE THIS LINE

        linkerOpts("-ObjC")  // Keep this - loads ObjC categories
    }

    // Ensure static library is built before framework linking
    tasks.matching { it.name.contains("link") && it.name.contains("Framework") && it.name.contains(iosTarget.name) }.configureEach {
        dependsOn(createUniversalCrashlyticsBridge)
    }

    // cinterop configuration stays the same (lines 62-68)
    // It still points to the .h file, just the .m is now in static library
}
```

#### 2.2. Add Clean Task

```kotlin
val cleanCrashlyticsBridge = tasks.register("cleanCrashlyticsBridge") {
    group = "build"
    description = "Clean CrashlyticsBridge static library build artifacts"

    doLast {
        delete(crashlyticsBridgeOutputDir)
        exec {
            workingDir = crashlyticsBridgeProjectDir
            commandLine("xcodebuild", "-project", "CrashlyticsBridge.xcodeproj", "-target", "CrashlyticsBridge", "clean")
            isIgnoreExitValue = true // Don't fail if project doesn't exist yet
        }
        logger.lifecycle("✅ Cleaned CrashlyticsBridge artifacts")
    }
}

tasks.named("clean") {
    dependsOn(cleanCrashlyticsBridge)
}
```

---

### Phase 3: Update cinterop Configuration

**Location**: `shared/src/nativeInterop/cinterop/CrashlyticsBridge.def`

**Update path** to point to new location:

```
language = Objective-C
headers = CrashlyticsBridge.h
headerFilter = CrashlyticsBridge.h
compilerOpts = -I${PROJECT_DIR}/../iosApp/CrashlyticsBridge/CrashlyticsBridge
```

**Update includeDirs** in build.gradle.kts:

```kotlin
cinterops.create("CrashlyticsBridge") {
    definitionFile.set(project.file("src/nativeInterop/cinterop/CrashlyticsBridge.def"))
    packageName = "com.worldwidewaves.crashlytics"
    includeDirs("$projectDir/../iosApp/CrashlyticsBridge/CrashlyticsBridge")  // Updated path
}
```

---

### Phase 4: Testing

#### 4.1. XCTest Unit Tests

**Run via Gradle**:

```kotlin
val runCrashlyticsBridgeTests = tasks.register("runCrashlyticsBridgeTests") {
    group = "verification"
    description = "Run CrashlyticsBridge XCTest unit tests"

    dependsOn(createUniversalCrashlyticsBridge)

    doLast {
        exec {
            workingDir = crashlyticsBridgeProjectDir
            commandLine(
                "xcodebuild",
                "test",
                "-project", "CrashlyticsBridge.xcodeproj",
                "-scheme", "CrashlyticsBridge",
                "-destination", "platform=iOS Simulator,name=iPhone 15 Pro"
            )
        }
        logger.lifecycle("✅ CrashlyticsBridge tests passed")
    }
}

// Add to test task dependencies
tasks.named("check") {
    dependsOn(runCrashlyticsBridgeTests)
}
```

#### 4.2. Integration with Existing Tests

**No changes needed** - existing Kotlin tests in `shared/src/commonTest/` already cover CrashlyticsLogger.

#### 4.3. Verify Symbol Resolution

**Add verification task**:

```kotlin
val verifyCrashlyticsBridgeSymbol = tasks.register("verifyCrashlyticsBridgeSymbol") {
    group = "verification"
    description = "Verify _OBJC_CLASS_\$_CrashlyticsBridge symbol exists in Shared.framework"

    dependsOn(tasks.matching { it.name.contains("linkDebugFrameworkIosArm64") })

    doLast {
        val frameworkPath = "$buildDir/bin/iosArm64/debugFramework/Shared.framework/Shared"

        val result = exec {
            commandLine("nm", frameworkPath)
            standardOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }

        val output = (result.standardOutput as ByteArrayOutputStream).toString()

        if (output.contains("_OBJC_CLASS_\$_CrashlyticsBridge")) {
            logger.lifecycle("✅ Symbol _OBJC_CLASS_\$_CrashlyticsBridge found in Shared.framework")
        } else {
            throw GradleException("❌ Symbol _OBJC_CLASS_\$_CrashlyticsBridge NOT found in Shared.framework!")
        }
    }
}
```

---

### Phase 5: Documentation

#### 5.1. Create Architecture Documentation

**File**: `docs/ios/crashlytics-static-library-architecture.md`

```markdown
# CrashlyticsBridge Static Library Architecture

## Why a Static Library?

iOS app executables (MH_EXECUTE) cannot export Objective-C class symbols for dynamic frameworks (MH_DYLIB) to import. This is a fundamental limitation of the Mach-O binary format on iOS.

### The Problem

Our Kotlin/Native Shared.framework needs to call Firebase Crashlytics (iOS-only API). The standard approach is:

```
Kotlin → cinterop → Objective-C bridge → Firebase
```

However, this creates a dependency problem:

1. Shared.framework uses weak linking: `-Wl,-U,_OBJC_CLASS_$_CrashlyticsBridge`
   - "Expect this symbol to exist at runtime, provided by the app"

2. iOS app compiles CrashlyticsBridge.m
   - Symbol exists: `_OBJC_CLASS_$_CrashlyticsBridge`
   - But it's PRIVATE (lowercase 's' in nm) - not exported

3. Runtime (dyld):
   - Shared.framework tries to resolve symbol
   - Can't find it (apps don't export symbols)
   - **CRASH: symbol not found**

### The Solution

**Static Library Approach**:

```
libCrashlyticsBridge.a
  ├─ CrashlyticsBridge.m
  ├─ CrashlyticsBridge.h
  └─ Links: FirebaseCrashlytics (SPM)
       ↓ built by Xcode
       ↓ symbol: _OBJC_CLASS_$_CrashlyticsBridge (public in .a)
       ↓
Shared.framework (Kotlin/Native)
  ├─ Links: libCrashlyticsBridge.a (at framework link time)
  ├─ Resolves: _OBJC_CLASS_$_CrashlyticsBridge ✅
  └─ No runtime lookup needed
       ↓ embedded in iOS app
       ↓
iOS App
  ├─ Links: Shared.framework (contains symbol)
  ├─ Links: libCrashlyticsBridge.a (same binary for consistency)
  └─ Links: Firebase frameworks (SPM)
```

**Key Insight**: The symbol is resolved **at framework link time**, not app runtime. Static library provides symbols to Kotlin/Native linker during framework build.

## Build Process

### Automated via Gradle

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

**What happens**:
1. `buildCrashlyticsBridgeIosArm64` - Builds .a for device
2. `buildCrashlyticsBridgeSimulatorArm64` - Builds .a for simulator
3. `buildCrashlyticsBridgeSimulatorX86` - Builds .a for x86 simulator
4. `createUniversalCrashlyticsBridge` - Combines into universal binary
5. `linkReleaseFrameworkIosArm64` - Links Shared.framework against .a
6. Symbol `_OBJC_CLASS_$_CrashlyticsBridge` now IN framework ✅

### Manual Build (if needed)

```bash
# Build static library for all architectures
cd iosApp/CrashlyticsBridge
xcodebuild -project CrashlyticsBridge.xcodeproj -target CrashlyticsBridge \
  -configuration Release -sdk iphoneos build

xcodebuild -project CrashlyticsBridge.xcodeproj -target CrashlyticsBridge \
  -configuration Release -sdk iphonesimulator build

# Create universal binary
lipo -create \
  build/Release-iphoneos/libCrashlyticsBridge.a \
  build/Release-iphonesimulator/libCrashlyticsBridge.a \
  -output build/libCrashlyticsBridge-universal.a
```

## Testing

### Unit Tests (XCTest)

```bash
# Run library tests
./gradlew runCrashlyticsBridgeTests

# Or manually:
cd iosApp/CrashlyticsBridge
xcodebuild test -project CrashlyticsBridge.xcodeproj \
  -scheme CrashlyticsBridge \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

### Integration Tests

Existing Kotlin tests in `shared/src/commonTest/` cover CrashlyticsLogger end-to-end.

### Verify Symbol

```bash
# Check symbol exists in framework
nm shared/build/bin/iosArm64/debugFramework/Shared.framework/Shared | \
  grep "_OBJC_CLASS_\$_CrashlyticsBridge"

# Expected output:
# [some address] S _OBJC_CLASS_$_CrashlyticsBridge
#                ↑ capital 'S' = exported symbol
```

## Troubleshooting

### Symbol Still Missing After Build

**Check**:
1. Static library built: `ls iosApp/build/CrashlyticsBridge/libCrashlyticsBridge-universal.a`
2. Symbol in .a: `nm iosApp/build/CrashlyticsBridge/libCrashlyticsBridge-universal.a | grep CrashlyticsBridge`
3. Framework linked .a: Check Gradle build logs for "libCrashlyticsBridge.a" in link command
4. Symbol in framework: `nm shared/build/.../Shared.framework/Shared | grep CrashlyticsBridge`

### Build Fails with Firebase Not Found

**Cause**: Firebase SPM dependency not resolved in static library project.

**Fix**:
1. Open `CrashlyticsBridge.xcodeproj` in Xcode
2. Wait for package resolution
3. File → Packages → Resolve Package Versions
4. Rebuild

### Tests Fail

**XCTest limitation**: Firebase requires app initialization context. Unit tests verify:
- Methods exist
- Selectors are correct
- Class can be loaded

Full integration testing happens via iOS app + existing Kotlin tests.

## Maintenance

### When to Update

**Rebuild static library when**:
- CrashlyticsBridge.m/.h changes
- Firebase Crashlytics version updates
- New iOS architectures added

**Gradle handles this automatically** via task dependencies and input/output tracking.

### Version Compatibility

- iOS Deployment Target: 16.0+ (matches main app)
- Firebase version: Managed by SPM (stays in sync with main app)
- Xcode version: 15.3+ required for project format

## References

- Mach-O symbol visibility: https://developer.apple.com/library/archive/documentation/DeveloperTools/Conceptual/MachOTopics/
- Kotlin/Native cinterop: https://kotlinlang.org/docs/native-c-interop.html
- Static vs Dynamic linking: https://developer.apple.com/library/archive/documentation/DeveloperTools/Conceptual/DynamicLibraries/

## Original Issue

- Builds 35-38: All crashed with "symbol not found _OBJC_CLASS_$_CrashlyticsBridge"
- Root cause: Apps can't export symbols to frameworks
- Solution: Static library provides symbol at framework link time
- Commit: [TBD after implementation]
```

#### 5.2. Add Inline Comments to Gradle

Already included in Phase 2 code above - extensive comments explaining WHY each step is needed.

#### 5.3. Update CLAUDE.md

**Add section** after line 229 (after Notifications System):

```markdown
### Crashlytics Integration (iOS)

**Status**: ✅ Production-Ready (Static Library Architecture)

Firebase Crashlytics integration on iOS uses a **static library architecture** due to Mach-O symbol export limitations.

**Architecture**:
- `libCrashlyticsBridge.a` - Static library with Objective-C→Firebase bridge
- Linked to Shared.framework at framework build time
- Symbol `_OBJC_CLASS_$_CrashlyticsBridge` resolved during linking

**Why Static Library?**:
iOS app executables cannot export Objective-C symbols for frameworks to import. Using a static library ensures the symbol is available when Kotlin/Native links Shared.framework.

**Key Files**:
- `iosApp/CrashlyticsBridge/` - Static library Xcode project
- `shared/build.gradle.kts` - Gradle automation for multi-arch .a build
- `shared/src/iosMain/.../CrashlyticsLogger.ios.kt` - Kotlin implementation
- `docs/ios/crashlytics-static-library-architecture.md` - Architecture guide

**Build**: Fully automated via Gradle (runs before framework linking)

**Tests**:
- XCTest: `./gradlew runCrashlyticsBridgeTests`
- Kotlin: `./gradlew :shared:testDebugUnitTest` (existing)

**See**: [Crashlytics Static Library Architecture](docs/ios/crashlytics-static-library-architecture.md)
```

---

### Phase 6: Complete Implementation Checklist

#### Pre-Implementation

- [ ] Review this plan completely
- [ ] Understand why static library is needed
- [ ] Have Xcode 16.4+ installed
- [ ] Backup current state: `git stash` or `git branch crashlytics-static-lib`

#### Implementation Steps

1. **Generate Xcode Project**:
   ```bash
   python3 scripts/ios/generate-crashlytics-library-project.py
   ```

2. **Create Test File**:
   - Create `iosApp/CrashlyticsBridge/CrashlyticsBridgeTests/CrashlyticsBridgeTests.m`
   - Copy content from Phase 1.3 above

3. **Move Source Files**:
   ```bash
   mkdir -p iosApp/CrashlyticsBridge/CrashlyticsBridge
   mv iosApp/worldwidewaves/Utils/CrashlyticsBridge.h iosApp/CrashlyticsBridge/CrashlyticsBridge/
   mv iosApp/worldwidewaves/Utils/CrashlyticsBridge.m iosApp/CrashlyticsBridge/CrashlyticsBridge/
   ```

4. **Update Gradle Configuration**:
   - Add all code from Phase 2 to `shared/build.gradle.kts`
   - Update cinterop includeDirs (Phase 3)

5. **Update cinterop Definition**:
   - Edit `shared/src/nativeInterop/cinterop/CrashlyticsBridge.def`
   - Update compilerOpts path

6. **Create Documentation**:
   - Create `docs/ios/crashlytics-static-library-architecture.md` (Phase 5.1)
   - Update `CLAUDE.md` (Phase 5.3)

7. **Test Build**:
   ```bash
   ./gradlew clean
   ./gradlew createUniversalCrashlyticsBridge
   ./gradlew :shared:linkDebugFrameworkIosArm64
   ```

8. **Verify Symbol**:
   ```bash
   nm shared/build/bin/iosArm64/debugFramework/Shared.framework/Shared | \
     grep "_OBJC_CLASS_\$_CrashlyticsBridge"
   # Should show: [address] S _OBJC_CLASS_$_CrashlyticsBridge
   #              ↑ capital S = exported
   ```

9. **Run Tests**:
   ```bash
   ./gradlew runCrashlyticsBridgeTests
   ./gradlew :shared:testDebugUnitTest
   ```

10. **Test iOS App Build**:
    ```bash
    ./gradlew :shared:embedAndSignAppleFrameworkForXcode
    cd iosApp
    xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves \
      -sdk iphonesimulator build
    ```

11. **Test Archive Build**:
    ```bash
    xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves \
      -sdk iphoneos -configuration Release archive \
      -archivePath /tmp/test.xcarchive
    ```

12. **Verify Archive Symbol**:
    ```bash
    nm /tmp/test.xcarchive/Products/Applications/worldwidewaves.app/Frameworks/Shared.framework/Shared | \
      grep "_OBJC_CLASS_\$_CrashlyticsBridge"
    ```

13. **Clean Up Reverted Changes**:
    - Remove `iosApp/worldwidewaves/exports.txt`
    - Revert `OTHER_LDFLAGS` in project.pbxproj to empty string
    - Remove any test Archives

14. **Commit**:
    ```bash
    git add -A
    git commit -m "feat(ios): implement Crashlytics via static library architecture

    Fixes symbol export issue for Kotlin/Native cinterop.

    See docs/ios/crashlytics-static-library-architecture.md for details."
    ```

---

## Expected Outcomes

### After Implementation

✅ **Build Process**:
- `./gradlew :shared:embedAndSignAppleFrameworkForXcode` - Builds everything automatically
- Static library built before framework linking
- Symbol resolved at compile time, not runtime
- No manual steps required

✅ **Testing**:
- `./gradlew runCrashlyticsBridgeTests` - XCTest unit tests pass
- `./gradlew :shared:testDebugUnitTest` - All 902+ Kotlin tests pass
- iOS app builds and launches successfully
- Crashlytics calls from Kotlin work on iOS

✅ **TestFlight**:
- Build 39+ will launch without crash
- Symbol `_OBJC_CLASS_$_CrashlyticsBridge` exists in Shared.framework
- Runtime symbol resolution succeeds
- Crashlytics integration fully functional

✅ **Maintenance**:
- Gradle handles all builds automatically
- Tests run via standard test commands
- Documentation explains the architecture
- Future developers understand why this pattern exists

---

## Prompt for Later Implementation

**When ready to implement, use this prompt**:

```
Implement the CrashlyticsBridge static library solution for iOS Crashlytics integration.

Read and follow the complete plan in:
docs/ios/CRASHLYTICS_STATIC_LIBRARY_IMPLEMENTATION_PLAN.md

Key requirements:
1. Generate complete Xcode project for static library (use Python script in plan)
2. Add full Gradle automation (build, test, clean tasks)
3. Create comprehensive XCTest unit tests
4. Update all documentation (architecture guide + CLAUDE.md)
5. Verify symbol resolution at every step
6. Test complete build pipeline (Debug + Release + Archive)
7. Ensure all 902+ tests still pass

The plan has ALL implementation details, code examples, and verification steps.

Do NOT take shortcuts - implement the complete solution with full automation and testing.
```

---

## Notes for Future Reference

### Why Other Approaches Failed

1. **Swift @objc** - Linker strips ObjC metadata from Swift classes
2. **Export flags on app** - Apps don't export, only import
3. **DEAD_CODE_STRIPPING = NO** - Symbol exists but still private
4. **-rdynamic** - Doesn't work for ObjC class symbols on iOS
5. **Direct platform API calls** - Firebase not available at Kotlin/Native compile time

### Why Static Library Works

- Kotlin/Native linker can link against `.a` files
- Symbol resolved during framework link (not runtime)
- Firebase available during static library build (SPM)
- Standard solution for cinterop with external dependencies
- Matches patterns used in other KMM projects

### Estimated Time Breakdown

- Xcode project generation script: 30 min
- Gradle integration: 30 min
- Tests creation: 20 min
- Documentation: 30 min
- Testing & verification: 30 min
- Debugging & fixes: 30 min

**Total**: 2.5-3 hours

---

## Status

- [x] Problem identified and analyzed
- [x] Solution architecture designed
- [x] Implementation plan documented
- [ ] **TODO: Execute implementation** (use plan as prompt)
- [ ] TODO: Verify in TestFlight build 39

---

**Last Updated**: 2025-11-10 01:30 AM
**Next Action**: Implement following this plan when ready
