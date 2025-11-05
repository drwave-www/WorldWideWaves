# Push Notifications System

> **Status**: Production-Ready | **Phase**: 7 - Documentation | **Tests**: 77 passing (100%)

## Overview

The WorldWideWaves push notification system delivers timely alerts about wave events through scheduled and immediate notifications. The system is designed for **favorites-only** distribution to keep notification volume manageable while providing users with critical event updates.

### Key Characteristics

- **Hybrid approach**: 6 scheduled time-based notifications + 1 immediate wave hit notification
- **Favorites-only eligibility**: Only events marked as favorites generate notifications
- **Simulation-aware**: Respects simulation mode (speed == 1 enables, speed > 1 disables)
- **32-language support**: Full localization with platform-specific resolution
- **Cross-platform**: Unified shared core + platform-specific implementations

---

## Architecture

### System Design

```
┌─────────────────────────────────────────────────────┐
│                   Shared Core                       │
├─────────────────────────────────────────────────────┤
│  NotificationTrigger (sealed class: 3 types)        │
│  NotificationContent (platform-agnostic payload)    │
│  NotificationManager (expect/actual interface)      │
│  NotificationScheduler (scheduling logic)           │
│  NotificationContentProvider (content generation)   │
│  NotificationsModule (Koin DI configuration)        │
└─────────────────────────────────────────────────────┘
         ↓                              ↓
  ┌──────────────┐           ┌──────────────┐
  │   Android    │           │     iOS      │
  ├──────────────┤           ├──────────────┤
  │WorkManager   │           │UNNotification│
  │Channels      │           │Center        │
  │NotificationM │           │Bridge        │
  │anagerCompat  │           │(Swift)       │
  └──────────────┘           └──────────────┘
```

### Core Components

#### 1. NotificationTrigger (sealed class)
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationTrigger.kt`

Defines three notification trigger types:

```kotlin
sealed class NotificationTrigger {
    abstract val id: String

    // EventStarting: 5 standard intervals (1h, 30m, 10m, 5m, 1m before event)
    data class EventStarting(val duration: Duration) : NotificationTrigger()

    // EventFinished: When event completes
    data object EventFinished : NotificationTrigger()

    // WaveHit: When wave reaches user (app must be open/backgrounded)
    data object WaveHit : NotificationTrigger()
}
```

**Unique Identifiers**:
- `EventStarting(1.hours)` → `"start_60m"`
- `EventStarting(30.minutes)` → `"start_30m"`
- `EventFinished` → `"finished"`
- `WaveHit` → `"wave_hit"`

#### 2. NotificationContent
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationContent.kt`

Platform-agnostic payload containing localization keys and deep links:

```kotlin
data class NotificationContent(
    val titleKey: String,           // e.g., "notification_event_starting"
    val bodyKey: String,            // e.g., "notification_1h_before"
    val bodyArgs: List<String> = emptyList(),  // e.g., ["New York"]
    val deepLink: String,           // e.g., "worldwidewaves://event?id=xyz"
)
```

**Key Design**:
- Contains localization KEYS, not final strings
- Platform workers resolve strings in their context (Android resource system, iOS locale)
- Body arguments support sprintf-style formatting (`%1$s`, `%2$s`)

#### 3. NotificationManager (interface)
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationManager.kt`

Platform-agnostic interface with expect/actual implementations:

```kotlin
interface NotificationManager {
    suspend fun scheduleNotification(
        eventId: String,
        trigger: NotificationTrigger,
        delay: Duration,
        content: NotificationContent,
    )

    suspend fun deliverNow(
        eventId: String,
        trigger: NotificationTrigger,
        content: NotificationContent,
    )

    suspend fun cancelNotification(eventId: String, trigger: NotificationTrigger)
    suspend fun cancelAllNotifications(eventId: String)
}

// expect/actual factory
expect fun createPlatformNotificationManager(): NotificationManager
```

#### 4. NotificationScheduler (interface)
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationScheduler.kt`

Coordinates when notifications should be scheduled:

```kotlin
interface NotificationScheduler {
    suspend fun shouldScheduleNotifications(event: IWWWEvent): Boolean
    suspend fun scheduleAllNotifications(event: IWWWEvent)
    suspend fun cancelAllNotifications(eventId: String)
    suspend fun syncNotifications(favoriteIds: Set<String>, currentEvents: List<IWWWEvent>)
}

class DefaultNotificationScheduler(...) : NotificationScheduler {
    companion object {
        val NOTIFICATION_INTERVALS = listOf(
            1.hours,    // 1 hour before start
            30.minutes, // 30 minutes before start
            10.minutes, // 10 minutes before start
            5.minutes,  // 5 minutes before start
            1.minutes,  // 1 minute before start
        )
    }
}
```

