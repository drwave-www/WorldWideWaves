# Changelog

All notable changes to WorldWideWaves will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Comprehensive documentation system (A+ grade, 115+ files)
- State management patterns documentation with architecture diagrams
- Dependency Injection patterns guide
- Reactive Programming patterns documentation
- Linux setup guide with KVM acceleration
- Automated setup verification script
- Correlation ID support for distributed tracing
- Structured logging support with context propagation

### Changed
- Documentation structure reorganized and standardized to kebab-case
- Logging system improvements across iOS and Android platforms
- iOS safety verification script enhanced to exclude documentation files

### Fixed
- MapLibre threading issues in Android instrumented tests
- All adapter calls wrapped in runOnUiThread for MapLibre compliance

---

## [0.9.0] - 2025-10-27

### Added
- iOS MapLibre integration with 95% feature parity with Android
- Comprehensive test suite: 902 unit tests with 100% pass rate
- WCAG 2.1 Level AA accessibility compliance
  - TalkBack and VoiceOver screen reader support
  - iOS Dynamic Type support (0.8x-3.0x text scaling)
  - Touch target minimums (48dp Android / 44pt iOS)
  - 4.5:1 minimum color contrast ratio
  - Live region announcements for dynamic content
  - 27+ automated accessibility tests
- iOS map features
  - User position marker with red pulse animation
  - Wave progression visualization with polygon rendering
  - Gesture controls (pan, zoom) with constraint enforcement
  - Camera positioning and bounds validation
  - Adaptive camera strategy for wave tracking
- Map testing infrastructure
  - 20+ MapBoundsEnforcer unit tests
  - Screen-specific integration tests for all 3 map types
  - Comprehensive bounds constraint test coverage
- Internationalization (i18n)
  - 33 language support with MokoResources
  - Auto-generated translations for accessibility strings
  - RTL (Right-to-Left) layout support
  - Localized date/time formatting
  - Bidirectional text wrapping
- Thread safety improvements
  - Mutex-based synchronization in MapDownloadGate
  - Proper coroutine scope management
  - Memory leak prevention with job cancellation

### Changed
- Position system refactored to unified observer architecture
  - Single efficient observation stream (replaced 3 separate streams)
  - PositionManager as single source of truth (SIMULATION > GPS priority)
  - Conservative debouncing and deduplication
  - 67% reduction in redundant getPolygons() calls
- Documentation reorganization
  - 115+ files organized by domain
  - Quality score improvement: 74 → 95
  - Comprehensive iOS development guides
  - Architecture diagrams with Mermaid
- Code quality improvements
  - Companion objects standardized to top of class
  - File organization standardized
  - Class naming conventions enforced
  - Dead code removed across domain layer
  - Logging consistency improved

### Fixed
- iOS Critical Fixes (11 violations eliminated)
  - All iOS deadlock patterns resolved
  - Force unwrap elimination (8 unsafe !! operators removed)
  - Dispatchers.Main initialization fixed in property declarations
  - Proper exception handling with @Throws annotations
  - DI access in init{} blocks eliminated
- iOS Map Fixes
  - Full map gesture controls (pan, zoom, tap)
  - Gesture property names corrected (isZoomEnabled vs allowsZooming)
  - Camera positioning and zoom calculation accuracy
  - Viewport bounds checking with epsilon tolerance
  - Initial zoom timing issues
  - Map recreation on availability change prevented
  - Hard-coded Paris camera position removed (P0 CRITICAL)
- Android Map Fixes
  - WINDOW mode camera positioning
  - Zero padding in WINDOW mode for gesture reliance
  - Constraint recalculation after programmatic zoom
  - Min zoom calculation from constraining dimension
  - Button animation gesture detection
- Memory and Performance
  - Proper job cancellation in WWWEvents
  - Polygon queue optimization (store only most recent set)
  - Map download gate thread safety
  - LRU cache size limit enforcement in GeoJsonDataProvider
