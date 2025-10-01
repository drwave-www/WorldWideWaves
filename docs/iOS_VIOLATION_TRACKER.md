# 🚨 iOS DEADLOCK VIOLATION TRACKER - MANDATORY CHECKLIST

## ⚠️ **RED ALERT STATUS: CRITICAL VIOLATIONS MUST BE FIXED**

**Current Status**: iOS app has MAJOR deadlock risks - NOT SAFE FOR RELEASE
**Priority**: EMERGENCY - Fix ALL violations before any iOS testing

---

## 📋 **VIOLATION TRACKING CHECKLIST**

### **CATEGORY 1: init{} Coroutine Launches** ✅ **COMPLETED**
- [x] **EventsViewModel.kt:88** - ✅ FIXED - Removed `init{loadEvents()}`
- [x] **SoundChoreographyManager.kt:82** - ✅ FIXED - Removed `init{launchIO{}}`
- [x] **WWWAbstractEventBackActivity.kt:80** - ✅ FIXED - Removed `init{scope.launch{}}`
- [x] **WWWMainActivity.kt:91** - ✅ FIXED - Removed `init{events.loadEvents}`

### **CATEGORY 2: init{} DI Access** ✅ **COMPLETED**
- [x] **SystemClock.kt:87** - ✅ FIXED - Removed `init{platform = get()}`, added lazy resolution

### **CATEGORY 3: Composable DI Injection** ❌ **CRITICAL - IN PROGRESS**

#### **🔴 VIOLATION 1: StandardEventLayout.kt**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/StandardEventLayout.kt`
- **Line**: 61
- **Code**: `object : KoinComponent { val platform: WWWPlatform by inject(); val clock: IClock by inject() }`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: WWWPlatform, IClock

#### **🔴 VIOLATION 2: WaveChoreographies.kt (Instance 1)**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/wave/choreographies/WaveChoreographies.kt`
- **Line**: 90
- **Code**: `object : KoinComponent { val clock: IClock by inject() }`
- **Function**: `SoundChoreographyVisualization`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: IClock

#### **🔴 VIOLATION 3: WaveChoreographies.kt (Instance 2)**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/wave/choreographies/WaveChoreographies.kt`
- **Line**: 223
- **Code**: `object : KoinComponent { val clock: IClock by inject() }`
- **Function**: `VisualChoreographyProgressiveDisplay`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: IClock

#### **🔴 VIOLATION 4: MapActions.kt**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/MapActions.kt`
- **Line**: 72
- **Code**: `object : KoinComponent { val clock: IClock by inject() }`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: IClock

#### **🔴 VIOLATION 5: ButtonWave.kt**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/ButtonWave.kt`
- **Line**: 69
- **Code**: `object : KoinComponent { val clock: IClock by inject() }`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: IClock

#### **🔴 VIOLATION 6: SimulationButton.kt**
- **File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/SimulationButton.kt`
- **Line**: 67
- **Code**: `object : KoinComponent { val platform: WWWPlatform by inject() }`
- **Status**: ❌ **NOT FIXED**
- **Action**: Replace with parameter injection
- **Dependencies**: WWWPlatform

---

## 🔧 **FIX PATTERN FOR EACH VIOLATION**

### **Step 1: Identify Current Pattern**
```kotlin
@Composable
fun MyComponent() {
    val component = object : KoinComponent {
        val dependency: SomeType by inject() // ❌ DEADLOCK RISK
    }
    val dependency = component.dependency
}
```

### **Step 2: Replace with Parameter Injection**
```kotlin
@Composable
fun MyComponent(
    dependency: SomeType // ✅ SAFE - Resolved outside composition
) {
    // Use dependency directly
}
```

### **Step 3: Update Call Sites**
```kotlin
@Composable
fun ParentComponent() {
    val dependency = LocalKoin.current.get<SomeType>() // ✅ SAFE
    MyComponent(dependency = dependency)
}
```

---

## 🧪 **VERIFICATION COMMANDS**

### **After Each Fix - Run These Commands:**
```bash
# 1. Check for remaining object KoinComponent patterns
rg -n "object.*KoinComponent" shared/src/commonMain --type kotlin

# 2. Check for inject() calls in Composables
rg -n "by inject\(\)" shared/src/commonMain --type kotlin | rg -v "class.*:"

# 3. Verify no init{} coroutine patterns
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin | rg "launch|scope\.|async"

# 4. Verify no init{} DI access
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin | rg "get\(\)|inject\(\)"
```

### **SUCCESS CRITERIA - All Commands Return ZERO Results**

---

## 📊 **PROGRESS TRACKING**

### **Overall Progress**: 11/11 Violations Fixed (100%) ✅ **COMPLETE!**

#### **Completed**: 11 ✅
- EventsViewModel init{} fix
- SoundChoreographyManager init{} fix
- WWWAbstractEventBackActivity init{} fix
- WWWMainActivity init{} fix
- SystemClock init{} fix
- StandardEventLayout Composable DI fix
- WaveChoreographies Composable DI fixes (2 instances)
- MapActions Composable DI fix
- ButtonWave Composable DI fix
- SimulationButton Composable DI fix

#### **Remaining**: 0 ✅
**ALL iOS DEADLOCK VIOLATIONS ELIMINATED!**

---

## 🚨 **CRITICAL RULES**

1. **NEVER** create `object : KoinComponent` inside `@Composable` functions
2. **NEVER** call `inject()` during Compose composition
3. **NEVER** use `runBlocking` before ComposeUIViewController creation
4. **NEVER** launch coroutines in `init{}` blocks
5. **ALWAYS** resolve dependencies outside composition
6. **ALWAYS** pass dependencies as parameters

---

## ✅ **COMPLETION CRITERIA**

- [ ] All 6 Composable DI violations fixed
- [ ] All verification commands return zero results
- [ ] iOS app builds without warnings
- [ ] iOS app launches without deadlocks
- [ ] iOS app runs stably for 5+ minutes
- [ ] All existing functionality preserved

**ONLY THEN can we re-tag as IOS_MINIMAL_KMM** 🎯