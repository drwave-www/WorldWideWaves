# Environment Setup

Complete setup guide for WorldWideWaves development environment across macOS, Linux, and Windows.

## Prerequisites

### All Platforms

| Tool | Minimum Version | Recommended | Purpose |
|------|----------------|-------------|---------|
| JDK | 17 | 17 (Temurin) | Kotlin compilation |
| Android Studio | 2024.1+ | Latest Stable | Android development |
| Git | 2.0+ | Latest | Version control |
| Node.js | 16+ | 18 LTS | Map generation scripts |

### macOS Only (for iOS Development)

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| Xcode | 15.0+ (16.0+ recommended) | iOS development |
| CocoaPods | 1.11+ | iOS dependencies (if used) |

### Optional Tools

| Tool | Purpose |
|------|---------|
| ripgrep | Code pattern search (required for `./scripts/verify-ios-safety.sh`) |
| Docker | Map generation (alternative to local Node.js) |
| gcloud CLI | Firebase Test Lab access |

## Hardware Requirements

### Minimum

- CPU: 4 cores
- RAM: 8 GB
- Disk: 20 GB free space
- Display: 1280x800

### Recommended

- CPU: 8+ cores (M1/M2 Mac or equivalent Intel/AMD)
- RAM: 16-32 GB
- Disk: 50 GB free space on SSD
- Display: 1920x1080 or higher

## macOS Setup

### 1. Install Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install JDK 17

```bash
brew install openjdk@17

# Link for system Java wrapper
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify installation
java -version
# Should show: openjdk version "17.x.x"
```

### 3. Install Android Studio

```bash
brew install --cask android-studio

# Or download from: https://developer.android.com/studio
```

**First launch setup:**

1. Choose "Standard" installation
2. Accept license agreements
3. Download Android SDK components
4. Set SDK location: `~/Library/Android/sdk`

### 4. Install Android SDK Components

```bash
# Install via sdkmanager
~/Library/Android/sdk/tools/bin/sdkmanager \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;35.0.0" \
  "emulator" \
  "system-images;android-30;google_apis;arm64-v8a"

# Accept licenses
~/Library/Android/sdk/tools/bin/sdkmanager --licenses
```

### 5. Install Xcode (iOS Development)

```bash
# Install from App Store or
xcode-select --install

# Verify installation
xcodebuild -version
# Should show: Xcode 16.x

# Accept license
sudo xcodebuild -license accept

# Install command-line tools
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

### 6. Install Node.js

```bash
brew install node@18

# Verify installation
node --version
npm --version

# ripgrep for iOS safety verification
brew install ripgrep
```

### 7. Install Optional Tools

```bash
# Docker
brew install --cask docker

# Google Cloud SDK
brew install --cask google-cloud-sdk

# Initialize gcloud
gcloud init
```

## Linux Setup (Ubuntu/Debian)

### 1. Install JDK 17

```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
```

### 2. Install Android Studio

```bash
# Download from: https://developer.android.com/studio
# Extract and run:
tar -xzf android-studio-*.tar.gz
cd android-studio/bin
./studio.sh
```

**First launch:** Follow macOS setup steps 3-4.

### 3. Install KVM (Hardware Acceleration)

```bash
# Check KVM support
sudo apt install cpu-checker
sudo kvm-ok

# Install KVM packages
sudo apt install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils

# Add user to KVM groups
sudo usermod -aG kvm $USER
sudo usermod -aG libvirt $USER

# Logout and login for changes to take effect
```

### 4. Install Node.js

```bash
# Using NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs

# Verify installation
node --version
npm --version

# ripgrep for iOS safety verification
sudo apt install ripgrep
```

### 5. Install Optional Tools

```bash
# Docker
sudo apt install docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Google Cloud SDK
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init
```

## Windows Setup

### 1. Install JDK 17

Download and install from: https://adoptium.net/temurin/releases/?version=17

**Set JAVA_HOME:**

```powershell
# PowerShell (Admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot", [System.EnvironmentVariableTarget]::Machine)

