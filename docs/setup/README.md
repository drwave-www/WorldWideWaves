# Setup Guides

## Platform-Specific Setup

### [Linux Setup Guide](linux-setup.md)
Complete Linux development environment setup (Ubuntu, Fedora, Arch).
- Distribution-specific prerequisites
- Java JDK 17 installation
- Android Studio setup
- KVM hardware acceleration
- Android emulator configuration
- Common Linux-specific issues

## Firebase Configuration

### [Firebase Setup (General)](firebase-setup.md)
Basic Firebase project configuration and security rules.
- Firestore database setup
- Storage bucket configuration
- Security rules deployment

### [Firebase iOS Setup](firebase-ios-setup.md)
iOS-specific Firebase configuration and integration.
- GoogleService-Info.plist generation
- iOS project configuration
- CocoaPods dependencies

### [Firebase iOS Auto-Generation](firebase-ios-auto-generation.md)
Automated Firebase configuration generation for iOS.
- Script-based configuration
- CI/CD integration
- Multi-environment support

## iOS Resources

### [On-Demand Resources (ODR) Bundle](odr-bundle.md)
iOS map data packaging and on-demand resource configuration.
- Map data bundling for 40+ cities
- ODR tags and priority configuration
- Reducing initial app size

## Quick Start

1. **Platform setup**:
   - Linux: Start with [linux-setup.md](linux-setup.md)
   - macOS/Windows: See [../environment-setup.md](../environment-setup.md)
2. **Firebase config**: Continue with [firebase-setup.md](firebase-setup.md)
3. **iOS-specific** (macOS only): Configure with [firebase-ios-setup.md](firebase-ios-setup.md)
4. **Map data** (iOS): Package with [odr-bundle.md](odr-bundle.md)
