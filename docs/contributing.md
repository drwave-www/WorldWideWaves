# Contributing to WorldWideWaves

Thank you for your interest in contributing to WorldWideWaves. This guide covers how to propose changes, coding standards, and review processes.

## Getting Started

1. Read the [Environment Setup](environment-setup.md) guide
2. Review the [Architecture](architecture.md) documentation
3. Check existing [issues](https://github.com/mglcel/WorldWideWaves/issues)
4. Join discussions in pull requests

## Ways to Contribute

### Code Contributions

- Bug fixes
- New features
- Performance improvements
- Test coverage improvements
- Platform-specific implementations

### Non-Code Contributions

- Documentation improvements
- Bug reports and feature requests
- Testing and QA
- Translation assistance
- Map data generation for new cities

## Branching Model

### Branch Naming

Follow this convention:

```
<type>/<short-description>

Examples:
feat/add-tokyo-map
fix/position-update-bug
refactor/choreography-engine
docs/update-setup-guide
test/add-accessibility-tests
```

### Main Branches

| Branch | Purpose | Protected |
|--------|---------|-----------|
| `main` | Production-ready code | Yes |
| `develop` | Integration branch (if used) | Yes |

### Workflow

```bash
# 1. Create feature branch from main
git checkout main
git pull origin main
git checkout -b feat/your-feature-name

# 2. Make changes and commit
git add .
git commit -m "feat: add your feature"

# 3. Push and create PR
git push origin feat/your-feature-name
```

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(maps): add Tokyo offline map` |
| `fix` | Bug fix | `fix(position): prevent duplicate emissions` |
| `docs` | Documentation only | `docs: update contributing guide` |
| `refactor` | Code refactoring | `refactor(choreography): simplify frame sequencer` |
| `test` | Adding/updating tests | `test: add position manager tests` |
| `chore` | Build/tooling changes | `chore: upgrade Kotlin to 2.2.0` |
| `perf` | Performance improvement | `perf: optimize polygon containment check` |
| `style` | Code style changes | `style: format with ktlint` |

### Scope

Optional component name:

- `maps` - Map system
- `position` - Position management
- `choreography` - Wave choreography
- `events` - Event system
- `ui` - User interface
- `di` - Dependency injection
- `android` - Android-specific
- `ios` - iOS-specific

### Subject

- Use imperative mood: "add" not "added" or "adds"
- No capitalization
- No period at end
- Max 72 characters

### Body

- Explain what and why, not how
- Wrap at 72 characters
- Separate from subject with blank line

### Footer

- Reference issues: `Closes #123`, `Fixes #456`
- Breaking changes: `BREAKING CHANGE: description`

### Examples

```
feat(maps): add Tokyo offline map module

Implements Dynamic Feature module for Tokyo with MapLibre tiles
and custom style matching project design language.

Closes #234
```

```
fix(position): prevent duplicate position emissions

Apply debounce (500ms) and distinctUntilChanged() operators
to position stream to reduce StateFlow emissions by 80%.

Performance impact: -80% state emissions, improved battery life.

Fixes #345
```

```
refactor(choreography): simplify frame sequencer logic

Extract frame calculation into separate pure function for easier
testing and maintenance. No functional changes.
```

## Pull Request Process

### Before Opening PR

1. **Setup git hooks (first time only):**

   ```bash
   ./dev/setup-git-hooks.sh
   ```

   This enables automatic code quality checks, documentation generation, and testing.

2. **Sync with main:**

   ```bash
   git checkout main
   git pull origin main
   git checkout your-branch
   git rebase main
   ```

3. **Run tests:**

   ```bash
   ./gradlew :shared:testDebugUnitTest
   ./gradlew ktlintCheck detekt
   ```

4. **Update documentation** if needed:
   - Update relevant `docs/*.md` files for feature changes
   - Update `CLAUDE.md` for architecture/pattern changes
   - Add KDoc comments for new public APIs
   - Git hooks will generate Dokka API docs automatically on push

5. **Add tests** for new functionality

### Git Hooks - Automated Quality Checks

Git hooks run automatically on commit and push. They ensure code quality and documentation stay current.

**What hooks do automatically:**

**On Commit (pre-commit):**

- Kotlin formatting (ktlint) with auto-fix
- Static analysis (detekt) with auto-fix where possible
- Swift linting (swiftlint) with auto-fix
- Shell script validation (shellcheck)
- Copyright header addition
- Trailing whitespace removal
- Markdown linting (if `markdownlint-cli2` installed)

**On Push (pre-push):**

- Dokka API documentation generation
- Documentation update detection (warns if code changed without docs)
- Translation updates (if `OPENAI_API_KEY` set)
- Critical integration tests on Android emulator
- Automatic emulator launch if needed

**Bypass hooks (emergencies only):**

```bash
git commit --no-verify   # Skip pre-commit
git push --no-verify     # Skip pre-push
```

**Install optional markdown linting:**

```bash
npm install -g markdownlint-cli2
```

See [Development Workflow](development.md#git-hooks-setup) for complete hook documentation.

### PR Title

Follow commit convention:

```
feat(maps): add Tokyo offline map
fix(position): prevent duplicate emissions
docs: update environment setup guide
```

### PR Description Template

```markdown
## Summary
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Refactoring
- [ ] Performance improvement

## Testing
- [ ] Unit tests added/updated
- [ ] UI tests added/updated
- [ ] Manual testing completed
- [ ] Tests pass locally

## Checklist
- [ ] Code follows project style guidelines
- [ ] Documentation updated
- [ ] No new compiler warnings
- [ ] Commits follow conventional commits
- [ ] PR title follows conventional commits

## Screenshots (if UI changes)
Add screenshots or screen recordings.

## Related Issues
Closes #123
```

### Review Process

1. **Automated Checks**
   - Quality gates must pass (lint, tests, build)
   - All workflows must succeed
   - No merge conflicts

2. **Code Review**
   - At least 1 approval required
   - Address all comments
   - Keep discussion respectful and constructive

3. **Merge**
   - Use "Squash and merge" for clean history
   - Ensure commit message follows convention
   - Delete branch after merge

## Code Style Guide

### Kotlin

**Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).**

**Key points:**

```kotlin
// 1. Naming
class EventRepository { }          // PascalCase for classes
fun loadEvents() { }               // camelCase for functions
val eventList = listOf()           // camelCase for variables
const val MAX_RETRIES = 3          // SCREAMING_SNAKE_CASE for constants

// 2. Formatting
fun example(
    param1: String,
    param2: Int
): Result {
    // 4-space indentation
    return Result.Success
}

// 3. Visibility modifiers
private val internal = ""          // Explicit visibility
public val exposed = ""            // Only when necessary

// 4. Type inference
val name = "World"                 // Inferred: String
val count: Int = 42               // Explicit when not obvious

// 5. Immutability
val immutable = listOf()           // Prefer val
var mutable = mutableListOf()      // var only when necessary
```

**Import order:**

1. Android imports
2. Third-party imports (alphabetical)
3. Project imports (alphabetical)
4. Blank line before project imports

### Compose

```kotlin
// 1. Naming
@Composable
fun EventsListScreen() { }         // Noun for screens
@Composable
fun EventCard() { }                // Noun for components

// 2. State hoisting
@Composable
fun EventCard(
    event: Event,
    onEventClick: (Event) -> Unit  // Callbacks for actions
) {
    // UI implementation
}

// 3. Modifiers
Box(
    modifier = Modifier              // Modifier as first parameter
        .fillMaxSize()
        .padding(16.dp)
)

// 4. Remember for expensive operations
@Composable
fun ExpensiveView() {
    val computed = remember {        // Cache computation
        expensiveCalculation()
    }
}

// 5. Side effects
@Composable
fun DataLoader() {
    LaunchedEffect(Unit) {           // One-time effect
        loadData()
    }
}
```

### Architecture Patterns

```kotlin
// 1. Repository pattern
interface EventRepository {
    suspend fun getEvents(): List<Event>
}

class EventRepositoryImpl(
    private val api: EventApi
) : EventRepository {
    override suspend fun getEvents() = api.fetchEvents()
}

// 2. Use case pattern
class LoadEventsUseCase(
    private val repository: EventRepository
) {
    suspend operator fun invoke(): Result<List<Event>> {
        return try {
            Result.Success(repository.getEvents())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// 3. ViewModel pattern
class EventsViewModel(
    private val loadEvents: LoadEventsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<EventState>(EventState.Loading)
    val state: StateFlow<EventState> = _state.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = EventState.Loading
            when (val result = loadEvents()) {
                is Result.Success -> _state.value = EventState.Success(result.data)
                is Result.Error -> _state.value = EventState.Error(result.error)
            }
        }
    }
}
```

### Testing

```kotlin
// 1. Test naming: methodName_scenario_expectedResult
@Test
fun loadEvents_whenApiReturnsData_emitsSuccessState() {
    // Test implementation
}

// 2. Arrange-Act-Assert
@Test
fun example() {
    // Arrange
    val repository = EventRepositoryImpl(api)

    // Act
    val result = repository.getEvents()

    // Assert
    assertEquals(expectedEvents, result)
}

// 3. Use test coroutine dispatcher
@Test
fun testAsync() = runTest {
    // Test with virtual time
}

// 4. Test real code, not mocks
@Test
fun calculateDistance_realCoordinates_returnsAccurateDistance() {
    val distance = calculateDistance(
        lat1 = 48.8566, lon1 = 2.3522,  // Paris
        lat2 = 51.5074, lon2 = -0.1278  // London
    )
    assertEquals(343.5, distance, delta = 0.5)  // ~343km
}
```

## Documentation Standards

### Code Documentation

**Use KDoc for public APIs:**

```kotlin
/**
 * Calculates the great-circle distance between two points on Earth.
 *
 * Uses the Haversine formula for accuracy. Assumes Earth radius of 6371 km.
 *
 * @param lat1 Latitude of first point in degrees
 * @param lon1 Longitude of first point in degrees
 * @param lat2 Latitude of second point in degrees
 * @param lon2 Longitude of second point in degrees
 * @return Distance in kilometers
 */
fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double
```

**Inline comments for complex logic:**

```kotlin
// Apply Haversine formula
val a = sin(dLat / 2).pow(2) +
        cos(lat1) * cos(lat2) *
        sin(dLon / 2).pow(2)
```

### Markdown Documentation

**Structure:**

- H1 for title
- H2 for main sections
- H3 for subsections
- Code blocks with syntax highlighting
- Links to related docs

**Example:**

```markdown
# Feature Name

Brief description.

## Usage

\`\`\`kotlin
// Code example
\`\`\`

## Configuration

Details about configuration.

## See Also

- [Architecture](architecture.md)
- [Development Workflow](development.md)
```

## Platform-Specific Guidelines

### Android

**Activity/Fragment:**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                App()
            }
        }
    }
}
```

**Resources:**

- Use vector drawables (XML) over PNG
- Provide dark theme variants
- Use string resources for all text
- Follow Material Design 3 guidelines

### iOS

**Swift integration:**

```swift
// Call Kotlin from Swift
import Shared

