# WorldWideWaves Documentation

Complete developer documentation for WorldWideWaves - a Kotlin Multiplatform mobile app orchestrating synchronized human waves globally.

## Documentation Map

### Getting Started

Start here if you're new to the project:

1. **[Environment Setup](environment-setup.md)**
   Development environment configuration for macOS, Linux, and Windows. Covers JDK, Android Studio, Xcode, Node.js installation, and first-run verification.

2. **[Development Workflow](development.md)**
   Local dev loop, testing strategies, debugging techniques, and common troubleshooting. Essential for daily development.

3. **[Contributing Guidelines](contributing.md)**
   Contribution process, branching model, commit conventions, code style, and pull request requirements.

### Architecture & Design

Deep technical documentation:

4. **[Architecture](architecture.md)**
   System design, component responsibilities, data flows, concurrency model, and performance optimizations. Includes Mermaid diagrams.

5. **[CI/CD Pipeline](ci-cd.md)**
   GitHub Actions workflows, quality gates, testing strategy, secrets management, and release process.

### Operations

For deployment and production:

6. **[Operations Guide](operations.md)**
   Runtime configuration, monitoring, observability, deployment procedures, and incident response playbooks.

### Specialized Topics

7. **[Firebase Setup](setup/firebase-setup.md)**
   Firebase project configuration, security rules, and environment-specific setup.

8. **[Map Architecture Analysis](architecture/map-architecture-analysis.md)**
   Detailed analysis of shared vs platform-specific map system design.

## Quick Links

### Common Tasks

| Task | Documentation |
|------|--------------|
| Set up development environment | [Environment Setup](environment-setup.md) |
| Run tests locally | [Development Workflow](development.md#testing) |
| Submit a pull request | [Contributing Guidelines](contributing.md#pull-request-process) |
| Understand architecture | [Architecture](architecture.md) |
| Configure CI/CD | [CI/CD Pipeline](ci-cd.md) |
| Deploy to production | [Operations Guide](operations.md#deployment) |
| Report a bug | [Contributing Guidelines](contributing.md#issue-reporting) |

### By Role

**New Contributors:**
1. [Environment Setup](environment-setup.md)
2. [Development Workflow](development.md)
3. [Contributing Guidelines](contributing.md)

**Architects & Lead Developers:**
1. [Architecture](architecture.md)
2. [Map Architecture Analysis](architecture/map-architecture-analysis.md)
3. [CI/CD Pipeline](ci-cd.md)

**DevOps & SRE:**
1. [CI/CD Pipeline](ci-cd.md)
2. [Operations Guide](operations.md)
3. [Firebase Setup](setup/firebase-setup.md)

## Documentation Standards

### Maintenance

- Update docs when making architectural changes
- Keep code examples in sync with actual implementation
- Validate all links when updating documentation
- Use Mermaid for diagrams (text-based, version-controllable)

### Style Guide

- Use active voice
- Be concise but complete
- Include code examples where helpful
- Link to related documentation
- Use tables for structured data
- Use numbered lists for sequential steps
- Use bulleted lists for unordered items

### Markdown Conventions

```markdown
# H1 - Document Title (one per file)
## H2 - Major Sections
### H3 - Subsections
#### H4 - Detailed Topics

**Bold** for emphasis
`code` for inline code
\`\`\`language for code blocks

[Relative links](architecture.md) for internal docs
[External links](https://example.com) for external resources
```

## Project Glossary

| Term | Definition |
|------|------------|
| **Wave** | Synchronized event where participants perform coordinated action |
| **Choreography** | Sequence of timed visual/audio effects during wave |
| **Position Manager** | Unified system managing GPS and simulated positions |
| **Event Observer** | Component monitoring event state and progression |
| **Dynamic Feature** | Android on-demand module for city map data |
| **KMM** | Kotlin Multiplatform Mobile |
| **StateFlow** | Kotlin reactive state holder |
| **Use Case** | Domain layer component encapsulating business logic |

## External Resources

### Kotlin Multiplatform

- [Official KMP Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [expect/actual Declarations](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)

### Android Development

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [MapLibre Android](https://github.com/maplibre/maplibre-gl-native)

### iOS Development

- [SwiftUI](https://developer.apple.com/xcode/swiftui/)
- [Calling Kotlin from Swift](https://kotlinlang.org/docs/native-objc-interop.html)

### Tools & Frameworks

- [Koin DI](https://insert-koin.io/)
- [Firebase](https://firebase.google.com/docs)
- [GitHub Actions](https://docs.github.com/en/actions)
- [Conventional Commits](https://www.conventionalcommits.org/)

## Documentation Feedback

Found an issue or have a suggestion?

- Open an issue: [WorldWideWaves Issues](https://github.com/mglcel/WorldWideWaves/issues)
- Submit a PR with documentation improvements
- Discussion: [WorldWideWaves Discussions](https://github.com/mglcel/WorldWideWaves/discussions)

## Historical Documentation

Historical documentation (iOS map refactoring, test reports, setup guides) has been archived:

**[Archive Directory](archive/README.md)**
- iOS map refactor analysis (10 documents)
- Testing reports (5 documents)
- Setup guides (3 documents)
- Complete archive index and retrieval instructions

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2025-10-14 | 2.1 | Documentation cleanup - 18 historical docs archived |
| 2025-10-01 | 2.0 | Complete documentation rewrite with production-grade structure |
| 2025-09-23 | 1.5 | Post Position System Refactor updates |
| 2025-01-15 | 1.0 | Initial documentation |

---

**Last Updated:** October 14, 2025
**Maintainer:** WorldWideWaves Development Team
**Status:** Active Development (Android Production-Ready, iOS 95% Feature Parity)
