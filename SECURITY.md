# Security Policy

## Supported Versions

Currently supported versions for security updates:

| Version | Supported          |
| ------- | ------------------ |
| 0.9.x   | :white_check_mark: |
| < 0.9   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, report them via email to: security@worldwidewaves.com

You should receive a response within 48 hours. If for some reason you do not, please follow up via email to ensure we received your original message.

Please include the following information in your report:

- **Type of vulnerability** (e.g., injection, authentication bypass, data exposure)
- **Full paths of source file(s)** related to the vulnerability
- **Location of the affected source code** (tag/branch/commit or direct URL)
- **Step-by-step instructions to reproduce** the issue
- **Proof-of-concept or exploit code** (if possible)
- **Impact of the issue** including potential attack scenarios
- **Any suggested fixes** (optional but appreciated)

## Security Response Process

1. **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours
2. **Assessment**: Our team will investigate and assess the severity within 5 business days
3. **Updates**: We will keep you informed of our progress throughout the investigation
4. **Resolution**: We will work to patch confirmed vulnerabilities as quickly as possible
5. **Disclosure**: We will coordinate public disclosure with you after the fix is released

## Security Measures

WorldWideWaves implements multiple security measures to protect user data and ensure safe operation:

### Data Protection
- **Location Privacy**: User location data is handled with appropriate privacy measures
- **Ephemeral Events**: Events are designed to be temporary, minimizing long-term data storage
- **No Credential Exposure**: API keys, tokens, and secrets are never logged or transmitted insecurely

### Input Validation
- **Geographic Coordinates**: All latitude/longitude inputs validated (lat: -90 to 90, lon: -180 to 180)
- **User Input Sanitization**: All user-provided text inputs are validated and sanitized
- **Time/Duration Validation**: Reasonable range checks on all temporal inputs
- **File Path Validation**: Directory traversal prevention on all file operations
- **URL Validation**: SSRF attack prevention on network requests

### Network Security
- **HTTPS Only**: All network communication uses HTTPS
- **API Key Protection**: Secrets stored in secure configuration, never in code
- **Certificate Pinning**: Considered for production deployment

### Application Security
- **Secure Exception Handling**: Errors handled without exposing sensitive information
- **Memory Safety**: Proper cleanup and disposal of sensitive data
- **Thread Safety**: Explicit synchronization for shared mutable state (Mutex, coroutines)
- **Null Safety**: Zero tolerance for unsafe `!!` operators in production code

### Platform-Specific Security

**Android**:
- Follows Android security best practices
- ProGuard/R8 obfuscation enabled
- Secure storage for sensitive data

**iOS**:
- Keychain integration for sensitive data
- App Transport Security (ATS) compliance
- Thread-safe Kotlin/Native implementation

### Testing & Verification
- **902+ Unit Tests**: Comprehensive test coverage for security-critical code
- **iOS Safety Verification**: Automated script prevents deadlock patterns
- **Pre-commit Hooks**: Automated security checks before code commits
- **CI/CD Security**: GitHub Actions workflows validate security measures

## Security Development Lifecycle

All code changes must follow these security requirements:

1. **Code Review**: All PRs require security review
2. **Input Validation**: Validate all external inputs
3. **Exception Handling**: Proper error handling without information disclosure
4. **Testing**: Security-relevant code must have corresponding tests
5. **Documentation**: Security decisions must be documented

See [CLAUDE.md](CLAUDE.md#security-patterns) for detailed security development requirements.

## Known Security Considerations

### Location Data
- **Precision**: Location data precision is limited based on use case requirements
- **Retention**: Ephemeral design minimizes data retention risks
- **Permissions**: Users must explicitly grant location permissions

### Third-Party Dependencies
- Regular dependency updates via Renovate/Dependabot
- Security advisories monitored for all dependencies
- Gradle version catalogs for centralized dependency management

### API Keys and Secrets
- Never committed to version control
- Stored in BuildConfig or secure configuration
- Proper key rotation procedures implemented
- Firebase security rules enforce access control

## Security Tools

We use the following tools to maintain security:

- **Detekt**: Static code analysis for Kotlin
- **SwiftLint**: Static code analysis for Swift
- **GitHub Security Advisories**: Dependency vulnerability tracking
- **Pre-commit Hooks**: Automated security validation
- **iOS Safety Verification**: Custom deadlock prevention checks

## Acknowledgments

We appreciate the security research community's efforts to responsibly disclose vulnerabilities. Security researchers who report valid vulnerabilities will be acknowledged in our release notes (unless they prefer to remain anonymous).

---

**Last Updated**: October 27, 2025
**Contact**: security@worldwidewaves.com
