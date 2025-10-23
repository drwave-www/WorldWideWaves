# Android Map Constraints Documentation

**Purpose**: Complete reference for Android MapLibre constraint implementation, created October 2025 to support iOS feature parity.

## Overview

This documentation suite provides comprehensive coverage of Android's map constraint system, serving as a reference for implementing equivalent functionality on iOS.

## Documents

### [Index](ANDROID_MAP_CONSTRAINT_INDEX.md)
**Navigation hub** - Overview and links to all constraint documentation.
- Quick concept summary
- Document relationship map
- Related files index

### [Analysis](ANDROID_MAP_CONSTRAINT_ANALYSIS.md)
**Deep dive** - Detailed architectural analysis.
- 3-layer architecture breakdown
- Min zoom calculation formulas
- BOUNDS vs WINDOW mode comparison
- Padding logic explanation
- Implementation recommendations

### [Code Patterns](ANDROID_CONSTRAINT_CODE_PATTERNS.md)
**Implementation guide** - Working code examples.
- 7 complete code patterns
- Actual implementations with line numbers
- Testing approaches
- Edge case handling

### [Quick Reference](ANDROID_PATTERNS_QUICK_REFERENCE.md)
**Developer cheat sheet** - One-page reference.
- Decision trees
- Formula summaries
- 5 critical implementation rules
- Common pitfalls

### [Source File Reference](ANDROID_SOURCE_FILE_REFERENCE.md)
**Navigation aid** - File locations and class hierarchy.
- Absolute file paths
- Line number references
- Class relationships
- Test file locations

## Usage Scenarios

| Scenario | Start Here |
|----------|------------|
| Understanding architecture | [Analysis](ANDROID_MAP_CONSTRAINT_ANALYSIS.md) |
| Implementing features | [Code Patterns](ANDROID_CONSTRAINT_CODE_PATTERNS.md) |
| Quick lookup during coding | [Quick Reference](ANDROID_PATTERNS_QUICK_REFERENCE.md) |
| Finding source files | [Source File Reference](ANDROID_SOURCE_FILE_REFERENCE.md) |
| Navigation | [Index](ANDROID_MAP_CONSTRAINT_INDEX.md) |

## Key Concepts

- **Min Zoom Calculation**: Formula-based approach ensuring event fits viewport
- **BOUNDS Mode**: Constraint bounds exactly match event bounds
- **WINDOW Mode**: Viewport bounds must stay within event bounds
- **Padding**: Insets that affect viewport calculation
- **Gesture Clamping**: Preventive vs reactive constraint enforcement

## Status

**Created**: October 2025
**Purpose**: iOS implementation reference
**Maintenance**: Reference documentation, updates as needed

## Related Documentation

- [Map Architecture Analysis](../architecture/MAP_ARCHITECTURE_ANALYSIS.md) - Overall map system design
- [iOS Documentation](../ios/) - iOS-specific implementation
- [iOS/Android Parity Gap](../ios/iOS_ANDROID_MAP_PARITY_GAP_ANALYSIS.md) - Platform comparison
