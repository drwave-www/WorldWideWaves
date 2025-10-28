# Operations Guide

Runtime configuration, monitoring, and operational procedures for WorldWideWaves.

## Configuration

### Runtime Configuration

**Android (DataStore):**

Application settings stored in `androidx.datastore.preferences`:

```kotlin
// Key-value pairs
enableNotifications: Boolean = true
locationUpdateInterval: Long = 5000  // milliseconds
mapStyle: String = "default"
simulationMode: Boolean = false
```

**iOS (UserDefaults):**

```swift
UserDefaults.standard.set(true, forKey: "enableNotifications")
UserDefaults.standard.set(5000, forKey: "locationUpdateInterval")
```

**Location:**
- Android: `/data/data/com.worldwidewaves/files/datastore/settings.preferences_pb`
- iOS: `~/Library/Preferences/com.worldwidewaves.plist`

### Build Configuration

**Debug vs Release:**

| Feature | Debug | Release |
|---------|-------|---------|
| Logging | Verbose | Minimal |
| Minification | Off | R8 enabled |
| Debugging | Enabled | Disabled |
| Crashlytics | Off | On |
| Firebase | Test project | Production project |

**Build config fields:**

```kotlin
// composeApp/build.gradle.kts
defaultConfig {
    buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")  // Debug
    buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "false") // Release
}
```

### Firebase Configuration

**Android:**

Auto-generated from environment variables or `local.properties`:

```bash
./gradlew generateFirebaseConfig
```

Output: `composeApp/google-services.json`

**iOS:**

Manual configuration:
1. Download `GoogleService-Info.plist` from Firebase Console
2. Add to Xcode project
3. Initialize in `AppDelegate.swift`

**Environment-specific projects:**

| Environment | Project ID | Purpose |
|-------------|-----------|---------|
| Development | `worldwidewaves-dev` | Local testing |
| Staging | `worldwidewaves-staging` | Pre-production |
| Production | `worldwidewaves-prod` | Live app |

### API Configuration

**Base URLs:**

```kotlin
// shared/src/commonMain/kotlin/com/worldwidewaves/shared/data/Config.kt
object ApiConfig {
    const val EVENT_API_BASE_URL = "https://api.worldwidewaves.net/v1"
    const val MAP_TILES_BASE_URL = "https://tiles.worldwidewaves.net"
}
```

**Override for testing:**

```kotlin
// local.properties
api.base.url=http://localhost:8080/v1
```

## Observability

### Logging

**Log Levels:**

| Level | Usage | Example |
|-------|-------|---------|
| VERBOSE | Detailed flow | Position updates, state transitions |
| DEBUG | Debugging info | Method entry/exit, variable values |
| INFO | Important events | Event loaded, wave started |
| WARNING | Recoverable errors | API retry, fallback used |
| ERROR | Errors | Exception caught, operation failed |

**Android (Logcat):**

```bash
# View all app logs
adb logcat -s WWW

# Filter by level
adb logcat WWW:E *:S  # Errors only

# Filter by component
adb logcat | grep "PositionManager"

# Save to file
adb logcat -d > app_logs.txt
```

**iOS (Console):**

```bash
# View logs
log stream --predicate 'subsystem == "com.worldwidewaves"'

# Filter by level
log show --predicate 'subsystem == "com.worldwidewaves" AND messageType == error'
```

**Production logging:**

Verbose and debug logging disabled in release builds. Only INFO, WARNING, and ERROR logged.

### Analytics

**Firebase Analytics events:**

| Event | Parameters | Purpose |
|-------|-----------|---------|
| `app_open` | - | Track app launches |
| `screen_view` | `screen_name`, `screen_class` | Track navigation |
| `event_viewed` | `event_id`, `event_name` | Track event browsing |
| `event_joined` | `event_id`, `wave_time` | Track wave participation |
| `map_loaded` | `city`, `load_time_ms` | Track map performance |
| `error` | `error_type`, `error_message` | Track errors |

**Custom dimensions:**