- Code Quality
  - License header consistency (duplicate headers, typos)
  - MapLibre threading compliance
  - Swift code safety improvements
  - Detekt and SwiftLint warnings resolved

### Removed
- Unused parameters and properties (dead code cleanup)
- Shadowed width/height extensions from MapTestFixtures
- Settings tab (UI simplification)
- Unused ZOOM_SAFETY_MARGIN constant

---

## [0.8.0] - 2025-09-01

### Added
- Polygon splitting algorithm for antimeridian crossing
  - Support for multi-piece output per side
  - Composed-longitude midpoint insertion
  - Degenerate polygon filtering (<3 points)
- Sound choreography test layer (hidden in release builds)
- Map download flow with progress indicators
- Map uninstall functionality
- Downloaded maps tab on events screen
- Event bounding box area handling
- 40+ city offline map modules with self-hosted tiles

### Changed
- Wave speed adjusted to 6 m/s for diagonal street management
- Simplified splitByLongitude to single-pass splitter
- Event timing: Dubai events start at 4pm
- Refactored EventMap for improved maintainability
- Optimized map download UI consistency across activities
- Map zoom levels updated for all cities

### Fixed
- Multi-polygon cut issues in antimeridian handling
- Events orchestration and interpretation
- Simulation datetime handling
- Recursive loop in event processing
- Event filtering thread safety
- Tab navigation functionality
- Map height calculations
- Zoom calculation for southern hemisphere cities

---

## [0.7.0] - 2025-07-01

### Added
- Internationalization system with MokoResources
  - Base strings with descriptions for translation context
  - GPT-5 powered translation automation script
  - Choreography text localization
  - Event metadata localization (location, description, country, community)
- City cover image generation automation
  - Script-based automation with map rendering
  - WEBP conversion for resource optimization
- Build optimization
  - Proguard rules for shared module and j2objc
  - R8 minification configuration
  - MockK moved to testImplementation

### Changed
- Event guidelines and FAQ content updated
- WWW concept information content revised
- PNG/JPG resources converted to WEBP format
- Wave color scheme updated
- DrWave signature simplified for clarity
- Estimated time to hit display improved

### Fixed
- CI/CD improvements
  - iOS build targeting iPhone 16 with iOS 18.2 simulator
  - Xcode 16.2 compatibility on macos-14 runners
  - License generation for Gradle and Node.js
- R8 minification issues
  - Guava/WorkManager R8 references resolved with j2objc-annotations
  - Shared library minification disabled for release
- GeoJSON parsing
  - FeatureCollection support added
  - Multi-polygon geometry merging
  - Empty GeoJSON file fallback handling
- Location simulation
  - Immediate emission of simulated location
  - Location provider priming with last known position
- Map rendering
  - MBTiles tile decompression with Gzip
  - URL-encoded character decoding in glyph paths
  - Graceful handling of missing tiles
  - Zoom padding removed for GeoJSON boundary maximization

---

## [0.6.0] - 2025-04-01

### Added
- macOS support for development environment
- City default map generation system
- Flag assets for country/community representation
- Accurate zoom calculation based on image dimensions
- Comprehensive tests for GeoJSON fallback handling

### Changed
- Map rendering engine improvements
  - MapLibre GL Native API integration
  - Proper style loading with native support
  - MBTiles parsing using MapLibre native support
- Zoom and center calculation from GeoJSON boundaries
- Presentation improvements for country and community names

### Fixed
- GeoJSON merging for MultiPolygon geometries
- Zoom calculation for southern hemisphere cities
- Map image generation script reliability
- MBTiles URL parsing regex
- Temporary test file cleanup

---

## [0.5.0] - 2025-02-01

### Added
- Full map screen with download button overlay for non-downloaded maps
- Map download progress indicators
- Auto-load functionality for maps
- Hit counter in wave screen with warming time shift
- Bounding box area management in WWWEventArea