**Eligibility Checks**:
1. Event is favorited by user
2. Simulation mode is off OR speed == 1 (realistic mode)
3. Event hasn't started yet

#### 5. NotificationContentProvider
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationContentProvider.kt`

Generates platform-agnostic notification content with localization keys:

```kotlin
interface NotificationContentProvider {
    fun generateStartingNotification(event: IWWWEvent, duration: Duration): NotificationContent
    fun generateFinishedNotification(event: IWWWEvent): NotificationContent
    fun generateWaveHitNotification(event: IWWWEvent): NotificationContent
}
```

---

## Notification Types (7 Total)

### Scheduled Notifications (6)

Each favorited event generates up to 6 scheduled notifications:

| Trigger | Timing | Title Key | Body Key | Body Args |
|---------|--------|-----------|----------|-----------|
| EventStarting(1h) | 1 hour before | `notification_event_starting` | `notification_1h_before` | `[location]` |
| EventStarting(30m) | 30 min before | `notification_event_starting` | `notification_30m_before` | `[location]` |
| EventStarting(10m) | 10 min before | `notification_event_starting` | `notification_10m_before` | `[location]` |
| EventStarting(5m) | 5 min before | `notification_event_starting` | `notification_5m_before` | `[location]` |
| EventStarting(1m) | 1 min before | `notification_event_starting` | `notification_1m_before` | `[location]` |
| EventFinished | At end time | `notification_wave_completed` | `notification_event_finished` | `[eventName]` |

### Immediate Notification (1)

**WaveHit**: Delivered immediately when wave reaches user's location

- **Trigger**: `WaveHit`
- **Requirements**: App must be open or backgrounded (cannot trigger from true background)
- **Title Key**: `notification_wave_hit`
- **Body Key**: `notification_wave_at_location`
- **Body Args**: `[eventName, location]`

---

## Eligibility Criteria

### Favorites-Only Approach

Notifications are **only scheduled for events the user has favorited**. This:
- Reduces notification volume (typically <60 per user)
- Prevents overwhelming users with alerts
- Aligns with user intent (favorited = interested)
- Respects platform limits:
  - iOS: 64 pending notifications max
  - Android: ~500 pending notifications max

### Integration Points

Notifications are scheduled when:
1. User favorites an event (EventFavoriteHandler triggers scheduler)
2. App launches and syncs favorite states (AppInitializer calls scheduler.syncNotifications)

Notifications are cancelled when:
1. User unfavorites an event
2. Event is cancelled in Firestore
3. Event time changes (reschedule by cancel + re-add)

---

## Simulation Mode Behavior

The notification system respects simulation mode speed:

| Scenario | Behavior | Reason |
|----------|----------|--------|
| **Real events** (no simulation) | ✅ Schedule notifications | Real-time delivery works |
| **Realistic simulation** (speed=1) | ✅ Schedule notifications | Same timing as real events |
| **Accelerated simulation** (speed>1) | ❌ Skip notifications | Notifications would fire late/out-of-order |

**Check**: `WWWPlatform.getSimulation()?.speed`

```kotlin
// In NotificationScheduler.shouldScheduleNotifications()
val simulation = platform.getSimulation()
if (simulation != null && simulation.speed != 1) {
    return false  // Skip notification scheduling
}
```

---

## Localization Strategy (32 Languages)

### Key-Based Resolution

Notifications use **localization keys** that are resolved by each platform:

**Shared Code** (NotificationContentProvider):
```kotlin
// Creates content with keys, not strings
NotificationContent(
    titleKey = "notification_event_starting",
    bodyKey = "notification_1h_before",
    bodyArgs = listOf(location),
    deepLink = deepLink
)
```

**Android** (NotificationWorker):
```kotlin
// Resolves keys using Android resources
val title = context.getString(R.string.notification_event_starting)
val body = context.getString(R.string.notification_1h_before, location)
```

**iOS** (IOSNotificationManager):
```kotlin
// Uses iOS native localization at delivery time
content.titleLocKey = "notification_event_starting"
content.bodyLocKey = "notification_1h_before"
content.bodyLocArgs = listOf(location)  // iOS resolves with user's current locale
```

### Supported Languages (32)

- **Americas**: en, es, pt, fr (Canada)
- **Europe**: de, fr, it, nl, pl, ro, ru, tr, uk
- **Middle East**: ar, fa, he, ur
- **Africa**: am, ha, ig, sw, xh, yo, zu
- **Asia**: bn, hi, id, ja, ko, ms, pa, th, vi, zh, fil

### String Resources

All notification strings defined in:
- `shared/src/commonMain/moko-resources/base/strings.xml` (base/English)
- Automatically translated to all 32 languages by translation pipeline

**Format**: `notification_*` keys (e.g., `notification_1h_before`, `notification_wave_hit`)

---

## Platform Details

### Android Implementation

**File**: `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/AndroidNotificationManager.kt`

**Architecture**:
- **Scheduled**: WorkManager OneTimeWorkRequest with initial delay
- **Immediate**: NotificationCompat.Builder + NotificationManagerCompat
- **Persistence**: WorkManager handles rescheduling across app restarts
- **Threading**: WorkManager executes in background thread pool

**Work Naming**:
```kotlin
// Format: notification_${eventId}_${trigger.id}
"notification_abc123_start_60m"    // 1h before
"notification_abc123_start_30m"    // 30m before
"notification_abc123_finished"     // Event finished
"notification_abc123_wave_hit"     // Wave hit
```

**Update Policy**: `ExistingWorkPolicy.REPLACE`
- If notification already scheduled, new request replaces old one
- Enables update when event details change

**Notification Worker**:
**File**: `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/NotificationWorker.kt`

- CoroutineWorker that resolves localization keys and delivers notification
- Uses deep links with PendingIntent for tap routing
- Error handling: `Result.success()`/`Result.failure()` with retry policy

**Notification Channels**:
**File**: `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/NotificationChannelManager.kt`

- **Channel ID**: `WAVE_EVENTS_CHANNEL`
- **Importance**: HIGH (time-sensitive)
- **Features**: Vibration enabled, badges enabled, sound enabled
- **Android O+**: Required for all notifications

### iOS Implementation

**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/notifications/IOSNotificationManager.kt`

