/*
 * Copyright 2025 DrWave
 *
 * Test configuration for WorldWideWaves test suite optimization
 * This file contains shared test configuration, quality gates, and CI integration settings
 */

// Test Quality Gates Configuration
object TestQualityGates {
    const val UNIT_TEST_TIMEOUT_SECONDS = 30
    const val INTEGRATION_TEST_TIMEOUT_SECONDS = 300
    const val E2E_TEST_TIMEOUT_SECONDS = 1800

    const val MIN_UNIT_COVERAGE_PERCENT = 90
    const val MIN_INTEGRATION_COVERAGE_PERCENT = 80
    const val MIN_MUTATION_SCORE_PERCENT = 80

    const val MAX_FLAKY_TEST_COUNT = 0
    const val MAX_TEST_SUITE_DURATION_MINUTES = 10
}

// Test Performance Budgets
object TestPerformanceBudgets {
    const val UNIT_TEST_MAX_MS = 100
    const val INTEGRATION_TEST_MAX_MS = 5000
    const val E2E_TEST_MAX_MS = 60000

    const val GEOMETRIC_CALCULATION_BUDGET_MS = 50
    const val EVENT_VALIDATION_BUDGET_MS = 25
    const val POSITION_UPDATE_BUDGET_MS = 10
}

// Test Categories for Selective Execution
object TestCategories {
    const val UNIT = "unit"
    const val INTEGRATION = "integration"
    const val E2E = "e2e"
    const val SECURITY = "security"
    const val PERFORMANCE = "performance"
    const val BDD = "bdd"
    const val PROPERTY_BASED = "property"
    const val CONCURRENCY = "concurrency"
}

// Anti-pattern Detection Rules
object TestAntiPatterns {
    val FORBIDDEN_PATTERNS = listOf(
        "Thread.sleep" to "Use TestCoroutineScheduler or delay() instead",
        "System.currentTimeMillis" to "Inject clock dependency for deterministic testing",
        "Random()" to "Use Random(seed) for reproducible tests",
        "mockk<.*>.*returns.*mockk" to "Avoid testing mock implementations",
        "verify.*never" to "Focus on behavior verification, not mock interactions",
        "testTag.*TestComponent" to "Test real components, not test doubles",
        "eventually\\s*\\{" to "Use deterministic waiting with TestCoroutineScheduler"
    )

    val REQUIRED_PATTERNS = listOf(
        "@Test" to "All test methods must be annotated",
        "class.*Test" to "Test classes must end with 'Test'",
        "package.*test" to "Tests must be in test packages"
    )
}

// CI Integration Commands
object CICommands {
    const val UNIT_TESTS = "./gradlew test --parallel --build-cache"
    const val INTEGRATION_TESTS = "./gradlew connectedAndroidTest --build-cache"
    const val REAL_INTEGRATION_TESTS = "./gradlew realIntegrationTest"
    const val COVERAGE_REPORT = "./gradlew jacocoTestReport koverHtmlReport"
    const val MUTATION_TESTING = "./gradlew pitest --testMutationThreshold=${TestQualityGates.MIN_MUTATION_SCORE_PERCENT}"
    const val LINT_TESTS = "grep -r 'Thread.sleep\\|System.currentTimeMillis' src/test/ && exit 1 || exit 0"
}

// Test Parallelization Configuration
fun configureTestExecution(testTask: org.gradle.api.tasks.testing.Test) {
    testTask.apply {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

        systemProperty("test.timeout.seconds", TestQualityGates.UNIT_TEST_TIMEOUT_SECONDS)
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")

        // Test filtering by category
        if (project.hasProperty("testCategory")) {
            val category = project.property("testCategory").toString()
            useJUnitPlatform {
                includeTags(category)
            }
        }

        // Performance monitoring
        beforeTest { descriptor ->
            logger.lifecycle("Starting test: ${descriptor.displayName}")
        }

        afterTest { descriptor, result ->
            val duration = result.endTime - result.startTime
            if (duration > TestPerformanceBudgets.UNIT_TEST_MAX_MS) {
                logger.warn("Slow test detected: ${descriptor.displayName} took ${duration}ms")
            }
        }

        // Fail fast on quality gate violations
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }

        reports {
            html.required.set(true)
            junitXml.required.set(true)
        }
    }
}

// Mutation Testing Configuration
fun configureMutationTesting() {
    // Target packages for mutation testing
    val mutationTargets = listOf(
        "com.worldwidewaves.shared.events",
        "com.worldwidewaves.shared.events.utils",
        "com.worldwidewaves.shared.position",
        "com.worldwidewaves.shared.choreographies"
    )

    // Expected mutation operators
    val mutationOperators = listOf(
        "MATH", // Arithmetic operators
        "CONDITIONALS_BOUNDARY", // <, >, ==, !=
        "INCREMENTS", // ++, --
        "INVERT_NEGS", // Boolean negation
        "RETURN_VALS", // Return value modifications
        "VOID_METHOD_CALLS" // Method call removals
    )
}

// Test Data Builders Configuration
object TestDataBuilders {
    // Deterministic test data generation
    const val DEFAULT_SEED = 42L

    // Geographic test data bounds
    const val TEST_LAT_MIN = -89.0
    const val TEST_LAT_MAX = 89.0
    const val TEST_LNG_MIN = -179.0
    const val TEST_LNG_MAX = 179.0

    // Test event configuration
    const val TEST_EVENT_RADIUS_KM = 2.0
    const val TEST_WAVE_SPEED_KMH = 50.0
    const val TEST_EVENT_DURATION_HOURS = 2
}

// Security Test Configuration
object SecurityTestConfig {
    val MALICIOUS_COORDINATES = listOf(
        -91.0 to 0.0, // Invalid latitude
        91.0 to 0.0,
        0.0 to -181.0, // Invalid longitude
        0.0 to 181.0,
        Double.NaN to 0.0, // NaN injection
        Double.POSITIVE_INFINITY to 0.0,
        Double.NEGATIVE_INFINITY to 0.0
    )

    val MALICIOUS_STRINGS = listOf(
        "../../../etc/passwd", // Path traversal
        "<script>alert('xss')</script>", // XSS
        "'; DROP TABLE events; --", // SQL injection
        "event\u0000null", // Null byte injection
        "event\r\nX-Injected: header", // Header injection
        "a".repeat(10000) // DoS via large input
    )

    val MALICIOUS_JSON = listOf(
        """{"admin": true}""", // Privilege escalation
        """{"name": "event\",\"admin\":true,\"x\":\""}""", // JSON injection
        """{"__proto__": {"admin": true}}""" // Prototype pollution
    )
}