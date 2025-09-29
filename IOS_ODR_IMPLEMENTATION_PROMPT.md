# iOS ODR Maps Implementation Prompt

## 🎯 Task: Complete iOS On-Demand Resources Maps Implementation

### **Objective:**
Implement comprehensive iOS ODR (On-Demand Resources) system to provide equivalent functionality to Android's Dynamic Feature Modules for map delivery.

### **Context from CLAUDE.md:**
- Follow iOS-specific configuration: Bundle ID `com.worldwidewaves`, Project Path `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp/`, Xcode Project `worldwidewaves.xcodeproj`
- Use Pure SwiftUI + Kotlin Business Logic approach (NOT Compose UI on iOS)
- Apply iOS deadlock prevention rules - no `object : KoinComponent` in @Composable functions
- Always annotate Kotlin methods called from Swift with `@Throws(Throwable::class)`
- Never modify gradle.build.kt files without explicit approval
- Follow security patterns - no credential exposure, proper input validation
- Maintain architectural patterns - use Koin for DI, Clean Architecture separation
- Write comprehensive tests for all functionality

### **Implementation Requirements:**

#### **Phase 1: Foundation Verification**
1. **Test Current ODR Implementation:**
   - Run `IOSODRIntegrationTest` to verify existing stack works
   - Test `IOSMapAvailabilityChecker` with real map IDs
   - Verify `IOSPlatformMapManager` handles download lifecycle correctly

2. **Bundle Setup for Test Cities:**
   - Create iOS bundle structure: `iosApp/worldwidewaves/Resources/Maps/`
   - Copy 5 test cities (paris_france, new_york_usa, london_england, berlin_germany, tokyo_japan)
   - Include both `.geojson` and `.mbtiles` files for each city

#### **Phase 2: Script Enhancement**
3. **Enhanced `40-generate-modules.sh`:**
   - ✅ Already modified to handle iOS ODR + two-file support
   - Test the script with real data: `bash scripts/maps/40-generate-modules.sh paris_france`
   - Run `scripts/test-odr-bundle-generation.sh` to validate functionality

4. **Info.plist Configuration:**
   - Handle Xcode "Generate Info.plist = Yes" scenario gracefully
   - Add NSBundleResourceRequestTags entries idempotently
   - Validate plist structure after script execution

#### **Phase 3: Integration Testing**
5. **End-to-End Testing:**
   - Test Wave screen with ODR maps
   - Test FullMap screen with ODR maps
   - Verify simulation button respects ODR availability
   - Test map download progress UI

6. **Error Handling:**
   - Test network failure scenarios
   - Test storage constraint handling
   - Verify graceful fallback mechanisms

#### **Phase 4: Production Readiness**
7. **Bundle Optimization:**
   - Analyze bundle size impact
   - Implement intelligent resource cleanup
   - Configure appropriate ODR categories and priorities

8. **Comprehensive Testing:**
   - Unit tests for all ODR components
   - Integration tests for complete workflow
   - Performance testing for download efficiency

### **Success Criteria:**
- ✅ iOS maps work identically to Android Dynamic Features
- ✅ Download progress and availability states update correctly
- ✅ Both geojson and mbtiles are properly bundled and accessible
- ✅ Info.plist ODR configuration is correct and idempotent
- ✅ No regressions in existing functionality
- ✅ Comprehensive test coverage for all ODR edge cases

### **Quality Gates:**
- All existing unit tests continue to pass (328+ tests)
- New ODR tests achieve 100% success rate
- Code follows iOS deadlock prevention rules
- No security vulnerabilities introduced
- Performance comparable to Android implementation

### **Next Steps:**
Execute implementation systematically following WorldWideWaves development patterns. Test thoroughly at each phase. Document any issues or architectural decisions.