### Changed
- Map download flow optimized for consistent behavior
- Event image used as background for better visual consistency
- Map download UI consistency across all activities
- Zoom levels and file compression optimization

### Fixed
- Download button functionality
- Unresolved reference to mapError in AndroidEventMap
- Map targeting functionality (user/wave)
- Zoom auto-adjustment on wave screen
- Full map display on wave completion
- Position tracking with first location set in all cases

---

## [0.4.0] - 2025-01-01

### Added
- Initial 40 prioritized cities with offline maps
  - Karachi added to city list
  - Comprehensive OsmAdminIds configuration
- Kotlin Flow-based reactive architecture
  - Event updates via Flow instead of listeners
  - Improved efficiency with reactive streams

### Changed
- Event map complete refactoring for better architecture
- Code organization with cleanups and refactorings
- openmaptiles-tools integrated as submodule

### Fixed
- Geofabrik zones configuration
- OsmAdminId handling
- Templating issues in map configuration
- JSON syntax errors
- jq array handling in scripts
- Memory optimizations in map rendering
- Layer management: delete existing layers before drawing new ones

---

## [0.3.0] - 2024-12-01

### Added
- Instagram video generation and posting (WorldWideWaves-social)
  - Video generation for 4:5 and 9:16 formats
  - Background sound synchronization
  - Audio generation for supported languages
  - Multi-page layout support

### Changed
- Gunicorn timeout increased to 600s for video processing
- nginx timeout configuration for long-running requests
- Video player rendering improvements

### Fixed
- docker-compose pwd issues
- CSS adjustments for mobile video display
- Horizontal layout positioning in video
- Title positioning and spacing
- Character spacing for Japanese and Chinese
- Vertical language support (secret feature)

---

## [0.2.0] - 2024-11-01

### Added
- Event favorites mechanism with DataStore persistence
- Favorites filtering tabs in event list
- Initial test suite setup
- Date formatter moved to common code for sharing
- Asset organization system

### Changed
- Resource management improvements
- Events JSON loading method refactored
- Flags size increased for better visibility

### Fixed
- Error handling in date formatter
- Resource loading with proper not-found image fallback
- Common resource access from iOS

---

## [0.1.0] - 2024-10-01

### Added
- Initial project setup with Kotlin Multiplatform Mobile
- Jetpack Compose UI for Android
- iOS project structure
- Splash screen
- Event configuration reading from JSON
- Wave screen initial implementation
- First theme and color scheme
- GitHub Actions CI/CD workflow
- Code license headers

### Infrastructure
- Gradle build system
- KMM project structure
- Common resources management
- iOS resource bridge

---

## Project Milestones

### Technology Evolution
- **Oct 2024**: Initial KMM project with Android Compose
- **Nov 2024**: iOS integration and data persistence
- **Dec 2024**: Social media integration (Instagram)
- **Jan 2025**: Offline maps with 40 cities
- **Feb 2025**: Map download management
- **Apr 2025**: MapLibre integration
- **Jul 2025**: Internationalization (33 languages)
- **Sep 2025**: Position system refactor
- **Oct 2025**: iOS MapLibre parity + WCAG 2.1 Level AA accessibility

### Test Coverage Milestones
- **Nov 2024**: Initial test infrastructure
- **Jan 2025**: Flow-based testing patterns
- **Sep 2025**: 535+ unit tests
- **Oct 2025**: 902+ unit tests with 100% pass rate

### Documentation Milestones
- **Oct 2025**: Documentation quality score 74 → 95 (A+ grade)
- **Oct 2025**: 115+ organized documentation files
- **Oct 2025**: Comprehensive iOS development guides
- **Oct 2025**: Architecture diagrams with Mermaid

---

**Note**: This changelog is maintained following the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format. For detailed commit history, see `git log`.

**Versioning**: This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html):
- **MAJOR** version for incompatible API changes
- **MINOR** version for backwards-compatible functionality additions
- **PATCH** version for backwards-compatible bug fixes
