# CLAUDE.md - WorldWideWaves Project Enhancement

## Project Overview
WorldWideWaves is a Kotlin Multiplatform Mobile application that leverages Compose for cross-platform UI development. This project enables seamless development for both Android and iOS platforms while sharing business logic and UI components.

**Technology Stack:**
- Kotlin Multiplatform Mobile (KMM)
- Jetpack Compose Multiplatform
- Platform-specific iOS and Android modules
- Shared business logic layer

## 🚨 MANDATORY DEVELOPMENT REQUIREMENTS - NEVER SKIP THESE

### Security Patterns for Kotlin Multiplatform Mobile + Compose
- **Data Encryption**: Use platform-specific secure storage (Keychain on iOS, EncryptedSharedPreferences on Android)
- **Network Security**: Implement certificate pinning and TLS 1.3+ for all API communications
- **Authentication**: Use biometric authentication where available, fallback to secure PIN/password
- **Code Obfuscation**: Enable ProGuard/R8 for Android, consider Swift obfuscation for iOS
- **Sensitive Data**: Never log sensitive information, use sealed classes for secure data handling

### Performance Considerations for Kotlin Multiplatform Mobile
- **Memory Management**: Be mindful of memory leaks in shared code, especially with coroutines
- **UI Performance**: Use LazyColumn/LazyRow for large datasets, implement proper state hoisting
- **Background Tasks**: Leverage platform-specific background processing capabilities
- **Bundle Size**: Minimize shared dependencies, use platform-specific implementations when necessary
- **Startup Time**: Implement lazy initialization, defer heavy operations until needed

### Error Handling Patterns
- **Result/Either Pattern**: Use sealed classes for error handling across platforms
- **Exception Boundary**: Implement try-catch blocks around platform-specific code
- **Logging**: Use structured logging with different levels (DEBUG, INFO, WARN, ERROR)
- **Crash Reporting**: Integrate platform-specific crash reporting (Crashlytics, Bugsnag)
- **Graceful Degradation**: Provide fallback behavior for platform-specific features

### Input Validation Requirements
- **Client-Side Validation**: Validate all user inputs using Compose validation
- **Sanitization**: Sanitize inputs before processing or storage
- **Type Safety**: Leverage Kotlin's type system for compile-time validation
- **Format Validation**: Use regex patterns for email, phone, and other format validations
- **Length Limits**: Enforce reasonable length limits for all text inputs

## Critical Asset Protection

### User Data Protection
- **Local Storage**: Encrypt all sensitive data using platform-specific secure storage
- **Data Transmission**: Use HTTPS/TLS for all network communications
- **Data Minimization**: Only collect and store necessary user information
- **Data Retention**: Implement automated data cleanup based on retention policies
- **GDPR Compliance**: Provide data export and deletion capabilities

### Configuration Files Protection
- **API Keys**: Never hardcode API keys, use build configurations or secure storage
- **Environment Variables**: Use build variants for different environments (dev, staging, prod)
- **Configuration Encryption**: Encrypt sensitive configuration data
- **Version Control**: Use .gitignore to exclude sensitive configuration files
- **Runtime Configuration**: Load sensitive configs at runtime from secure sources

### Access Control Patterns
- **Role-Based Access**: Implement user roles and permissions
- **Session Management**: Use secure session tokens with appropriate expiration
- **API Authorization**: Implement OAuth 2.0 or similar for API access
- **Device Authentication**: Use device fingerprinting for additional security
- **Privilege Escalation Prevention**: Validate permissions before sensitive operations

## Common Issue Prevention

### Configuration Errors Prevention
- **Build Variants**: Use separate configurations for different environments
- **Validation Scripts**: Create scripts to validate configuration completeness
- **Default Values**: Provide sensible defaults for optional configurations
- **Configuration Testing**: Test different configuration scenarios
- **Documentation**: Maintain clear documentation for all configuration options

### Dependency Issues Prevention
- **Version Catalogs**: Use Gradle version catalogs for dependency management
- **Dependency Locking**: Lock dependency versions to prevent unexpected updates
- **Conflict Resolution**: Implement strategies for resolving dependency conflicts
- **Regular Updates**: Schedule regular dependency updates with thorough testing
- **Minimal Dependencies**: Only include necessary dependencies to reduce conflicts

### Monitoring and Alerting Patterns
- **Performance Monitoring**: Track app startup time, memory usage, and network calls
- **Error Tracking**: Implement comprehensive error logging and reporting
- **User Analytics**: Track user interactions and feature usage
- **Health Checks**: Implement health check endpoints for backend services
- **Alerting**: Set up alerts for critical errors and performance degradation

### Testing Requirements
- **Unit Tests**: Test shared business logic with comprehensive unit tests
- **UI Tests**: Use Compose testing framework for UI component testing
- **Integration Tests**: Test platform-specific integrations
- **Performance Tests**: Benchmark critical paths and UI rendering
- **Accessibility Tests**: Ensure UI components meet accessibility standards

## Learning Protocol

### Pattern Recognition for Continuous Improvement
- **Code Review Feedback**: Learn from code review comments and suggestions
- **Performance Metrics**: Monitor and learn from app performance data
- **User Feedback**: Incorporate user feedback into development decisions
- **Security Audits**: Regular security reviews and vulnerability assessments
- **Best Practices Evolution**: Stay updated with Kotlin Multiplatform best practices

### Confidence Levels
- **HIGH**: Shared business logic, common UI components, established patterns
- **MEDIUM**: Platform-specific integrations, new Compose features, complex animations
- **LOW**: Experimental features, bleeding-edge KMM capabilities, complex platform bridges

### Automatic CLAUDE.md Updates
This file should be updated when:
- New critical security patterns are identified
- Performance optimizations are discovered
- Common issues and their solutions are documented
- Testing strategies are refined
- New dependency management practices are adopted

---

**Last Updated**: Generated automatically by claude-ally
**Project Type**: kotlin-multiplatform-mobile
**Confidence Level**: HIGH