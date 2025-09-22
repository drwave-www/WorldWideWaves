/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.worldwidewaves.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.monitoring.AndroidPerformanceMonitor
import com.worldwidewaves.shared.monitoring.PerformanceIssue
import com.worldwidewaves.shared.monitoring.PerformanceMetrics
import kotlin.time.Duration

/**
 * Performance Dashboard for WorldWideWaves
 *
 * Provides real-time performance monitoring visualization for:
 * - Wave timing accuracy and participation rates
 * - App performance metrics (memory, screen load times)
 * - Critical issues and recommendations
 * - System health indicators
 *
 * This dashboard is intended for debug builds and internal monitoring.
 */
@Composable
fun PerformanceDashboard(
    monitor: AndroidPerformanceMonitor,
    modifier: Modifier = Modifier
) {
    val metrics by monitor.performanceMetrics.collectAsState()
    val report = monitor.getPerformanceReport()

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Performance Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PerformanceOverviewCard(metrics)
        }

        item {
            WavePerformanceCard(metrics)
        }

        item {
            SystemPerformanceCard(metrics)
        }

        if (report.criticalIssues.isNotEmpty()) {
            item {
                CriticalIssuesCard(report.criticalIssues)
            }
        }

        if (report.recommendations.isNotEmpty()) {
            item {
                RecommendationsCard(report.recommendations)
            }
        }
    }
}

@Composable
private fun PerformanceOverviewCard(metrics: PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Events Tracked",
                    value = metrics.totalEvents.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    label = "Memory Usage",
                    value = "${metrics.memoryUsagePercent.toInt()}%",
                    color = getMemoryUsageColor(metrics.memoryUsagePercent)
                )
                MetricItem(
                    label = "Location Accuracy",
                    value = "${metrics.locationAccuracy.toInt()}m",
                    color = getLocationAccuracyColor(metrics.locationAccuracy)
                )
            }
        }
    }
}

@Composable
private fun WavePerformanceCard(metrics: PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Wave Coordination Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Timing Accuracy",
                    value = "${metrics.averageWaveTimingAccuracy.toInt()}%",
                    color = getTimingAccuracyColor(metrics.averageWaveTimingAccuracy)
                )
                MetricItem(
                    label = "Participation Rate",
                    value = "${(metrics.waveParticipationRate * 100).toInt()}%",
                    color = getParticipationRateColor(metrics.waveParticipationRate)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Wave timing accuracy measures how precisely users participate in waves. " +
                        "High accuracy (>95%) indicates excellent synchronization.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SystemPerformanceCard(metrics: PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Screen Load",
                    value = formatDuration(metrics.averageScreenLoadTime),
                    color = getScreenLoadTimeColor(metrics.averageScreenLoadTime)
                )
                MetricItem(
                    label = "Network Latency",
                    value = formatDuration(metrics.averageNetworkLatency),
                    color = getNetworkLatencyColor(metrics.averageNetworkLatency)
                )
            }
        }
    }
}

@Composable
private fun CriticalIssuesCard(issues: List<PerformanceIssue>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Critical Issues (${issues.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            issues.forEach { issue ->
                IssueItem(issue)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            recommendations.forEach { recommendation ->
                Text(
                    text = "• $recommendation",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IssueItem(issue: PerformanceIssue) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(getSeverityColor(issue.severity))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = issue.impact,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// Color helpers for different metrics
@Composable
private fun getTimingAccuracyColor(accuracy: Double): Color = when {
    accuracy >= 95.0 -> Color(0xFF4CAF50) // Green
    accuracy >= 90.0 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getParticipationRateColor(rate: Double): Color = when {
    rate >= 0.8 -> Color(0xFF4CAF50) // Green
    rate >= 0.6 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getMemoryUsageColor(usage: Double): Color = when {
    usage < 60.0 -> Color(0xFF4CAF50) // Green
    usage < 80.0 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getLocationAccuracyColor(accuracy: Float): Color = when {
    accuracy <= 5.0f -> Color(0xFF4CAF50) // Green
    accuracy <= 15.0f -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getScreenLoadTimeColor(duration: Duration): Color = when {
    duration.inWholeMilliseconds < 1000 -> Color(0xFF4CAF50) // Green
    duration.inWholeMilliseconds < 2000 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getNetworkLatencyColor(duration: Duration): Color = when {
    duration.inWholeMilliseconds < 100 -> Color(0xFF4CAF50) // Green
    duration.inWholeMilliseconds < 300 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
private fun getSeverityColor(severity: PerformanceIssue.Severity): Color = when (severity) {
    PerformanceIssue.Severity.LOW -> Color(0xFF2196F3) // Blue
    PerformanceIssue.Severity.MEDIUM -> Color(0xFFFF9800) // Orange
    PerformanceIssue.Severity.HIGH -> Color(0xFFF44336) // Red
    PerformanceIssue.Severity.CRITICAL -> Color(0xFF9C27B0) // Purple
}

private fun formatDuration(duration: Duration): String {
    val ms = duration.inWholeMilliseconds
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000}s"
        else -> "${ms / 60000}m"
    }
}