**Architecture**:
- **Scheduled**: UNNotificationRequest with UNTimeIntervalNotificationTrigger
- **Immediate**: 0.1 second delay trigger (closest to instant on iOS)
- **Persistence**: UNUserNotificationCenter handles across app restarts
- **Threading**: All operations dispatched to main queue

**iOS Safety [CRITICAL]**:
- ✅ CLASS-based (not object singleton)
- ✅ Lazy initialization for UNUserNotificationCenter
- ✅ NO `init{}` blocks with DI calls
- ✅ NO `object : KoinComponent` pattern

**Notification Identifiers**:
```kotlin
// Format: event_${eventId}_${trigger.id}
"event_abc123_start_60m"    // 1h before
"event_abc123_start_30m"    // 30m before
"event_abc123_finished"     // Event finished
"event_abc123_wave_hit"     // Wave hit
```

**Localization**:
- Uses `titleLocKey` and `bodyLocKey` on UNMutableNotificationContent
- iOS resolves keys at delivery time using user's locale
- No need to resolve strings in Kotlin layer

**Deep Links**:
- Stored in notification's `userInfo` dictionary
- SceneDelegate intercepts tap and routes to appropriate view

**Permission Bridge**:
**File**: `iosApp/worldwidewaves/NotificationPermissionBridge.swift`

- Swift bridge for requesting notification permissions
- Methods: `requestNotificationPermission()`, `checkNotificationPermission()`
- Requests `.alert`, `.sound`, `.badge` permissions
- Dispatches completion to main thread

### Platform Limits

| Platform | Limit | Mitigation |
|----------|-------|-----------|
| **iOS** | 64 pending notifications | Favorites-only (typical <60) |
| **Android** | ~500 pending (varies by OEM) | Favorites-only (typical <60) |

---

## Deep Link Routing

### URL Scheme

```
worldwidewaves://event?id=EVENT_ID
```

### Android Routing
Handled by manifest intent filter:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="worldwidewaves" android:host="event" />
</intent-filter>
```

### iOS Routing
Handled by SceneDelegate deep link handler:
- Intercepted in `scene(_:openURLContexts:)`
- Routes to EventDetailsScreen with event ID parameter

---

## API Reference

### NotificationScheduler Integration

```kotlin
// When user favorites an event
class EventFavoriteHandler(private val scheduler: NotificationScheduler) {
    suspend fun onEventFavorited(event: IWWWEvent) {
        if (scheduler.shouldScheduleNotifications(event)) {
            scheduler.scheduleAllNotifications(event)
        }
    }
}

// When user unfavorites
suspend fun onEventUnfavorited(eventId: String) {
    scheduler.cancelAllNotifications(eventId)
}

// On app launch
class AppInitializer(private val scheduler: NotificationScheduler) {
    suspend fun init() {
        val favorites = favoriteStore.getAllFavoriteIds()
        val events = eventRepository.getEvents(favorites)
        scheduler.syncNotifications(favorites, events)
    }
}
```

### NotificationManager Direct Use

```kotlin
// Schedule a notification
notificationManager.scheduleNotification(
    eventId = "event123",
    trigger = NotificationTrigger.EventStarting(30.minutes),
    delay = 30.minutes,
    content = NotificationContent(
        titleKey = "notification_event_starting",
        bodyKey = "notification_30m_before",
        bodyArgs = listOf("New York"),
        deepLink = "worldwidewaves://event?id=event123"
    )
)

