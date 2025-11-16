# Console.app Guide for iOS Logging

## Quick Start

### 1. Open Console.app

```bash
# Spotlight search
Cmd+Space → "Console" → Enter

# Or from Applications
/Applications/Utilities/Console.app
```

### 2. Connect Your iOS Device

- Connect via USB cable OR
- Enable WiFi debugging: Xcode → Window → Devices → Check "Connect via network"

### 3. Select Device in Console.app

```
Console.app sidebar:
├── This Mac
└── Devices
    └── [Your iPhone/iPad name] ← Click here
```

### 4. Enable All Message Types

```
Console.app → Action menu (top bar):
✓ Include Info Messages
✓ Include Debug Messages
```

### 5. Filter Logs

#### Filter by App Package

```
Search field: process:com.worldwidewaves
```

#### Filter by Log Category

```
category:WWW
```

#### Filter by Subsystem

```
subsystem:com.worldwidewaves.shared
```

#### Combine Filters

```
process:com.worldwidewaves AND category:WWW
```

---

## Common Filters for WorldWideWaves

### Show All App Logs

```
process:com.worldwidewaves
```

### Show Only Errors

```
process:com.worldwidewaves AND type:error
```

### Show Position/GPS Logs

```
process:com.worldwidewaves AND (category:PositionManager OR category:LocationProvider)
```

### Show Event Observation Logs

```
process:com.worldwidewaves AND category:EventObserver
```

### Show Crashlytics Logs

```
process:com.worldwidewaves AND category:FirebaseCrashlytics
```

---

## Advanced Usage

### Save Logs to File

1. **Action → Save As...** or `Cmd+S`
2. Choose location
3. Select time range: "All Messages" or custom range
4. Click **Save**

### Stream Logs to Terminal

```bash
# Using log command (more powerful)
log stream --predicate 'process == "com.worldwidewaves"' --level debug

# Save to file while streaming
log stream --predicate 'process == "com.worldwidewaves"' --level debug \
  | tee ~/Desktop/ios_logs_$(date +%Y%m%d_%H%M%S).txt
```

### Export System Diagnostics

```bash
# Generate full system report (includes app logs)
sysdiagnose
# Files saved to: ~/Library/Logs/DiagnosticReports/
```

---

## Filtering Cheat Sheet

| Filter Type | Syntax | Example |
| ------------- | -------- | --------- |

| Process name | `process:NAME` | `process:com.worldwidewaves` |
| Category | `category:NAME` | `category:WWW` |
| Subsystem | `subsystem:NAME` | `subsystem:com.worldwidewaves` |
| Message type | `type:TYPE` | `type:error` or `type:fault` |
| Message contains | `KEYWORD` | `EventObserver` |
| Combine (AND) | `FILTER AND FILTER` | `process:com.worldwidewaves AND type:error` |
| Combine (OR) | `FILTER OR FILTER` | `category:WWW OR category:Event` |
| Exclude (NOT) | `NOT FILTER` | `process:com.worldwidewaves NOT category:Debug` |

---

## Message Types

| Type | Description | Visibility |
| ------ | ------------- | ------------ |

| **Default** | Standard log messages | Always visible |
| **Info** | Informational messages | Enable "Include Info Messages" |
| **Debug** | Debug-level details | Enable "Include Debug Messages" |
| **Error** | Error conditions | Always visible |
| **Fault** | Critical failures | Always visible |

---

## Troubleshooting

### "No Messages" or Empty Console

**Solution 1: Enable Debug/Info Messages**

```
Action menu → ✓ Include Info Messages
Action menu → ✓ Include Debug Messages
```

**Solution 2: Check Device Trust**

```bash
# On Mac, accept trust prompt when device connects
# On device: Settings → General → Device Management → Trust this computer
```

**Solution 3: Verify Device Connection**

```bash
# Check device is visible
instruments -s devices

# Should show:
# iPhone 15 Pro (17.0) [UDID]
```

**Solution 4: Clear Filter**

```
Click "Clear" button in search field (×)
Verify no other filters are active
```

### App Not Appearing in Process List

**Solution: Restart App**

```
1. Force quit app on device
2. Launch app again
3. In Console.app: Click "Start" button to resume streaming
```

### Logs Cut Off or Truncated

**Solution: Increase Buffer Size**

```bash
# Using log command with unlimited buffer
log stream --predicate 'process == "com.worldwidewaves"' \
  --style compact \
  --level debug \
  --timeout 0
```

---

## Best Practices

### For Development

1. **Keep Console.app open** while testing
2. **Use specific filters** to reduce noise
3. **Save logs before app restart** (logs are cleared)
4. **Use timestamps** to correlate events

### For Bug Reports

1. **Clear existing logs**: `Cmd+K`
2. **Start streaming**: Click "Start"
3. **Reproduce bug**
4. **Stop streaming**: Click "Pause"
5. **Save logs**: `Cmd+S`
6. **Include device info**:
   - iOS version
   - Device model
   - App version

### For Performance Analysis

```bash
# Record all activity with timestamps
log stream --predicate 'process == "com.worldwidewaves"' \
  --style syslog \
  --level debug \
  | awk '{print strftime("%H:%M:%S"), $0}' \
  | tee performance_log.txt
```

---

## Integration with Xcode

### Open Console from Xcode

1. **Window → Devices and Simulators**
2. Select your device
3. Click **Open Console** button
4. Same as Console.app, but pre-filtered to selected device

### Xcode Console vs Console.app

| Feature | Xcode Console | Console.app |
| --------- | --------------- | ------------- |

| Device selection | Single device | All devices |
| Filtering | Basic | Advanced |
| Export | Limited | Full control |
| Real-time | Yes | Yes |
| Historical logs | Limited | Full access |
| **Best for** | Quick checks | Deep debugging |

---

## Remote Debugging (Without Physical Access)

### Option 1: TestFlight with Feedback

```
TestFlight builds automatically collect:
- Crash logs
- User feedback
- Device info
- Screenshots
```

### Option 2: Firebase Crashlytics

```
Automatic crash reporting with:
- Stack traces
- Device info
- User actions before crash
- Custom log messages
```

### Option 3: Network Debugging

```bash
# If device on same network
# Enable network debugging in Xcode
# Then use Console.app → Network devices
```

---

## Quick Reference Commands

```bash
# Stream all app logs
log stream --predicate 'process == "com.worldwidewaves"' --level debug

# Show last 100 messages
log show --predicate 'process == "com.worldwidewaves"' --last 100

# Show logs from specific time
log show --predicate 'process == "com.worldwidewaves"' \
  --start "2025-10-29 11:00:00"

# Show only errors and faults
log stream --predicate 'process == "com.worldwidewaves" AND type >= error'

# Export to JSON
log show --predicate 'process == "com.worldwidewaves"' \
  --style json > app_logs.json
```

---

## Related Documentation

- [iOS Logging Best Practices](https://developer.apple.com/documentation/os/logging)
- [Firebase Crashlytics Setup](../README.md#firebase-setup)
- [Runtime Log Configuration](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/utils/RuntimeLogConfig.kt)
