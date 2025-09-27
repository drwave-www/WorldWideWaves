# iOS Implementation Plan - Systematic Approach

## Objective
Fix iOS app to properly display events in simulator using the learned best practices.

## Key Learning
- iOS app WAS working (com.worldwidewaves.WorldWideWavesDrWaves shows events perfectly)
- Problem was testing wrong app (com.worldwidewaves.iosApp)
- Need to apply fixes to correct project configuration

## Phase 1: Establish Working Methodology
### Question: xcodebuild vs Xcode
- **Test**: Can we work with xcodeproj command line or must use Xcode GUI?
- **Method**: Try both approaches with simple changes
- **Decision**: Use the approach that reliably reflects changes

## Phase 2: Clean Baseline
### Revert to Known Working Point
- **Target**: Commit 79f20c85270e769e8b7018d18a5d73eb307475b1
- **Verify**: Basic iOS app launches without crashes
- **Validate**: Logs appear and can be monitored

## Phase 3: Apply Best Practices
### Dependency Management
- [ ] Remove Android-specific libraries from commonMain
- [ ] Use explicit org.jetbrains.compose.* dependencies
- [ ] Block androidx.lifecycle modules in iOS/common configurations
- [ ] Verify dependency trees are clean

### Coroutine Best Practices
- [ ] Remove all runBlocking calls from commonMain
- [ ] Fix Dispatchers.Main usage during initialization
- [ ] Disable problematic init{} blocks with coroutine launches
- [ ] Use background scopes for non-UI operations

## Phase 4: Systematic WWWMainActivity Integration
### Step 1: Verify Basic Compose
- [ ] Test simple Text("Hello iOS") works
- [ ] Ensure logs appear from ComposeUIViewController block

### Step 2: Add Minimal KMM Integration
- [ ] Add Koin initialization
- [ ] Test dependency injection works
- [ ] Verify no deadlocks during init

### Step 3: Add Events Loading
- [ ] Integrate WWWEvents without complex UI
- [ ] Show simple events count first
- [ ] Verify 40 events load successfully

### Step 4: Add Basic Events Display
- [ ] Show events list with LazyColumn
- [ ] Test scrolling and interaction
- [ ] Verify events appear: Paris, Rio, etc.

### Step 5: Add Full UI Features
- [ ] Integrate TabManager
- [ ] Add AboutTabScreen
- [ ] Add splash screen behavior
- [ ] Test all features work

## Phase 5: Validation
### Success Criteria
- [ ] Events display correctly in simulator
- [ ] No crashes during startup or navigation
- [ ] Logs show proper initialization flow
- [ ] Performance is acceptable

## Rollback Strategy
- If any step fails, revert to previous working step
- Document exact failure point and symptoms
- Apply targeted fix before proceeding

## Notes
- Test each step thoroughly before proceeding
- Commit working states as checkpoints
- Keep changes minimal and focused
- Prioritize stability over features