# Verify
java -version
```

### 2. Install Android Studio

Download from: https://developer.android.com/studio

Run installer and follow wizard. Install to: `C:\Program Files\Android\Android Studio`

### 3. Enable Hyper-V (Hardware Acceleration)

**Windows 10/11 Pro:**

```powershell
# PowerShell (Admin)
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V -All
```

**Windows 10/11 Home:**

- Install HAXM: https://github.com/intel/haxm/releases
- Or use Windows Hypervisor Platform (Settings > Apps > Optional Features)

### 4. Install Node.js

Download from: https://nodejs.org/en/download/

Choose LTS version installer.

### 5. Install Git

Download from: https://git-scm.com/download/win

Choose "Git Bash" option during installation.

### 6. Install Optional Tools

**Docker Desktop:**

- Download from: https://www.docker.com/products/docker-desktop/
- Requires WSL 2

**Google Cloud SDK:**

- Download from: https://cloud.google.com/sdk/docs/install#windows

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/mglcel/WorldWideWaves.git
cd WorldWideWaves
```

### 2. Configure Local Properties

Create `local.properties` in project root:

```properties
# Android SDK location
# macOS/Linux:
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
# Windows:
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk

# Firebase configuration (required for analytics/crashlytics)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PROJECT_NUMBER=123456789012
FIREBASE_MOBILE_SDK_APP_ID=1:123456789012:android:abcdef1234567890
FIREBASE_API_KEY=AIzaSyABCDEF1234567890abcdefGHIJKLMNOP

# Optional: Translation automation
# OPENAI_API_KEY=sk-...
```

**Note:** Never commit `local.properties` to version control.

### 3. Generate Firebase Configuration

```bash
# Auto-generate google-services.json from local.properties
./gradlew generateFirebaseConfig
```

### 4. Verify Build

```bash
# Build entire project
./gradlew build

# Expected output:
# BUILD SUCCESSFUL in Xs
```

### 5. Setup Git Hooks (Recommended)

```bash
./dev/setup-git-hooks.sh
```

Features:

- Automatic emulator launch for integration testing
- Translation updates (if `OPENAI_API_KEY` set)
- Pre-push integration tests

## Android Emulator Setup

### Create AVD (Android Virtual Device)

**Via Android Studio:**

1. Tools > Device Manager
2. Create Device > Pixel 3a
3. Select system image: API 30 (Android 11, arm64-v8a)
4. Configure:
   - RAM: 3-4 GB
   - Internal Storage: 8 GB
   - SD Card: 512 MB
5. Finish

**Via Command Line:**

```bash
# List available system images
sdkmanager --list | grep system-images

# Download system image
sdkmanager "system-images;android-30;google_apis;arm64-v8a"

# Create AVD
avdmanager create avd \
  -n Pixel_3a_API_30 \
  -k "system-images;android-30;google_apis;arm64-v8a" \
  -d "pixel_3a" \
  -c 512M

# Launch emulator
emulator -avd Pixel_3a_API_30 &
```

### Emulator Configuration

**Edit AVD config** (`~/.android/avd/Pixel_3a_API_30.avd/config.ini`):

```ini
# Increase RAM
hw.ramSize=4096

# Enable GPU acceleration
hw.gpu.enabled=yes
hw.gpu.mode=auto

# Enable keyboard input
hw.keyboard=yes
```

### Emulator Performance Tips

**macOS:**

```bash
# Use Apple Silicon native emulator
emulator -avd Pixel_3a_API_30 -gpu host
```

**Linux:**

```bash
# Ensure KVM acceleration
emulator -avd Pixel_3a_API_30 -qemu -enable-kvm -gpu host
```

**Windows:**

```bash
# Use Hyper-V acceleration
emulator -avd Pixel_3a_API_30 -gpu host
```

## iOS Simulator Setup (macOS Only)

### Install iOS Simulators

