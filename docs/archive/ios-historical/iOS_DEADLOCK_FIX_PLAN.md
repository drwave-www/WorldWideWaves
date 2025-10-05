# iOS Deadlock Fix Plan - Critical Violations

## 🚨 Problem Statement

The iOS app currently has critical `runBlocking` deadlock patterns that violate iOS ComposeUIViewController requirements. Any `init{}` block that launches coroutines on `Dispatchers.Main` or performs async work during DI creation will deadlock iOS.

## 🎯 Critical Violations to Fix

### 1. EventsViewModel.kt:88-90
```kotlin
// ❌ CURRENT (DEADLOCK)
init {
    loadEvents() // Launches coroutine in init
}

// ✅ FIXED
// Remove init{}, move to @Composable LaunchedEffect
```

### 2. SoundChoreographyManager.kt:82-86
```kotlin
// ❌ CURRENT (DEADLOCK)
init {
    coroutineScopeProvider.launchIO {
        preloadMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE)
    }
}

// ✅ FIXED
// Remove init{}, add suspend fun initialize() called from LaunchedEffect
```

### 3. WWWAbstractEventBackActivity.kt:74,80-84
```kotlin
// ❌ CURRENT (DEADLOCK)
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
init {
    scope.launch {
        trackEventLoading(eventId)
    }
}

// ✅ FIXED
// Remove init{}, call trackEventLoading from @Composable LaunchedEffect
```

### 4. WWWMainActivity.kt:91-96
```kotlin
// ❌ CURRENT (POTENTIAL DEADLOCK)
init {
    events.loadEvents(onTermination = { ... })
}

// ✅ FIXED
// Remove init{}, move events.loadEvents to @Composable LaunchedEffect
```

## 🔧 Fix Strategy

### Phase 1: Remove All init{} Coroutine Launches
1. **EventsViewModel**: Remove `init{}`, expose `suspend fun load()`
2. **SoundChoreographyManager**: Remove `init{}`, add `suspend fun initialize()`
3. **WWWAbstractEventBackActivity**: Remove `init{}`, expose `suspend fun start()`
4. **WWWMainActivity**: Remove `init{}`, move events loading to Draw()

### Phase 2: LaunchedEffect Pattern
```kotlin
@Composable
fun MyScreen() {
    val viewModel = remember { MyViewModel() } // No work in init

    LaunchedEffect(Unit) {
        viewModel.load() // All async work here
    }

    UI(viewModel.state)
}
```

### Phase 3: DI Pattern
```kotlin
// ✅ SAFE: No coroutines in providers
val myModule = module {
    single { MyRepository() } // Pure constructor only
    single { MyViewModel() }  // No init{} work
}

// ✅ SAFE: Async work after UI created
@Composable
fun App() {
    LaunchedEffect(Unit) {
        // All initialization here
        repositories.forEach { it.initialize() }
    }
}
```

## 📝 Implementation Steps

### Step 1: Fix EventsViewModel
- Remove `init { loadEvents() }`
- Change `loadEvents()` to `suspend fun load()`
- Update callers to use `LaunchedEffect { viewModel.load() }`

### Step 2: Fix SoundChoreographyManager
- Remove `init { coroutineScopeProvider.launchIO { ... } }`
- Add `suspend fun initialize()`
- Call from `LaunchedEffect { soundManager.initialize() }`

### Step 3: Fix WWWAbstractEventBackActivity
- Remove `init { scope.launch { ... } }`
- Add `suspend fun start()`
- Call from `LaunchedEffect { activity.start() }`

### Step 4: Fix WWWMainActivity
- Remove `init { events.loadEvents(...) }`
- Move to `LaunchedEffect` in `Draw()` method
- Ensure splash logic works with async pattern

### Step 5: Test iOS Stability
- Verify no `runBlocking` in startup path
- Test cold start multiple times
- Confirm no deadlocks on iOS simulator

## 🧪 Validation Commands

```bash
# Check for violations
rg -n "init\s*\{.*launch" shared/src/commonMain
rg -n "init\s*\{.*coroutine" shared/src/commonMain
rg -n "Dispatchers\.Main.*init" shared/src/commonMain

# Should return zero results after fixes
```

## ✅ Success Criteria

1. **No `init{}` blocks with coroutine launches**
2. **No `Dispatchers.Main` usage in constructors/init**
3. **All async work moved to `LaunchedEffect`**
4. **iOS app launches without deadlocks**
5. **Cold start stability verified**

## 🚨 Critical Priority

These fixes must be completed before the iOS app can be considered stable. The current "working" state is fragile and will deadlock under different conditions.