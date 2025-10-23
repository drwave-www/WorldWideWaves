# Documentation Archive

This directory contains historical documentation that has been superseded or completed.

**Last Updated**: October 14, 2025

---

## Archive Structure

### `/ios-map-refactor/`
Historical documentation from the iOS MapLibre implementation and refactoring effort (September-October 2025).

**Status**: ✅ Work completed October 8, 2025
**Current documentation**: See `docs/ios/ios-map-implementation-status.md` and `CLAUDE_iOS.md`

**Contents** (10 documents):
- `ANDROID_VS_IOS_EVENTMAP_ANALYSIS.md` - Deep architecture comparison
- `iOS_MAP_ACTUAL_STATUS.md` - Status snapshot
- `iOS_MAP_FINAL_ASSESSMENT.md` - Final assessment
- `iOS_MAP_LOG_ANALYSIS.md` - Log analysis
- `iOS_MAP_REFACTOR_COMPLETION.md` - Completion report
- `iOS_MAP_REFACTOR_TODO.md` - Original refactor plan
- `iOS_MAP_ROOT_CAUSE_ANALYSIS.md` - Root cause investigation
- `iOS_CAMERA_COMMAND_FLOW.md` - Camera command implementation
- `iOS_CAMERA_COMMAND_QUEUE_FIX.md` - Queue fix analysis
- `NEXT_SESSION_iOS_MAP.md` / `NEXT_SESSION_iOS_MAP_REFACTOR.md` - Session prompts
- `SESSION_SUMMARY_iOS_MAP_REFACTOR.md` - Session summary

### `/testing-reports/`
Historical test execution reports and verification documentation.

**Status**: Superseded by continuous CI/CD testing
**Current documentation**: See `docs/testing-strategy.md` and `docs/ci-cd.md`

**Contents** (5 documents):
- `E2E_TEST_RESULTS.md` - End-to-end test results
- `FINAL_COMPLETION_REPORT.md` - Project completion report
- `IOS_E2E_TEST_RESULTS.md` - iOS E2E results
- `LOCAL_SIMULATOR_TESTING.md` - Local testing guide
- `iOS_ANDROID_PARITY_VERIFICATION.md` - Platform parity verification

### `/setup-guides/`
One-time setup instructions and historical configuration guides.

**Status**: Setup completed, kept for reference
**Current documentation**: See `docs/environment-setup.md` and `docs/setup/`

**Contents** (4 documents):
- `FIREBASE_TEST_LAB_GUIDE.md` - Firebase Test Lab setup
- `iOS_FONT_SETUP_INSTRUCTIONS.md` - iOS font configuration
- `RUN_LOCAL_TESTS_INSTRUCTIONS.md` - Local test execution
- `PRE_PUSH_VERIFICATION.md` - Pre-push verification script

### `/session-summaries/`
Historical session summaries and progress reports from development work (2025).

**Status**: Historical record of completed work
**Current documentation**: Active work tracked in `TODO_NEXT.md` and `CLAUDE.md`

**Contents** (17 documents):
- Test implementation summaries (Phases 1-3)
- Refactoring session summaries
- Project analysis and evaluation reports
- Workflow enhancement documentation
- Comprehensive testing plans (historical)

---

## Why These Were Archived

### Completion
Most documents represent completed work:
- iOS MapLibre implementation is production-ready (95% feature parity)
- All critical memory leaks resolved (10/10 fixed)
- Testing infrastructure established

### Superseded
Better documentation now exists:
- `docs/ios/ios-map-implementation-status.md` - Current iOS map status
- `CLAUDE_iOS.md` - Complete iOS development guide
- `docs/testing-strategy.md` - Testing approach
- `docs/ci-cd.md` - CI/CD pipeline documentation

### Historical Value
These documents are preserved for:
- Understanding architectural decisions
- Learning from past analysis
- Reference during similar future work
- Project history and audit trail

---

## Active Documentation

For current, maintained documentation, see:

**Root Level**:
- `README.md` - Project overview
- `CLAUDE.md` - Development instructions
- `CLAUDE_iOS.md` - iOS-specific guide

**`/docs/` Directory**:
- `docs/README.md` - Documentation index
- `docs/architecture.md` - System architecture
- `docs/development.md` - Development workflow
- `docs/testing-strategy.md` - Testing approach
- `docs/ci-cd.md` - CI/CD pipeline
- `docs/ios/ios-map-implementation-status.md` - Current iOS map status
- `docs/iOS_*.md` - iOS-specific documentation

---

## Retrieval

If you need to reference archived documentation:

```bash
# Find specific content
grep -r "search term" docs/archive/

# View archive structure
tree docs/archive/

# Read specific archived doc
cat docs/archive/ios-map-refactor/iOS_MAP_REFACTOR_TODO.md
```

---

**Note**: This archive is version-controlled and can be accessed from any commit in the repository history.