// Deliver immediately (wave hit)
notificationManager.deliverNow(
    eventId = "event123",
    trigger = NotificationTrigger.WaveHit,
    content = contentProvider.generateWaveHitNotification(event)
)

// Cancel notifications
notificationManager.cancelAllNotifications("event123")
```

---

## Limitations & Constraints

### Platform Constraints

1. **iOS 64 Notification Limit**
   - iOS allows max 64 pending notifications
   - Current design: favorites-only keeps <60
   - Future: Could implement LRU cache if needed

2. **Accelerated Simulation Incompatible**
   - Notifications don't support time acceleration
   - Speed > 1: Notifications disabled
   - Speed = 1: Notifications work like real events

3. **Wave Hit Requires Active App**
   - Cannot trigger from true background
   - Requires app to be foreground or backgrounded
   - Depends on WaveProgressionTracker position monitoring

4. **No Delivery Guarantees**
   - Platform can suppress notifications if user disabled
   - User can swipe away pending notifications
   - No persistence of notification delivery status

### Design Constraints

1. **Favorites-Only**
   - Notifications only for favorited events
   - Cannot notify about unfavorited events
   - Users must actively favorite to enable

2. **Localization Key-Based**
   - Shared code cannot access MokoRes directly
   - Platform workers resolve strings
   - Limited to string interpolation via args

3. **No Custom Actions**
   - Current: Simple tap-to-open notifications
   - No custom action buttons (snooze, decline, etc.)

---

## Testing

### Test Coverage

**Unit Tests**: 77 tests (100% passing)

```
├── commonTest/ (27 tests)
│   ├── NotificationTriggerTest
│   ├── NotificationContentProviderTest
│   └── NotificationSchedulerTest
├── androidUnitTest/ (25 tests)
│   └── AndroidNotificationManagerTest
└── iosTest/ (25 tests)
    └── IOSNotificationManagerTest
```

### Test Organization

**Platform-Independent** (commonTest/):
- Trigger type validation
- Content provider string key generation
- Scheduler eligibility logic
- Simulation mode compatibility

**Android-Specific** (androidUnitTest/):
- WorkManager job enqueueing
- Unique work name format
- Notification cancellation
- Exception handling

**iOS-Specific** (iosTest/):
- UNNotificationCenter operations
- Identifier format validation
- Content structure verification
- iOS safety checks

### Running Tests

```bash
# All notification tests
./gradlew :shared:testDebugUnitTest

# Specific test class
./gradlew :shared:testDebugUnitTest --tests "*NotificationSchedulerTest*"

# iOS safety verification
./scripts/dev/verification/verify-ios-safety.sh
```

---

## Troubleshooting

### Notifications Not Appearing

**Checklist**:
1. Is event favorited? (Check FavoriteEventsStore)
2. Is simulation mode compatible? (Check WWWPlatform.getSimulation()?.speed)
3. Are notification permissions granted? (User → Settings → Notifications)
4. Is app in foreground/background? (Wave hit requires active app)

**Debug**:
```kotlin
val shouldSchedule = scheduler.shouldScheduleNotifications(event)
Log.d("Notifications", "Should schedule: $shouldSchedule")
```

### Permissions Issues

**Android**:
```xml
<!-- Verify AndroidManifest.xml has -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

**iOS**:
```swift
// Verify Info.plist has
<key>NSUserNotificationsUsageDescription</key>
<string>We'll notify you when your favorited wave events start</string>
```

### Deep Link Not Working

**Android**:
1. Verify intent filter in AndroidManifest.xml
2. Check deep link format: `worldwidewaves://event?id=EVENT_ID`
3. Verify EventDetailsScreen accepts `id` parameter

**iOS**:
1. Verify SceneDelegate `scene(_:openURLContexts:)` implementation
2. Check URL scheme registered in Info.plist
3. Verify deep link URL construction

---

## Related Documentation

- **[CLAUDE.md](../../CLAUDE.md)** - Project development guidelines
- **[Architecture Guide](../architecture.md)** - System architecture
- **[Testing Patterns](../testing/test-patterns.md)** - Test best practices
- **[Simulation Mode](./simulation-mode.md)** - Simulation system details
- **[iOS Development Guide](../ios/ios-development-guide.md)** - iOS-specific guidance

---

**Last Updated**: November 5, 2025
**Version**: 1.0 (Phase 7 - Documentation Complete)
**Status**: Production-Ready
