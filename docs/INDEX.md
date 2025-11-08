# WorldWideWaves Documentation Index

> **Complete documentation index for WorldWideWaves project**
> **Total Documentation**: 116 files | 54,365+ lines
> **Last Updated**: October 27, 2025
> **Status**: Production-Ready (Android), Near-Parity (iOS 98%)

---

## 🚀 Quick Navigation

### For New Contributors ("Start Here!")

1. **[README.md](../README.md)** - Project overview and quick start
2. **[Environment Setup](environment-setup.md)** - Development environment setup
3. **[Development Workflow](development.md)** - Daily development guide
4. **[Contributing Guidelines](contributing.md)** - How to contribute code
5. **[CLAUDE.md](../CLAUDE.md)** - AI assistant instructions and patterns

### For Daily Development

| I want to... | Documentation |
|--------------|--------------|
| Build the app | [Development Workflow](development.md#build-commands) |
| Run tests | [Testing Strategy](testing-strategy.md) |
| Debug a crash | [iOS Debugging Guide](ios/ios-debugging-guide.md), [Logging Guide](logging-guide.md) |
| Add a feature | [Architecture](architecture.md), [Patterns](patterns/) |
| Fix iOS deadlock | [iOS Safety Patterns](patterns/ios-safety-patterns.md), [iOS Violation Tracker](ios/ios-violation-tracker.md) |
| Improve accessibility | [Accessibility Guide](accessibility-guide.md) |
| Add a new city map | [Maps README](../scripts/maps/README.md) |
| Review a PR | [Contributing Guidelines](contributing.md#pull-request-process) |
| Deploy to production | [Operations Guide](operations.md) |

### For Architects & Technical Leads

| Topic | Documentation |
|-------|--------------|
| System architecture | [Architecture](architecture.md) |
| Map subsystem | [Map Architecture Analysis](architecture/map-architecture-analysis.md) |
| iOS/Android parity | [iOS Android Map Parity](ios/ios-android-map-parity-gap-analysis.md) |
| Code patterns | [Patterns Directory](patterns/README.md) |
| CI/CD pipelines | [CI/CD Guide](ci-cd.md) |
| Future work | [Future Work Plan](future-work-plan.md) |

---

## 📂 Documentation by Topic

### 1. Getting Started

#### Essential First Reads

| Document | Description | Size | Priority |
|----------|-------------|------|----------|
| [README.md](../README.md) | Project overview, quick start, tech stack | 450 lines | **CRITICAL** |
| [CLAUDE.md](../CLAUDE.md) | AI assistant instructions, iOS requirements, patterns | 1,550 lines | **CRITICAL** |
| [CLAUDE_iOS.md](../CLAUDE_iOS.md) | Complete iOS development guide | 850 lines | iOS devs |
| [Environment Setup](environment-setup.md) | Dev environment for macOS/Linux/Windows | 620 lines | **CRITICAL** |
| [Development Workflow](development.md) | Daily dev loop, testing, debugging | 580 lines | **CRITICAL** |
| [Contributing Guidelines](contributing.md) | Contribution process, PR requirements | 420 lines | High |

#### Platform-Specific Setup

| Document | Description |
|----------|-------------|
| [Linux Setup Guide](setup/linux-setup.md) | Ubuntu/Fedora/Arch setup, KVM acceleration |
| [Firebase Setup](setup/firebase-setup.md) | Firebase project configuration |
| [Firebase iOS Setup](setup/firebase-ios-setup.md) | iOS Firebase integration |
| [Firebase iOS Auto-Generation](setup/firebase-ios-auto-generation.md) | Automated iOS config |
| [Xcode Cleanup Guide](setup/xcode-cleanup-guide.md) | Xcode project maintenance |
| [ODR Bundle Setup](setup/odr-bundle.md) | On-Demand Resources configuration |
| [Setup README](setup/README.md) | Setup directory index |

---

### 2. Architecture & Design

#### Core Architecture

| Document | Description | Size | Focus |
|----------|-------------|------|-------|
| [Architecture](architecture.md) | System design, components, data flows | 880 lines | **CRITICAL** |
| [Map Architecture Analysis](architecture/map-architecture-analysis.md) | Detailed map subsystem design | 1,200 lines | Maps |
| [Map Architecture Verification](architecture/map-architecture-analysis-verification-report.md) | Architecture compliance report | 380 lines | Validation |
| [Architecture README](architecture/README.md) | Architecture directory index | 120 lines | Index |

#### Design Patterns

| Document | Description | Focus |
|----------|-------------|-------|
| [iOS Safety Patterns](patterns/ios-safety-patterns.md) | iOS deadlock prevention, DI safety | iOS threading |
| [Null Safety Patterns](patterns/null-safety-patterns.md) | Safe null handling, `!!` elimination | Safety |
| [Reactive Patterns](patterns/reactive-patterns.md) | StateFlow, Flow, coroutines | Reactive |
| [DI Patterns](patterns/di-patterns.md) | Koin dependency injection | DI |
| [State Management Patterns](patterns/state-management-patterns.md) | ViewModel, state handling | State |
| [Patterns README](patterns/README.md) | Patterns directory index | Index |

#### Code Style

| Document | Description |
|----------|-------------|
| [Class Organization](code-style/class-organization.md) | Class structure standards, companion objects |
| [Logging Guide](logging-guide.md) | Logging patterns, verbosity levels |
| [Correlation Tracing](correlation-tracing.md) | Request tracing, debugging |

---

### 3. Testing

#### Testing Strategy & Guides

| Document | Description | Size | Focus |
|----------|-------------|------|-------|
| [Testing Strategy](testing-strategy.md) | Overall testing approach, patterns | 520 lines | Strategy |
| [Comprehensive Test Specifications](comprehensive-test-specifications.md) | Detailed test specs by layer | 1,850 lines | Specs |
| [UI Testing Guide](ui-testing-guide.md) | Compose UI testing patterns | 420 lines | UI |
| [Test Gap Analysis](test-gap-analysis.md) | Coverage gaps and priorities | 380 lines | Analysis |
| [Map Testing Implementation Summary](map-testing-implementation-summary.md) | Map-specific test results | 290 lines | Maps |
| [Map Screens Test Specification](map-screens-test-specification.md) | Map screen testing specs | 340 lines | Maps |
| [Test Patterns](testing/test-patterns.md) | Common testing patterns | 280 lines | Patterns |
| [Testing README](testing/README.md) | Testing directory index | 95 lines | Index |

---

### 4. Platform-Specific Documentation

#### iOS Documentation

| Document | Description | Size | Priority |
|----------|-------------|------|----------|
| [CLAUDE_iOS.md](../CLAUDE_iOS.md) | Complete iOS development guide | 850 lines | **CRITICAL** |
| [iOS Success State](ios/ios-success-state.md) | Current working iOS state verification | 620 lines | High |
| [iOS Violation Tracker](ios/ios-violation-tracker.md) | Deadlock violation history (11 fixed) | 1,450 lines | **CRITICAL** |
| [iOS Debugging Guide](ios/ios-debugging-guide.md) | Advanced iOS debugging techniques | 780 lines | High |
| [iOS Map Accessibility](ios/ios-map-accessibility.md) | Map VoiceOver implementation | 520 lines | A11y |
| [iOS Gesture Analysis](ios/ios-gesture-analysis-real.md) | Map gesture implementation | 380 lines | Maps |
| [iOS Android Map Parity](ios/ios-android-map-parity-gap-analysis.md) | 97-point systematic comparison | 2,150 lines | Parity |
| [Critical Fixes Completed](ios/critical-fixes-completed.md) | Production-ready fixes summary | 420 lines | Status |
| [iOS README](ios/README.md) | iOS documentation index | 210 lines | Index |

### iOS Cinterop & Platform APIs

- [Cinterop Memory Safety Patterns](ios/cinterop-memory-safety-patterns.md) - **CRITICAL** - usePinned, useContents, addressOf
- [Swift-Kotlin Bridging Guide](ios/swift-kotlin-bridging-guide.md) - Type conversions, @objc, MapWrapperRegistry
- [Platform API Usage Guide](ios/platform-api-usage-guide.md) - UIKit/Foundation/CoreLocation threading

#### Android Documentation

| Document | Description | Size |
|----------|-------------|------|
| [Android Development Guide](android/android-development-guide.md) | Complete Android development guide | 680 lines |
| [Android Map Constraint Analysis](android/android-map-constraint-analysis.md) | Map bounds enforcement | 520 lines |
| [Android Constraint Code Patterns](android/android-constraint-code-patterns.md) | Camera constraint code | 380 lines |
| [Android Source File Reference](android/android-source-file-reference.md) | Map implementation files | 280 lines |
| [Android Patterns Quick Reference](android/android-patterns-quick-reference.md) | Quick pattern lookup | 190 lines |
| [Android Map Constraint Index](android/android-map-constraint-index.md) | Constraint documentation index | 150 lines |
| [Android README](android/README.md) | Android documentation index | 120 lines |

---

### 5. Features & Implementation

#### Core Features

| Document | Description | Size |
|----------|-------------|------|
| [Simulation Mode](features/simulation-mode.md) | Time acceleration and position simulation for testing | 730 lines |
| [Choreography System](features/choreography-system.md) | Wave choreography and timing system | 1,020 lines |

#### Accessibility

| Document | Description | Size |
|----------|-------------|------|
| [Accessibility Guide](accessibility-guide.md) | WCAG 2.1 AA compliance patterns | 1,280 lines |

#### Full Map Analysis

| Document | Description |
|----------|-------------|
| [Full Map Analysis Index](full-map-analysis-index.md) | Index of all map-related documentation |

---

### 6. Operations & CI/CD

| Document | Description | Size |
|----------|-------------|------|
| [CI/CD Guide](ci-cd.md) | GitHub Actions workflows, quality gates | 720 lines |
| [Operations Guide](operations.md) | Deployment, monitoring, observability | 620 lines |
| [GitHub Workflows README](.github/workflows/README.md) | CI/CD workflow documentation | 280 lines |

---

### 7. Planning & Future Work

| Document | Description | Size |
|----------|-------------|------|
| [Future Work Plan](future-work-plan.md) | Roadmap, optional improvements | 1,850 lines |
| [FUTURE_WORK_PLAN.md](FUTURE_WORK_PLAN.md) | Alternative future work document | 920 lines |
| [A+ Grade Action Plan](A_PLUS_GRADE_ACTION_PLAN.md) | Production excellence checklist | 580 lines |
| [Documentation Review 2025-10-27](DOCUMENTATION_REVIEW_2025-10-27.md) | Recent documentation audit | 420 lines |
| [Documentation Improvements Summary](DOCUMENTATION_IMPROVEMENTS_SUMMARY.md) | Documentation enhancements | 320 lines |

---

### 8. Development Workflows

| Document | Description |
|----------|-------------|
| [Next Session Prompt](development/next-session-prompt.md) | Template for continuing work |
| [Option A Fallback TODO](development/option-a-fallback-todo.md) | Alternative approach planning |
| [Development README](development/README.md) | Development directory index |

---

## 📚 Complete Alphabetical File List

### Root Documentation (/)

- [CLAUDE.md](../CLAUDE.md) - AI assistant instructions (1,550 lines) ⭐
- [CLAUDE_iOS.md](../CLAUDE_iOS.md) - iOS development guide (850 lines) ⭐
- [CLAUDE_MD_REORGANIZATION_SUMMARY.md](../CLAUDE_MD_REORGANIZATION_SUMMARY.md) - Doc reorganization summary
- [README.md](../README.md) - Project overview (450 lines) ⭐

### Core Documentation (docs/)

- [A_PLUS_GRADE_ACTION_PLAN.md](A_PLUS_GRADE_ACTION_PLAN.md) - Production excellence checklist
- [accessibility-guide.md](accessibility-guide.md) - WCAG 2.1 AA compliance (1,280 lines) ⭐
- [architecture.md](architecture.md) - System architecture (880 lines) ⭐
- [ci-cd.md](ci-cd.md) - CI/CD pipelines (720 lines)
- [comprehensive-test-specifications.md](comprehensive-test-specifications.md) - Test specs (1,850 lines) ⭐
- [contributing.md](contributing.md) - Contribution guidelines (420 lines) ⭐
- [correlation-tracing.md](correlation-tracing.md) - Request tracing
- [development.md](development.md) - Development workflow (580 lines) ⭐
- [DOCUMENTATION_IMPROVEMENTS_SUMMARY.md](DOCUMENTATION_IMPROVEMENTS_SUMMARY.md) - Doc improvements
- [DOCUMENTATION_REVIEW_2025-10-27.md](DOCUMENTATION_REVIEW_2025-10-27.md) - Recent audit
- [environment-setup.md](environment-setup.md) - Environment setup (620 lines) ⭐
- [full-map-analysis-index.md](full-map-analysis-index.md) - Map documentation index
- [future-work-plan.md](future-work-plan.md) - Future roadmap (1,850 lines) ⭐
- [FUTURE_WORK_PLAN.md](FUTURE_WORK_PLAN.md) - Alternative roadmap (920 lines)
- [logging-guide.md](logging-guide.md) - Logging patterns
- [map-screens-test-specification.md](map-screens-test-specification.md) - Map screen tests
- [map-testing-implementation-summary.md](map-testing-implementation-summary.md) - Map test results
- [operations.md](operations.md) - Operations guide (620 lines)
- [README.md](README.md) - Documentation index (183 lines) ⭐
- [test-gap-analysis.md](test-gap-analysis.md) - Coverage analysis (380 lines)
- [testing-strategy.md](testing-strategy.md) - Testing strategy (520 lines) ⭐
- [ui-testing-guide.md](ui-testing-guide.md) - UI testing (420 lines)

### Android Documentation (docs/android/)

- [android-constraint-code-patterns.md](android/android-constraint-code-patterns.md) - Constraint patterns
- [android-development-guide.md](android/android-development-guide.md) - Android guide (680 lines)
- [android-map-constraint-analysis.md](android/android-map-constraint-analysis.md) - Map constraints
- [android-map-constraint-index.md](android/android-map-constraint-index.md) - Constraint index
- [android-patterns-quick-reference.md](android/android-patterns-quick-reference.md) - Quick reference
- [android-source-file-reference.md](android/android-source-file-reference.md) - Source files
- [README.md](android/README.md) - Android documentation index

### Architecture Documentation (docs/architecture/)

- [map-architecture-analysis.md](architecture/map-architecture-analysis.md) - Map architecture (1,200 lines) ⭐
- [map-architecture-analysis-verification-report.md](architecture/map-architecture-analysis-verification-report.md) - Verification
- [README.md](architecture/README.md) - Architecture index

### Code Style Documentation (docs/code-style/)

- [class-organization.md](code-style/class-organization.md) - Class structure standards

### Development Documentation (docs/development/)

- [next-session-prompt.md](development/next-session-prompt.md) - Session template
- [option-a-fallback-todo.md](development/option-a-fallback-todo.md) - Alternative approach
- [README.md](development/README.md) - Development index

### iOS Documentation (docs/ios/)

- [critical-fixes-completed.md](ios/critical-fixes-completed.md) - Production fixes (420 lines)
- [ios-android-map-parity-gap-analysis.md](ios/ios-android-map-parity-gap-analysis.md) - Parity analysis (2,150 lines) ⭐
- [ios-debugging-guide.md](ios/ios-debugging-guide.md) - Debugging guide (780 lines) ⭐
- [ios-gesture-analysis-real.md](ios/ios-gesture-analysis-real.md) - Gesture implementation
- [ios-map-accessibility.md](ios/ios-map-accessibility.md) - Map accessibility (520 lines)
- [ios-success-state.md](ios/ios-success-state.md) - Success state verification (620 lines) ⭐
- [ios-violation-tracker.md](ios/ios-violation-tracker.md) - Deadlock violations (1,450 lines) ⭐
- [README.md](ios/README.md) - iOS documentation index (210 lines)

### Patterns Documentation (docs/patterns/)

- [di-patterns.md](patterns/di-patterns.md) - Dependency injection patterns
- [ios-safety-patterns.md](patterns/ios-safety-patterns.md) - iOS safety patterns ⭐
- [null-safety-patterns.md](patterns/null-safety-patterns.md) - Null safety patterns
- [reactive-patterns.md](patterns/reactive-patterns.md) - Reactive patterns
- [README.md](patterns/README.md) - Patterns index
- [state-management-patterns.md](patterns/state-management-patterns.md) - State patterns

### Setup Documentation (docs/setup/)

- [firebase-ios-auto-generation.md](setup/firebase-ios-auto-generation.md) - Auto config
- [firebase-ios-setup.md](setup/firebase-ios-setup.md) - iOS Firebase setup
- [firebase-setup.md](setup/firebase-setup.md) - Firebase configuration
- [linux-setup.md](setup/linux-setup.md) - Linux setup guide
- [odr-bundle.md](setup/odr-bundle.md) - On-Demand Resources
- [README.md](setup/README.md) - Setup index
- [xcode-cleanup-guide.md](setup/xcode-cleanup-guide.md) - Xcode maintenance

### Testing Documentation (docs/testing/)

- [README.md](testing/README.md) - Testing index
- [test-patterns.md](testing/test-patterns.md) - Common test patterns

---

## 📦 Archive Documentation

Historical documentation (session summaries, refactoring reports, test results) is preserved in the archive:

### Archive Overview

- **[Archive README](archive/README.md)** - Complete archive index and retrieval instructions
- **[Archive Review 2025-10-27](archive/ARCHIVE_REVIEW_2025-10-27.md)** - Recent archive audit

### Archive Categories

#### Session Summaries (archive/session-summaries/)

17 files documenting development sessions, refactoring efforts, and testing phases:

- AUTONOMOUS_SESSION_COMPLETE_SUMMARY.md
- CLAUDE_EVALUATION.md
- CLEANUP_AND_VALIDATION_SUMMARY.md
- COMPREHENSIVE_PROJECT_ANALYSIS.md
- COMPREHENSIVE_TEST_TODO.md
- COMPREHENSIVE_TESTING_TODO_REPORT.md
- DEVELOPMENT_WORKFLOW_ENHANCEMENTS.md
- DOCUMENTATION_SESSION_SUMMARY.md
- FINAL_SESSION_SUMMARY.md
- FINAL_TEST_IMPLEMENTATION_REPORT.md
- IOS_SEMANTIC_BRIDGING.md
- iOS_TEST_IMPROVEMENTS_SUMMARY.md
- PHASE_3_REFACTORING_SUMMARY.md
- PHASE1_TEST_IMPLEMENTATION_SUMMARY.md
- PHASE2_TEST_IMPLEMENTATION_SUMMARY.md
- PHASE3_TEST_IMPLEMENTATION_SUMMARY.md
- REFACTORING_SUMMARY.md
- TODO_ACCESSIBILITY.md
- TODO_FIREBASE_UI.md
- TODO_NEXT.md
- WORKFLOW_FIXES_2025-10-14.md
- prompt_next_session.md

#### iOS Map Refactor (archive/ios-map-refactor/)

13 files documenting the iOS map refactoring effort:

- ANDROID_VS_IOS_EVENTMAP_ANALYSIS.md
- iOS_CAMERA_COMMAND_FLOW.md
- iOS_CAMERA_COMMAND_QUEUE_FIX.md
- iOS_MAP_ACTUAL_STATUS.md
- iOS_MAP_FINAL_ASSESSMENT.md
- iOS_MAP_LOG_ANALYSIS.md
- iOS_MAP_REFACTOR_COMPLETION.md
- iOS_MAP_REFACTOR_TODO.md
- iOS_MAP_ROOT_CAUSE_ANALYSIS.md
- NEXT_SESSION_iOS_MAP.md
- NEXT_SESSION_iOS_MAP_REFACTOR.md
- SESSION_SUMMARY_iOS_MAP_REFACTOR.md

#### iOS Gesture Fixes (archive/ios-gesture-fixes-2025-10-23/)

9 files documenting iOS gesture constraint fixes:

- iOS_EDGE_TOUCH_FIX_PLAN.md
- iOS_FINAL_FIX_COMPLETE.md
- iOS_FULLMAP_CORRECT_FIX.md
- iOS_FULLMAP_FIX_IMPLEMENTATION.md
- iOS_FULLMAP_GESTURE_DIAGNOSIS_CORRECTED.md
- MINZOOM_512PX_FIX.md
- MINZOOM_NATIVE_CALC_TEST.md
- README.md
- ZERO_PADDING_FIX_VERIFICATION.md

#### Testing Reports (archive/testing-reports/)

8 files documenting test results and verification:

- E2E_TEST_RESULTS.md
- FINAL_COMPLETION_REPORT.md
- iOS_ANDROID_PARITY_VERIFICATION.md
- IOS_E2E_TEST_RESULTS.md
- LOCAL_SIMULATOR_TESTING.md
- MAP_BOUNDS_TEST_PLAN.md
- test-coverage-final-report.md
- TESTING_CHECKLIST.md

#### Setup Guides (archive/setup-guides/)

4 archived setup guides:

- FIREBASE_TEST_LAB_GUIDE.md
- iOS_FONT_SETUP_INSTRUCTIONS.md
- PRE_PUSH_VERIFICATION.md
- RUN_LOCAL_TESTS_INSTRUCTIONS.md

---

## 🗂️ Component READMEs

### Module Documentation

- [composeApp README](../composeApp/README.md) - Main application module
- [shared README](../shared/README.md) - Shared Kotlin Multiplatform code

### Scripts & Tools

- [Scripts README](../scripts/README.md) - Scripts overview
- [Maps Scripts README](../scripts/maps/README.md) - Map tile generation
- [Polygons Scripts README](../scripts/polygons/README.md) - GeoJSON polygon tools
- [Images Scripts README](../scripts/images/README.md) - Image processing
- [Licenses Scripts README](../scripts/licenses/README.md) - License management
- [Style Scripts README](../scripts/style/README.md) - MapLibre style tools
- [Translate Scripts README](../scripts/translate/README.md) - Translation tools
- [Video Translate Scripts README](../scripts/video_translate/README.md) - Video translation

### iOS App

- [iosApp/worldwidewaves/MapLibre README](../iosApp/worldwidewaves/MapLibre/README.md) - iOS MapLibre wrapper
- [iosApp/worldwidewavesUITests README](../iosApp/worldwidewavesUITests/README.md) - iOS UI tests

### GitHub

- [GitHub Workflows README](../.github/workflows/README.md) - CI/CD workflows
- [Pull Request Template](../.github/pull_request_template.md) - PR template

---

## 📊 Documentation Statistics

### Overall Metrics

- **Total Files**: 116 markdown files
- **Total Lines**: 54,365+ lines of documentation
- **Active Documentation**: 75 files
- **Archived Documentation**: 41 files
- **Last Major Update**: October 27, 2025

### Documentation Coverage by Category

| Category | Files | Approximate Size |
|----------|-------|-----------------|
| iOS-specific | 15 | 9,200 lines |
| Android-specific | 7 | 2,800 lines |
| Architecture & Design | 8 | 3,500 lines |
| Testing | 10 | 5,800 lines |
| Setup & Configuration | 7 | 2,400 lines |
| Patterns & Best Practices | 6 | 1,800 lines |
| Operations & CI/CD | 3 | 1,500 lines |
| Archive | 41 | 18,000 lines |
| Other | 19 | 9,365 lines |

### Most Critical Documents

Documents marked with ⭐ in the lists above are essential reading for their respective audiences.

**Top 10 by Size**:

1. iOS Android Map Parity Gap Analysis (2,150 lines)
2. Comprehensive Test Specifications (1,850 lines)
3. Future Work Plan (1,850 lines)
4. CLAUDE.md (1,550 lines)
5. iOS Violation Tracker (1,450 lines)
6. Accessibility Guide (1,280 lines)
7. Map Architecture Analysis (1,200 lines)
8. FUTURE_WORK_PLAN.md (920 lines)
9. Architecture (880 lines)
10. CLAUDE_iOS.md (850 lines)

---

## 🔍 Finding Documentation

### By Task

#### "I want to set up my environment"

1. [Environment Setup](environment-setup.md) - Start here
2. [Linux Setup](setup/linux-setup.md) - If on Linux
3. [Firebase Setup](setup/firebase-setup.md) - Backend configuration
4. [Xcode Cleanup Guide](setup/xcode-cleanup-guide.md) - If using Xcode

#### "I want to add a new feature"

1. [Architecture](architecture.md) - Understand system design
2. [Patterns](patterns/README.md) - Learn common patterns
3. [Development Workflow](development.md) - Development process
4. [Testing Strategy](testing-strategy.md) - Write tests
5. [Contributing Guidelines](contributing.md) - Submit PR

#### "I want to fix an iOS deadlock"

1. [iOS Violation Tracker](ios/ios-violation-tracker.md) - Known violations and fixes
2. [iOS Safety Patterns](patterns/ios-safety-patterns.md) - Prevention patterns
3. [iOS Debugging Guide](ios/ios-debugging-guide.md) - Debugging techniques
4. [CLAUDE_iOS.md](../CLAUDE_iOS.md) - Complete iOS guide

#### "I want to improve map functionality"

1. [Map Architecture Analysis](architecture/map-architecture-analysis.md) - Map design
2. [iOS Android Map Parity](ios/ios-android-map-parity-gap-analysis.md) - Platform comparison
3. [Android Map Constraint Analysis](android/android-map-constraint-analysis.md) - Android constraints
4. [Full Map Analysis Index](full-map-analysis-index.md) - All map docs

#### "I want to ensure accessibility compliance"

1. [Accessibility Guide](accessibility-guide.md) - Complete WCAG 2.1 AA guide
2. [iOS Map Accessibility](ios/ios-map-accessibility.md) - Map-specific accessibility

#### "I want to understand the testing strategy"

1. [Testing Strategy](testing-strategy.md) - Overall approach
2. [Comprehensive Test Specifications](comprehensive-test-specifications.md) - Detailed specs
3. [UI Testing Guide](ui-testing-guide.md) - UI testing
4. [Test Gap Analysis](test-gap-analysis.md) - Coverage gaps

### By Role

#### New Contributor

**Essential Reading (1-2 hours)**:

1. [README.md](../README.md)
2. [Environment Setup](environment-setup.md)
3. [Development Workflow](development.md)
4. [Contributing Guidelines](contributing.md)

**Recommended Reading**:

- [CLAUDE.md](../CLAUDE.md) - AI assistant patterns
- [Architecture](architecture.md) - System overview

#### Mobile Developer (Day-to-Day)

**Daily Reference**:

1. [Development Workflow](development.md)
2. [Testing Strategy](testing-strategy.md)
3. [Logging Guide](logging-guide.md)
4. [Patterns](patterns/README.md)

**Platform-Specific**:

- **iOS**: [CLAUDE_iOS.md](../CLAUDE_iOS.md), [iOS README](ios/README.md)
- **Android**: [Android README](android/README.md)

#### iOS Specialist

**Critical Reading**:

1. [CLAUDE_iOS.md](../CLAUDE_iOS.md) - Complete iOS guide
2. [iOS Violation Tracker](ios/ios-violation-tracker.md) - Deadlock prevention
3. [iOS Safety Patterns](patterns/ios-safety-patterns.md) - Safety patterns
4. [iOS Success State](ios/ios-success-state.md) - Current state
5. [iOS Debugging Guide](ios/ios-debugging-guide.md) - Debugging

**Advanced Topics**:

- [iOS Android Map Parity](ios/ios-android-map-parity-gap-analysis.md)
- [iOS Map Accessibility](ios/ios-map-accessibility.md)

#### Android Specialist

**Core Reading**:

1. [Android Development Guide](android/android-development-guide.md)
2. [Android Map Constraint Analysis](android/android-map-constraint-analysis.md)
3. [Android Patterns Quick Reference](android/android-patterns-quick-reference.md)

#### Architect / Technical Lead

**Architecture & Design**:

1. [Architecture](architecture.md) - System design
2. [Map Architecture Analysis](architecture/map-architecture-analysis.md) - Map subsystem
3. [iOS Android Map Parity](ios/ios-android-map-parity-gap-analysis.md) - Platform parity
4. [Patterns](patterns/README.md) - Design patterns

**Planning**:

- [Future Work Plan](future-work-plan.md) - Roadmap
- [A+ Grade Action Plan](A_PLUS_GRADE_ACTION_PLAN.md) - Production excellence

#### DevOps / SRE

**Operations**:

1. [CI/CD Guide](ci-cd.md) - CI/CD pipelines
2. [Operations Guide](operations.md) - Deployment, monitoring
3. [GitHub Workflows README](../.github/workflows/README.md) - Workflow details

**Setup**:

- [Firebase Setup](setup/firebase-setup.md) - Backend configuration
- [Environment Setup](environment-setup.md) - Development setup

---

## 🏷️ Documentation Tags

### By Maturity Level

- **Production-Ready**: CLAUDE.md, Architecture, Testing Strategy, Accessibility Guide
- **Stable**: iOS guides, Android guides, Development Workflow
- **In Progress**: Future Work Plan, some pattern documents
- **Historical**: Archive directory

### By Update Frequency

- **Frequently Updated**: CLAUDE.md, Future Work Plan, iOS Violation Tracker
- **Stable**: Architecture, Patterns, Testing Strategy
- **Reference**: API docs, Historical archive

### By Priority

- **Critical**: CLAUDE.md, README.md, Environment Setup, Development Workflow
- **High**: Architecture, Testing Strategy, iOS guides
- **Medium**: Patterns, Operations, CI/CD
- **Low**: Archive, Historical summaries

---

## 🔗 External Resources

### Kotlin Multiplatform

- [Official KMP Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [expect/actual Declarations](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)

### Android Development

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [MapLibre Android](https://github.com/maplibre/maplibre-gl-native)

### iOS Development

- [SwiftUI](https://developer.apple.com/xcode/swiftui/)
- [Calling Kotlin from Swift](https://kotlinlang.org/docs/native-objc-interop.html)
- [MapLibre iOS](https://github.com/maplibre/maplibre-gl-native)

### Tools & Frameworks

- [Koin DI](https://insert-koin.io/)
- [Firebase](https://firebase.google.com/docs)
- [GitHub Actions](https://docs.github.com/en/actions)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## 📝 Documentation Maintenance

### Update Guidelines

- Update documentation when making architectural changes
- Keep code examples in sync with implementation
- Validate all links when updating documentation
- Use Mermaid for diagrams (text-based, version-controllable)

### Documentation Standards

- **Active voice**: "Use this pattern" not "This pattern should be used"
- **Concise but complete**: Provide necessary context without verbosity
- **Code examples**: Include examples for complex concepts
- **Cross-references**: Link to related documentation
- **Tables**: Use for structured data
- **Lists**: Numbered for sequential steps, bulleted for unordered items

### Version Control

- Document major changes in file headers
- Use semantic versioning for documentation versions
- Archive outdated documentation to archive/ directory
- Update this index when adding/removing documentation

---

## 🆘 Getting Help

### Documentation Issues

- **Found a broken link?** Open an issue or submit a PR
- **Documentation unclear?** Open a discussion or issue
- **Missing documentation?** Check archive or request in issues

### Project Support

- **Issues**: [GitHub Issues](https://github.com/mglcel/WorldWideWaves/issues)
- **Discussions**: [GitHub Discussions](https://github.com/mglcel/WorldWideWaves/discussions)
- **Pull Requests**: See [Contributing Guidelines](contributing.md)

---

## 📅 Version History

| Date | Version | Changes |
|------|---------|---------|
| 2025-10-27 | 3.0 | Complete documentation index with multi-dimensional organization |
| 2025-10-14 | 2.1 | Documentation cleanup - 18 historical docs archived |
| 2025-10-01 | 2.0 | Complete documentation rewrite with production-grade structure |
| 2025-09-23 | 1.5 | Post Position System Refactor updates |
| 2025-01-15 | 1.0 | Initial documentation |

---

**Maintained by**: WorldWideWaves Development Team
**Last Updated**: October 27, 2025
**Status**: Production-Ready (Android), Near-Parity (iOS 98%)
**License**: See project LICENSE file