let events = try EventRepositoryKt.getEvents()
```

**SwiftUI views:**

```swift
struct EventsListView: View {
    @State private var events: [Event] = []

    var body: some View {
        List(events) { event in
            EventRow(event: event)
        }
        .onAppear {
            loadEvents()
        }
    }

    private func loadEvents() {
        // Call Kotlin business logic
    }
}
```

## Issue Reporting

### Bug Reports

**Template:**

```markdown
**Describe the bug**
Clear description of the bug.

**To Reproduce**
Steps to reproduce:
1. Go to '...'
2. Click on '...'
3. See error

**Expected behavior**
What should happen instead.

**Screenshots**
If applicable.

**Environment:**
- Device: [e.g., Pixel 3a]
- OS: [e.g., Android 11]
- App version: [e.g., v0.22]

**Logs**
Attach relevant logs (use logcat for Android).
```

### Feature Requests

**Template:**

```markdown
**Is your feature request related to a problem?**
Description of the problem.

**Describe the solution**
How should it work?

**Describe alternatives**
Any alternative solutions considered?

**Additional context**
Any other context, mockups, or examples.
```

## Code Ownership

**Core areas and maintainers:**

| Area | Path | Primary Maintainer |
|------|------|-------------------|
| Position System | `shared/src/*/position/` | Core Team |
| Map System | `shared/src/*/map/` | Core Team |
| Choreography | `shared/src/*/choreographies/` | Core Team |
| Android UI | `composeApp/src/androidMain/` | Android Team |
| iOS UI | `iosApp/` | iOS Team |
| CI/CD | `.github/workflows/` | DevOps Team |

## Release Process

1. **Create release branch:**

   ```bash
   git checkout -b release/v0.23 main
   ```

2. **Update version:**
   - `composeApp/build.gradle.kts`: `versionCode`, `versionName`
   - Update `CHANGELOG.md`

3. **Test release build:**

   ```bash
   ./gradlew :composeApp:assembleRelease
   ```

4. **Tag release:**

   ```bash
   git tag -a v0.23 -m "Release v0.23"
   git push origin v0.23
   ```

5. **Deploy** (manual approval required)

## Questions?

- Open a [discussion](https://github.com/mglcel/WorldWideWaves/discussions)
- Check existing [issues](https://github.com/mglcel/WorldWideWaves/issues)
- Review [documentation](../README.md)

## Further Reading

- [Development Workflow](development.md)
- [Environment Setup](environment-setup.md)
- [Architecture](architecture.md)
- [CI/CD Pipeline](ci-cd.md)