- User device type (phone/tablet)
- OS version
- App version
- Location permission status

**Query analytics:**

Firebase Console > Analytics > Events

### Crash Reporting

**Firebase Crashlytics:**

Automatic crash collection in production builds.

**Custom exception logging:**

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
    Log.e("Component", "Operation failed", throwable = e)
}
```

**User context:**

```kotlin
FirebaseCrashlytics.getInstance().apply {
    setUserId(userId)
    setCustomKey("event_id", eventId)
    setCustomKey("wave_status", status.name)
}
```

**View crashes:**

Firebase Console > Crashlytics > Dashboard

### Performance Monitoring

**Key Metrics:**

| Metric | Target | Alert Threshold |
|--------|--------|----------------|
| App launch time | < 2s | > 3s |
| Event load time | < 1s | > 2s |
| Map load time | < 3s | > 5s |
| Frame rate | 60 FPS | < 30 FPS |
| Memory usage | < 300 MB | > 500 MB |
| Battery drain | < 5%/hour | > 10%/hour |

**Custom traces:**

```kotlin
val trace = FirebasePerformance.getInstance().newTrace("load_events")
trace.start()
try {
    loadEvents()
    trace.putMetric("event_count", eventCount)
} finally {
    trace.stop()
}
```

**Network monitoring:**

Automatic HTTP request monitoring via Firebase Performance.

**View performance:**

Firebase Console > Performance > Dashboard

## Health Checks

### App Health Indicators

**Healthy State:**

- App launches successfully
- Events load within 2 seconds
- Position updates every 5 seconds
- Maps render without errors
- No repeated crashes

**Degraded State:**

- Slow event loading (2-5 seconds)
- Position updates intermittent
- Map tiles loading slowly
- Occasional errors recovered

**Unhealthy State:**

- App crashes on launch
- Events fail to load
- Position not updating
- Maps fail to render
- Persistent errors

### Monitoring Dashboard

**Key indicators to monitor:**

1. **Crash-free rate:** > 99%
2. **ANR rate:** < 0.1%
3. **Event load success:** > 99%
4. **Map load success:** > 98%
5. **Active users:** Tracked in Firebase Analytics

**Alerts:**

- Crash rate spike > 1%
- ANR rate > 0.5%
- Event load failures > 5%
- API errors > 10%

## Deployment

### Android Deployment

**Build release APK:**

```bash
# Generate release build
./gradlew :composeApp:assembleRelease

# Output: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**Sign APK:**

Configure signing in `local.properties`:

```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=***
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=***
```

**Upload to Play Console:**

