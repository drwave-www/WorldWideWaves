# API Documentation Guide

Complete guide for generating, viewing, and deploying WorldWideWaves API documentation using Dokka.

## Quick Start

```bash
# Generate documentation
./gradlew :shared:dokkaHtml

# View documentation
open shared/build/dokka/index.html
```

Build time: ~3 minutes | Output size: ~23 MB

## Coverage

The generated API documentation covers all public APIs in the shared Kotlin Multiplatform module:

- Domain models (Event, Wave, Position)
- Repositories (EventRepository, PositionManager)
- ViewModels (EventListViewModel, EventDetailViewModel)
- Use cases (EventObserver, WaveProgressionTracker)
- Utilities (logging, date/time, coordinates)

## Configuration

Dokka is configured in `shared/build.gradle.kts`:

```kotlin
tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    moduleName.set("WorldWideWaves Shared")
    dokkaSourceSets {
        named("commonMain") {
            sourceLink {
                localDirectory.set(file("src/commonMain/kotlin"))
                remoteUrl.set(URL("https://github.com/drwave-www/WorldWideWaves/tree/main/shared/src/commonMain/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
```

## Deployment Options

### GitHub Pages

1. Generate docs: `./gradlew :shared:dokkaHtml`
2. Copy to docs: `cp -r shared/build/dokka/* docs/api/`
3. Commit and enable GitHub Pages

### CI/CD Automation

Create `.github/workflows/deploy-api-docs.yml` to auto-deploy on code changes.

### Offline Archive

```bash
cd shared/build
zip -r api-docs-$(date +%Y%m%d).zip dokka/
```

## Best Practices

### Writing KDoc

```kotlin
/**
 * Manages user position tracking from GPS and simulation sources.
 *
 * @property positionFlow Reactive stream of position updates
 * @param gpsProvider Platform-specific GPS provider
 * @see Position
 * @since 1.0.0
 */
class PositionManager(private val gpsProvider: GPSProvider) {
    // ...
}
```

### Coverage Goals

- Public APIs: 100% documented
- Internal classes: Document if non-trivial
- Private methods: Document complex logic only

## Troubleshooting

**Build failures:** Ensure all imports exist and KDoc links are valid

**Missing classes:** Verify classes are public and in `src/commonMain/kotlin`

**Broken GitHub links:** Check `remoteUrl` matches repository structure

## Related Resources

- [Dokka Official Docs](https://kotlin.github.io/dokka/)
- [KDoc Format](https://kotlinlang.org/docs/kotlin-doc.html)
- [WorldWideWaves Architecture](architecture.md)

---

**Last Updated:** October 27, 2025
**Dokka Version:** 1.9.20