```bash
# List available runtimes
xcrun simctl list runtimes

# Install iOS 18 Simulator (if not installed)
xcodebuild -downloadPlatform iOS

# List available device types
xcrun simctl list devicetypes
```

### Create Simulator

```bash
# Create iPhone 15 simulator
xcrun simctl create "iPhone 15" \
  "com.apple.CoreSimulator.SimDeviceType.iPhone-15" \
  "com.apple.CoreSimulator.SimRuntime.iOS-18-0"

# Boot simulator
xcrun simctl boot "iPhone 15"

# Open Simulator app
open -a Simulator
```

### Simulator Configuration

**Location simulation:**

```bash
# Set location (San Francisco)
xcrun simctl location "iPhone 15" set 37.7749,-122.4194
```

## First Run Verification

### Android

```bash
# 1. Launch emulator
emulator -avd Pixel_3a_API_30 &

# 2. Wait for boot
adb wait-for-device

# 3. Build and install app
./gradlew :composeApp:installDebug

# 4. Launch app
adb shell am start -n com.worldwidewaves/.MainActivity

# 5. Grant location permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.worldwidewaves android.permission.ACCESS_COARSE_LOCATION

# 6. Verify app launches successfully
```

### iOS

```bash
# 1. Build iOS framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# 2. Open Xcode project
open iosApp/iosApp.xcodeproj

# 3. Select iPhone 15 Simulator

# 4. Build and run (Cmd+R)

# 5. Grant location permissions when prompted
```

### Run Tests

```bash
# Unit tests
./gradlew :shared:testDebugUnitTest

# Expected: All tests passing
# ✓ BUILD SUCCESSFUL
```

## IDE Configuration

### Android Studio

**Recommended settings:**

1. **Preferences > Build, Execution, Deployment > Compiler**
   - Build process heap size: 4096 MB
   - Command-line Options: `--parallel --max-workers=4`

2. **Preferences > Editor > Code Style > Kotlin**
   - Set from: Kotlin style guide

3. **Preferences > Version Control > Commit**
   - Enable "Analyze code" before commit
   - Enable "Check TODO" before commit

4. **Preferences > Plugins**
   - Install: Compose Multiplatform IDE Support

### Xcode (macOS)

**Recommended settings:**

1. **Preferences > Locations**
   - Command Line Tools: Select Xcode version

2. **Preferences > Behaviors > Build Succeeds**
   - Show Debug Navigator

3. **Preferences > Text Editing**
   - Enable "Line numbers"
   - Indent using: 4 spaces

## Troubleshooting

### macOS

**Issue: Android Studio won't launch**

```bash
# Increase VM heap
# Edit: /Applications/Android Studio.app/Contents/bin/studio.vmoptions
-Xmx4g
```

**Issue: Xcode command-line tools not found**

```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
sudo xcode-select --install
```

### Linux

**Issue: KVM permission denied**

```bash
sudo chmod 666 /dev/kvm
# Or add user to kvm group:
sudo usermod -aG kvm $USER
```

**Issue: Emulator fails to start**

```bash
# Install required libraries
sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
```

### Windows

**Issue: Hyper-V not available**

- Windows Home: Install HAXM instead
- Windows Pro: Enable in "Turn Windows features on or off"

**Issue: Android SDK not found**

```powershell
# Set ANDROID_HOME
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk", [System.EnvironmentVariableTarget]::Machine)
```

### All Platforms

**Issue: Gradle build fails with "Out of Memory"**

Edit `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxDirectMemorySize=8g
```

**Issue: Firebase config missing**

```bash
./gradlew generateFirebaseConfig
# Verify google-services.json created in composeApp/
```

**Issue: Dependencies not resolving**

```bash
./gradlew --refresh-dependencies
```

## Next Steps

- [Development Workflow](development.md)
- [Architecture Overview](architecture.md)
- [Contributing Guidelines](contributing.md)
- [CI/CD Pipeline](ci-cd.md)