1. Open [Google Play Console](https://play.google.com/console)
2. Select app
3. Production > Create new release
4. Upload APK
5. Add release notes
6. Review and rollout

**Staged rollout:**

Start at 10% → 25% → 50% → 100% over 7 days.

### iOS Deployment

**Build release:**

```bash
# Build iOS framework
./gradlew :shared:linkReleaseFrameworkIosArm64

# Open Xcode
open iosApp/iosApp.xcodeproj

# Archive: Product > Archive
# Upload: Window > Organizer > Distribute App
```

**Upload to TestFlight:**

1. Archive in Xcode
2. Window > Organizer
3. Distribute App > App Store Connect
4. Upload to TestFlight
5. Wait for processing
6. Add to testing group

**Submit to App Store:**

1. TestFlight approved
2. App Store Connect > My Apps > WorldWideWaves
3. Add release version
4. Submit for review
5. Wait 1-3 days for approval

### Rollback

**Android:**

```bash
# Play Console > Production > Manage
# Halt rollout immediately
# Or rollback to previous version
```

**iOS:**

```bash
# App Store Connect > My Apps > WorldWideWaves
# Remove from sale (immediate)
# Or submit previous version for expedited review
```

## Data Management

### User Data

**Stored locally:**

- User preferences (notifications, map style)
- Event cache (24-hour expiry)
- Position history (session only)
- No personally identifiable information stored

**Transmitted to backend:**

- Anonymous usage analytics
- Crash reports (anonymized)
- Performance metrics
- No location data transmitted (local only)

### Data Retention

| Data Type | Retention | Purpose |
|-----------|-----------|---------|
| Analytics events | 14 months | Usage patterns |
| Crash reports | 90 days | Debugging |
| Performance traces | 90 days | Optimization |
| Event cache | 24 hours | Offline support |

### Data Privacy

**GDPR Compliance:**

- User consent for analytics
- Data portability (export analytics)
- Right to erasure (delete user data)
- Privacy policy linked in app

**Data Security:**

- No sensitive data stored
- Location data never leaves device
- HTTPS for all network requests
- No third-party data sharing

## Backup and Recovery

### Configuration Backup

**Critical files to backup:**

```
local.properties               # Environment configuration
composeApp/google-services.json   # Firebase config (can regenerate)
*.jks                            # Android signing keys
iosApp/GoogleService-Info.plist  # iOS Firebase config
```

**Backup procedure:**

```bash
# Encrypt and backup signing keys
gpg -c worldwidewaves-release.jks
# Store encrypted file securely (1Password, AWS Secrets Manager, etc.)
```

### Disaster Recovery

**Scenario: Lost signing keys**

- **Android:** Cannot update app, must publish new app with new package name
- **Prevention:** Backup keys to secure vault, document key passwords

**Scenario: Firebase project deleted**

- **Impact:** Analytics and crashlytics data lost, app still functions
- **Recovery:** Create new Firebase project, regenerate config, redeploy
- **Prevention:** Use separate projects for dev/staging/prod, enable deletion protection

**Scenario: Code repository lost**

- **Prevention:** Distributed git (multiple clones), GitHub backup, weekly exports

**Scenario: Play Console / App Store account suspended**

- **Prevention:** Follow platform policies, maintain quality standards, respond to violations promptly

## Incident Response

### Severity Levels

| Level | Description | Response Time | Example |
|-------|-------------|---------------|---------|
| P0 - Critical | App unusable | < 1 hour | Launch crash affecting 100% users |
| P1 - High | Major feature broken | < 4 hours | Events not loading for 50% users |
| P2 - Medium | Minor feature broken | < 1 day | Map tiles slow to load |
| P3 - Low | Cosmetic issue | < 1 week | UI alignment issue |

### Response Procedure

1. **Detect:** Monitoring alert or user report
2. **Assess:** Determine severity and impact
3. **Communicate:** Notify team, create incident channel
4. **Investigate:** Review logs, crashlytics, performance data
5. **Mitigate:** Hotfix or rollback
6. **Resolve:** Deploy fix, verify resolution
7. **Post-mortem:** Document incident, improve monitoring

### Communication Channels

**Internal:**
- Slack: #incidents channel
- GitHub Issues: Create incident issue

**External:**
- Twitter/X: Status updates
- In-app: Show error message with status link
- Email: Notify affected users (if identifiable)

## Troubleshooting

### Common Production Issues

**Issue: High crash rate**

1. Check Crashlytics for common crash
2. Review recent deploys for correlation
3. Rollback if recent deploy suspected
4. Prepare hotfix for specific crash
5. Test thoroughly before redeploy

**Issue: Slow event loading**

1. Check API monitoring for latency spike
2. Verify network connectivity not at app level
3. Check CDN status if using one
4. Review server load metrics
5. Enable caching to reduce API calls

**Issue: Maps not rendering**

1. Verify MapLibre tiles accessible
2. Check for Dynamic Feature install failures
3. Review device compatibility issues
4. Check for memory pressure on device
5. Fallback to simplified map rendering

**Issue: Position not updating**

1. Check location permission status
2. Verify GPS signal available (outdoor test)
3. Review PositionManager logs
4. Check for Android battery optimization blocking background location
5. Test with mock locations

## Further Reading

- [Architecture](architecture.md)
- [Development Workflow](development.md)
- [CI/CD Pipeline](ci-cd.md)
- [Contributing Guidelines](contributing.md)
