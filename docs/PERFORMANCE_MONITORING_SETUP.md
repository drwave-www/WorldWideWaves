# Performance Monitoring Setup Guide

## Overview

WorldWideWaves now includes a comprehensive Application Performance Monitoring (APM) solution designed specifically for real-time wave coordination applications. This system goes beyond basic crash reporting to provide detailed insights into wave timing accuracy, user participation rates, and system performance.

## Architecture

### Core Components

1. **PerformanceMonitor** (Common)
   - Cross-platform performance metrics collection
   - Wave-specific timing and participation tracking
   - Memory, network, and UI performance monitoring

2. **AndroidPerformanceMonitor** (Android-specific)
   - Android system integration
   - Memory monitoring via ActivityManager
   - Compose performance tracking
   - Frame metrics collection

3. **Performance Integration** (Utility)
   - Centralized performance tracking access
   - Wave timing and participation recording
   - Screen load and interaction measurement

## Integration Setup

### 1. Initialize Performance Monitoring

Add to your Application class:

```kotlin
class MainApplication : Application() {
    private lateinit var performanceMonitor: AndroidPerformanceMonitor

    override fun onCreate() {
        super.onCreate()

        // Initialize performance monitoring
        performanceMonitor = AndroidPerformanceMonitor(this)

        // Record app startup
        performanceMonitor.recordAppStartup(
            AndroidPerformanceMonitor.StartupType.COLD,
            getAppStartupDuration()
        )
    }
}
```

### 2. Activity Integration

Add to your main activities:

```kotlin
class MainActivity : ComponentActivity() {
    private val performanceMonitor by lazy {
        AndroidPerformanceMonitor(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)

        // Record screen load time
        val loadTime = (System.currentTimeMillis() - startTime).milliseconds
        performanceMonitor.recordScreenLoad("MainActivity", loadTime)
    }
}
```

### 3. Compose Integration

Add to your Compose screens:

```kotlin
@Composable
fun EventsScreen() {
    val performanceMonitor = rememberPerformanceMonitor()

    // Automatic performance monitoring
    PerformanceMonitoringEffect(
        monitor = performanceMonitor,
        screenName = "EventsScreen"
    )

    // Your UI content
    LazyColumn {
        // Record user interactions
        items(events) { event ->
            EventItem(
                event = event,
                onClick = {
                    val startTime = System.currentTimeMillis()
                    navigateToEvent(event)
                    val responseTime = (System.currentTimeMillis() - startTime).milliseconds
                    performanceMonitor.recordUserInteraction("event_click", responseTime)
                }
            )
        }
    }
}
```

## Wave-Specific Metrics

### Critical Wave Performance Tracking

```kotlin
// Wave timing accuracy
performanceMonitor.recordWaveTimingAccuracy(
    expectedTime = waveHitTime,
    actualTime = userParticipationTime
)

// Wave participation tracking
performanceMonitor.recordWaveParticipation(
    eventId = event.id,
    participationSuccess = userSuccessfullyParticipated
)

// Choreography performance
performanceMonitor.recordChoreographyPerformance(
    sequenceId = "warming_sequence_1",
    renderTime = animationRenderTime
)
```

### Location Performance

```kotlin
// GPS accuracy monitoring
performanceMonitor.recordLocationAccuracy(
    accuracy = locationResult.accuracy
)
```

## Performance Dashboard

### Debug Integration

Add the performance dashboard to debug builds:

```kotlin
@Composable
fun DebugScreen() {
    val performanceMonitor = rememberPerformanceMonitor()

    // Performance dashboard UI has been removed
    // Performance monitoring continues through PerformanceIntegration
    Text("Performance monitoring active")
}
```

### Performance Tracking Features

- **Wave Metrics**: Wave timing accuracy, participation rates
- **System Health**: Memory usage, network latency, screen load times
- **Critical Alerts**: Performance issues requiring immediate attention
- **Recommendations**: Actionable insights for performance improvements

## Key Performance Indicators (KPIs)

### Wave Coordination KPIs

1. **Wave Timing Accuracy** - Target: >95%
   - Measures precision of user participation timing
   - Critical for coordinated wave experience

2. **Wave Participation Rate** - Target: >80%
   - Percentage of users successfully participating
   - Indicates app reliability during waves

3. **Choreography Smoothness** - Target: <16ms frame time
   - Animation performance during wave phases
   - Ensures smooth visual experience

### System Performance KPIs

1. **Screen Load Time** - Target: <2 seconds
   - Time to display functional UI
   - Critical for user experience

2. **Memory Usage** - Target: <80% of available
   - Prevents crashes and maintains performance
   - Important for older devices

3. **Network Latency** - Target: <300ms
   - API response times
   - Critical for real-time coordination

4. **Location Accuracy** - Target: <10 meters
   - GPS precision for wave participation
   - Essential for location-based features

## Performance Alerts

### Critical Issue Detection

The system automatically detects and reports:

- Wave timing accuracy below 95%
- Memory usage above 80%
- Screen load times above 3 seconds
- Network latency above 500ms
- Frame drop rates above 5%

### Alert Channels

- In-app performance dashboard
- Debug console logging
- Optional remote reporting integration

## Production Monitoring

### Recommended External Tools

For production deployments, consider integrating with:

1. **Firebase Performance Monitoring**
   - Automatic crash reporting
   - Network performance tracking
   - Custom trace integration

2. **New Relic Mobile**
   - Advanced APM features
   - User journey analytics
   - Performance alerts

3. **Datadog RUM**
   - Real-user monitoring
   - Performance benchmarking
   - Custom dashboards

### Integration Example

```kotlin
// Extend the base monitor for production reporting
class ProductionPerformanceMonitor(
    context: Context,
    private val firebasePerformance: FirebasePerformance
) : AndroidPerformanceMonitor(context) {

    override fun recordMetric(name: String, value: Double, unit: String) {
        super.recordMetric(name, value, unit)

        // Also send to Firebase
        val trace = firebasePerformance.newTrace(name)
        trace.putMetric("value", value.toLong())
        trace.start()
        trace.stop()
    }
}
```

## Best Practices

### Monitoring Guidelines

1. **Focus on User Experience**: Prioritize metrics that directly impact wave coordination
2. **Minimize Overhead**: Performance monitoring should not degrade app performance
3. **Privacy Conscious**: Avoid collecting personally identifiable information
4. **Actionable Insights**: Collect metrics that lead to specific improvements

### Performance Optimization Workflow

1. **Measure**: Use dashboard to identify performance bottlenecks
2. **Analyze**: Understand root causes using detailed metrics
3. **Optimize**: Implement targeted performance improvements
4. **Validate**: Verify improvements using before/after metrics

## Troubleshooting

### Common Issues

1. **High Memory Usage**
   - Check for memory leaks in Compose
   - Optimize image loading and caching
   - Review coroutine lifecycle management

2. **Poor Wave Timing Accuracy**
   - Verify GPS accuracy and permissions
   - Check network synchronization
   - Optimize time calculation algorithms

3. **Slow Screen Loading**
   - Implement lazy loading for heavy content
   - Optimize image processing
   - Review database query performance

## Future Enhancements

### Planned Features

1. **Predictive Performance Analytics**
   - Machine learning for performance issue prediction
   - Proactive optimization recommendations

2. **Cross-Platform Metrics**
   - iOS performance monitoring integration
   - Unified performance dashboard

3. **User Journey Analytics**
   - Complete user flow performance tracking
   - Conversion funnel optimization

---

*This performance monitoring system ensures WorldWideWaves maintains its promise of precise, reliable wave coordination while providing developers with the insights needed for continuous optimization.*