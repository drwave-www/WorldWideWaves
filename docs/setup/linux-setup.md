# Linux Setup Guide for WorldWideWaves

Complete setup guide for developing WorldWideWaves on Linux (Ubuntu/Debian, Fedora, and Arch-based distributions).

> **Note**: This guide focuses on Linux-specific setup. For general prerequisites and project structure, see [Environment Setup](../environment-setup.md).

## Table of Contents

- [Distribution-Specific Prerequisites](#distribution-specific-prerequisites)
- [Java JDK 17 Installation](#java-jdk-17-installation)
- [Android Studio Installation](#android-studio-installation)
- [Android SDK Setup](#android-sdk-setup)
- [KVM Hardware Acceleration](#kvm-hardware-acceleration)
- [Android Emulator Setup](#android-emulator-setup)
- [Development Tools](#development-tools)
- [Project Configuration](#project-configuration)
- [Verification](#verification)
- [Common Issues](#common-issues)

## Distribution-Specific Prerequisites

### Ubuntu/Debian (20.04+, Debian 11+)

```bash
# Update package lists
sudo apt update

# Install build essentials and dependencies
sudo apt install -y \
  build-essential \
  git \
  curl \
  wget \
  unzip \
  cpu-checker \
  libncurses5 \
  lib32z1 \
  libc6:i386 \
  libstdc++6:i386 \
  libncurses5:i386 \
  libbz2-1.0:i386
```

### Fedora (35+)

```bash
# Update package cache
sudo dnf check-update

# Install build tools and dependencies
sudo dnf install -y \
  @development-tools \
  git \
  curl \
  wget \
  unzip \
  ncurses-libs.i686 \
  zlib.i686 \
  glibc.i686 \
  libstdc++.i686
```

### Arch Linux / Manjaro

```bash
# Update system
sudo pacman -Syu

# Install base development tools
sudo pacman -S --needed \
  base-devel \
  git \
  curl \
  wget \
  unzip

# Enable multilib repository (for 32-bit libraries)
# Edit /etc/pacman.conf, uncomment:
# [multilib]
# Include = /etc/pacman.d/mirrorlist

# Then install 32-bit libraries
sudo pacman -S --needed \
  lib32-gcc-libs \
  lib32-zlib \
  lib32-ncurses
```

## Java JDK 17 Installation

### Ubuntu/Debian

```bash
# Install OpenJDK 17
sudo apt install -y openjdk-17-jdk

# Verify installation
java -version
# Expected: openjdk version "17.0.x"

# Set JAVA_HOME (add to ~/.bashrc or ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Reload shell config
source ~/.bashrc
```

### Fedora

```bash
# Install OpenJDK 17
sudo dnf install -y java-17-openjdk java-17-openjdk-devel

# Verify installation
java -version

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Reload shell config
source ~/.bashrc
```

### Arch Linux

```bash
# Install JDK 17
sudo pacman -S jdk17-openjdk

# Set as default Java version
sudo archlinux-java set java-17-openjdk

# Verify installation
java -version

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Reload shell config
source ~/.bashrc
```

### Alternative: Manual Installation (All Distributions)

```bash
# Download Eclipse Temurin 17
wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9+9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz

# Extract to /opt
sudo tar -xzf OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz -C /opt

# Create symlink
sudo ln -s /opt/jdk-17.0.9+9 /opt/jdk-17

# Set JAVA_HOME (add to ~/.bashrc)
export JAVA_HOME=/opt/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Verify
java -version
```

## Android Studio Installation

### Ubuntu/Debian (Recommended: Snap)

```bash
# Install via Snap (easiest, auto-updates)
sudo snap install android-studio --classic

# Launch Android Studio
android-studio
```

### Ubuntu/Debian (Manual Installation)

```bash
# Download Android Studio (replace URL with latest version)
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.1.1.12/android-studio-2024.1.1.12-linux.tar.gz

# Extract to /opt
sudo tar -xzf android-studio-*.tar.gz -C /opt

# Create desktop entry
cat << 'EOF' | sudo tee /usr/share/applications/android-studio.desktop
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Icon=/opt/android-studio/bin/studio.svg
Exec="/opt/android-studio/bin/studio.sh" %f
Comment=Android Studio
Categories=Development;IDE;
Terminal=false
EOF

# Launch Android Studio
/opt/android-studio/bin/studio.sh
```

### Fedora

```bash
# Install via Flatpak (recommended)
flatpak install flathub com.google.AndroidStudio

# Or download and extract manually (same as Debian instructions)
```

### Arch Linux

```bash
# Install from AUR
yay -S android-studio

# Or use official binary
# Same manual installation as Ubuntu/Debian
```

### First Launch Setup

1. **Welcome Screen**: Choose "Standard" installation
2. **UI Theme**: Select Light/Dark theme
3. **SDK Components**: Accept default selections
4. **License Agreements**: Accept all licenses
5. **Download Components**: Wait for SDK download (~2-3 GB)
6. **SDK Location**: Default is `~/Android/Sdk`

## Android SDK Setup

### Configure SDK Location

Android Studio installs the SDK to `~/Android/Sdk` by default.

**Verify SDK installation:**

```bash
ls ~/Android/Sdk
# Expected directories: build-tools, emulator, platform-tools, platforms, system-images
```

**Add to PATH (add to ~/.bashrc or ~/.zshrc):**

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Reload shell
source ~/.bashrc
```

### Install Required SDK Components

**Via Android Studio:**

1. Tools → SDK Manager
2. **SDK Platforms** tab:
   - Android 14.0 (API 36)
   - Android 11.0 (API 30)
3. **SDK Tools** tab:
   - Android SDK Build-Tools 35.0.0
   - Android Emulator
   - Android SDK Platform-Tools
   - Intel/AMD x86 Emulator Accelerator (if not using KVM)

**Via Command Line:**

```bash
# Navigate to SDK tools
cd ~/Android/Sdk/cmdline-tools/latest/bin

# Install required components
./sdkmanager "platform-tools" \
             "platforms;android-36" \
             "platforms;android-30" \
             "build-tools;35.0.0" \
             "emulator" \
             "system-images;android-30;google_apis;x86_64"

# Accept licenses
./sdkmanager --licenses
```

### Create local.properties

Create `local.properties` in the WorldWideWaves project root:

```bash
cd ~/path/to/WorldWideWaves

cat << EOF > local.properties
# Android SDK location (Linux default path)
sdk.dir=/home/$USER/Android/Sdk

# Firebase configuration (replace with your values)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PROJECT_NUMBER=123456789012
FIREBASE_MOBILE_SDK_APP_ID=1:123456789012:android:abcdef1234567890
FIREBASE_API_KEY=AIzaSyABCDEF1234567890
EOF
```

**Important**: Never commit `local.properties` to version control (already in `.gitignore`).

## KVM Hardware Acceleration

KVM (Kernel-based Virtual Machine) provides hardware acceleration for Android emulators, dramatically improving performance.

### Check KVM Availability

```bash
# Check if CPU supports virtualization
egrep -c '(vmx|svm)' /proc/cpuinfo
# Output > 0 means virtualization is supported

# Install cpu-checker (Ubuntu/Debian)
sudo apt install cpu-checker

# Check KVM support
kvm-ok
# Expected: "KVM acceleration can be used"
```

### Install KVM (Ubuntu/Debian)

```bash
# Install KVM packages
sudo apt install -y \
  qemu-kvm \
  libvirt-daemon-system \
  libvirt-clients \
  bridge-utils

# Add current user to kvm and libvirt groups
sudo usermod -aG kvm $USER
sudo usermod -aG libvirt $USER

# Verify group membership
groups $USER
# Should include: kvm libvirt

# Log out and log back in for group changes to take effect
```

### Install KVM (Fedora)

```bash
# Install KVM packages
sudo dnf install -y \
  @virtualization \
  qemu-kvm \
  libvirt \
  virt-manager

# Add user to libvirt group
sudo usermod -aG libvirt $USER

# Start libvirtd service
sudo systemctl start libvirtd
sudo systemctl enable libvirtd

# Log out and log back in
```

### Install KVM (Arch Linux)

```bash
# Install KVM packages
sudo pacman -S \
  qemu \
  libvirt \
  virt-manager \
  ebtables \
  dnsmasq

# Add user to kvm and libvirt groups
sudo usermod -aG kvm,libvirt $USER

# Enable and start libvirtd
sudo systemctl enable libvirtd
sudo systemctl start libvirtd

# Log out and log back in
```

### Verify KVM Installation

```bash
# Check /dev/kvm exists and has correct permissions
ls -l /dev/kvm
# Expected: crw-rw---- 1 root kvm ... /dev/kvm

# Test KVM access (should return without errors)
virsh list --all
```

### Fix KVM Permission Issues

If you get "Permission denied" when accessing `/dev/kvm`:

```bash
# Temporary fix (until reboot)
sudo chmod 666 /dev/kvm

# Permanent fix: Create udev rule
sudo tee /etc/udev/rules.d/99-kvm.rules << EOF
KERNEL=="kvm", GROUP="kvm", MODE="0666"
EOF

# Reload udev rules
sudo udevadm control --reload-rules
sudo udevadm trigger
```

## Android Emulator Setup

### Create AVD via Android Studio

1. **Open Device Manager**: Tools → Device Manager
2. **Create Device**: Click "Create Device"
3. **Choose Device**: Select "Pixel 5" or "Pixel 3a"
4. **Select System Image**:
   - Release Name: R (API 30)
   - ABI: x86_64 (with Google APIs)
   - Download if not already installed
5. **Configure AVD**:
   - AVD Name: `Pixel_5_API_30`
   - Startup orientation: Portrait
   - Graphics: Automatic (uses KVM)
   - RAM: 4096 MB
   - Internal Storage: 8192 MB
   - SD Card: 512 MB
6. **Finish**: Click "Finish"

### Create AVD via Command Line

```bash
# List available system images
sdkmanager --list | grep system-images

# Download x86_64 system image (faster with KVM)
sdkmanager "system-images;android-30;google_apis;x86_64"

# Create AVD
avdmanager create avd \
  --name Pixel_5_API_30 \
  --package "system-images;android-30;google_apis;x86_64" \
  --device "pixel_5" \
  --sdcard 512M

# List created AVDs
avdmanager list avd
```

### Launch Emulator with KVM

```bash
# Launch emulator with KVM acceleration
emulator -avd Pixel_5_API_30 -qemu -enable-kvm &

# With GPU acceleration
emulator -avd Pixel_5_API_30 -gpu host -qemu -enable-kvm &

# Check emulator is running
adb devices
# Expected: emulator-5554   device
```

### Emulator Configuration Tweaks

Edit AVD config file (`~/.android/avd/Pixel_5_API_30.avd/config.ini`):

```ini
# Increase RAM for better performance
hw.ramSize=4096

# Enable GPU acceleration
hw.gpu.enabled=yes
hw.gpu.mode=auto

# Enable keyboard input
hw.keyboard=yes

# Reduce boot time
fastboot.chosenSnapshotFile=
fastboot.forceChosenSnapshotBoot=no
fastboot.forceColdBoot=no
fastboot.forceFastBoot=yes
```

### Performance Tips

**Improve startup time:**

```bash
# Create initial snapshot
emulator -avd Pixel_5_API_30 -gpu host -qemu -enable-kvm -no-boot-anim
# Wait for boot, then close emulator (snapshot auto-saved)

# Future launches use snapshot (instant boot)
emulator -avd Pixel_5_API_30 -gpu host -qemu -enable-kvm
```

**Monitor emulator performance:**

```bash
# Show extended controls
emulator -avd Pixel_5_API_30 -show-kernel

# Verbose output for debugging
emulator -avd Pixel_5_API_30 -verbose -qemu -enable-kvm
```

## Development Tools

### Install ripgrep

**Required for iOS safety verification script** (`./scripts/verify-ios-safety.sh`).

**Ubuntu/Debian:**

```bash
sudo apt install ripgrep
```

**Fedora:**

```bash
sudo dnf install ripgrep
```

**Arch Linux:**

```bash
sudo pacman -S ripgrep
```

**Verify installation:**

```bash
rg --version
# Expected: ripgrep 13.x or later
```

### Install Node.js (for map generation)

**Ubuntu/Debian (via NodeSource):**

```bash
# Add NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -

# Install Node.js
sudo apt install -y nodejs

# Verify installation
node --version  # Expected: v18.x
npm --version   # Expected: 9.x
```

**Fedora:**

```bash
# Install Node.js
sudo dnf install nodejs npm

# Or use official NodeSource
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install nodejs
```

**Arch Linux:**

```bash
# Install Node.js
sudo pacman -S nodejs npm
```

### Install Docker (Optional)

**Ubuntu/Debian:**

```bash
# Install Docker
sudo apt install -y docker.io

# Start and enable Docker service
sudo systemctl enable --now docker

# Add user to docker group (avoid sudo)
sudo usermod -aG docker $USER

# Log out and log back in for group changes
```

**Fedora:**

```bash
# Install Docker
sudo dnf install docker

# Start and enable Docker
sudo systemctl enable --now docker

# Add user to docker group
sudo usermod -aG docker $USER
```

**Arch Linux:**

```bash
# Install Docker
sudo pacman -S docker

# Enable and start Docker
sudo systemctl enable --now docker

# Add user to docker group
sudo usermod -aG docker $USER
```

**Verify Docker installation:**

```bash
docker --version
docker run hello-world
```

### Install Git (if not already installed)

**All distributions:**

```bash
# Ubuntu/Debian
sudo apt install git

# Fedora
sudo dnf install git

# Arch Linux
sudo pacman -S git
```

**Configure Git:**

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

## Project Configuration

### Clone WorldWideWaves Repository

```bash
# Clone repository
git clone https://github.com/drwave-www/WorldWideWaves.git
cd WorldWideWaves
```

### Configure local.properties

Create `local.properties` (if not already created):

```bash
cat << EOF > local.properties
# Android SDK location (adjust path if needed)
sdk.dir=/home/$USER/Android/Sdk

# Firebase configuration (replace with your values)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PROJECT_NUMBER=123456789012
FIREBASE_MOBILE_SDK_APP_ID=1:123456789012:android:abcdef1234567890
FIREBASE_API_KEY=AIzaSyABCDEF1234567890
EOF
```

### Generate Firebase Configuration

```bash
# Auto-generate google-services.json from local.properties
./gradlew generateFirebaseConfig

# Verify google-services.json created
ls composeApp/google-services.json
```

### Setup Git Hooks (Recommended)

```bash
# Install custom git hooks
./dev/setup-git-hooks.sh

# Verify hooks installed
ls -la .git/hooks/
```

**Features:**

- Pre-commit: License header validation
- Pre-push: Run unit tests before pushing
- Post-checkout: Automatic dependency updates

## Verification

### Build Project

```bash
# Clean and build entire project
./gradlew clean build

# Expected output:
# BUILD SUCCESSFUL in XXs
```

### Run Unit Tests

```bash
# Run all unit tests
./gradlew :shared:testDebugUnitTest

# Expected: All tests passing
# BUILD SUCCESSFUL
```

### Launch Android App

```bash
# 1. Start emulator (if not running)
emulator -avd Pixel_5_API_30 -gpu host -qemu -enable-kvm &

# 2. Wait for device to boot
adb wait-for-device

# 3. Build and install app
./gradlew :composeApp:installDebug

# 4. Launch app
adb shell am start -n com.worldwidewaves/.MainActivity

# 5. Grant location permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.worldwidewaves android.permission.ACCESS_COARSE_LOCATION
```

### Verify iOS Safety (even on Linux)

```bash
# Run iOS safety verification script
./scripts/verify-ios-safety.sh

# Expected: 0 violations found
```

### Run Lint Checks

```bash
# Kotlin linting
./gradlew ktlintCheck

# Detekt static analysis
./gradlew detekt

# Fix auto-fixable issues
./gradlew ktlintFormat
```

## Common Issues

### 1. KVM Permission Denied

**Symptom:**

```
/dev/kvm: Permission denied
```

**Solution:**

```bash
# Add user to kvm group
sudo usermod -aG kvm $USER

# Or set permissions on /dev/kvm
sudo chmod 666 /dev/kvm

# Create udev rule for permanent fix
sudo tee /etc/udev/rules.d/99-kvm.rules << EOF
KERNEL=="kvm", GROUP="kvm", MODE="0666"
EOF

# Reload udev
sudo udevadm control --reload-rules
sudo udevadm trigger

# Log out and log back in
```

### 2. Missing 32-bit Libraries

**Symptom:**

```
error while loading shared libraries: libstdc++.so.6: cannot open shared object file
```

**Solution (Ubuntu/Debian):**

```bash
sudo apt install -y \
  libc6:i386 \
  libncurses5:i386 \
  libstdc++6:i386 \
  lib32z1 \
  libbz2-1.0:i386
```

**Solution (Fedora):**

```bash
sudo dnf install -y \
  glibc.i686 \
  ncurses-libs.i686 \
  libstdc++.i686 \
  zlib.i686
```

**Solution (Arch Linux):**

```bash
# Enable multilib in /etc/pacman.conf
sudo pacman -S \
  lib32-gcc-libs \
  lib32-zlib \
  lib32-ncurses
```

### 3. Emulator Crashes on Launch

**Symptom:**

```
Emulator: ERROR: x86 emulation currently requires hardware acceleration!
```

**Solution:**

```bash
# Verify KVM is installed and enabled
kvm-ok

# If KVM not available, use ARM emulator (slower)
sdkmanager "system-images;android-30;google_apis;arm64-v8a"

avdmanager create avd \
  --name Pixel_5_API_30_ARM \
  --package "system-images;android-30;google_apis;arm64-v8a" \
  --device "pixel_5"

emulator -avd Pixel_5_API_30_ARM
```

### 4. Android SDK Not Found

**Symptom:**

```
SDK location not found. Define location with sdk.dir in local.properties
```

**Solution:**

```bash
# Verify SDK directory exists
ls ~/Android/Sdk

# Create/update local.properties
echo "sdk.dir=/home/$USER/Android/Sdk" > local.properties

# Set ANDROID_HOME environment variable
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
source ~/.bashrc
```

### 5. Gradle Build Out of Memory

**Symptom:**

```
Expiring Daemon because JVM heap space is exhausted
```

**Solution:**

```bash
# Edit gradle.properties in project root
echo "org.gradle.jvmargs=-Xmx8g -XX:MaxDirectMemorySize=8g" >> gradle.properties

# Restart Gradle daemon
./gradlew --stop
./gradlew build
```

### 6. Emulator Too Slow

**Symptom:**
Emulator takes >5 minutes to boot, app laggy.

**Solution:**

```bash
# Ensure KVM is enabled
emulator -avd Pixel_5_API_30 -qemu -enable-kvm -gpu host

# Use x86_64 image (not ARM)
# Increase RAM in AVD config
echo "hw.ramSize=4096" >> ~/.android/avd/Pixel_5_API_30.avd/config.ini

# Enable quick boot snapshot
# Launch once, let it boot fully, then close
# Next launch will use snapshot
```

### 7. adb: device offline

**Symptom:**

```
adb devices
List of devices attached
emulator-5554   offline
```

**Solution:**

```bash
# Restart ADB server
adb kill-server
adb start-server

# Or restart emulator
adb reboot

# Cold boot emulator (no snapshot)
emulator -avd Pixel_5_API_30 -no-snapshot
```

### 8. Firebase Configuration Missing

**Symptom:**

```
File google-services.json is missing
```

**Solution:**

```bash
# Generate Firebase config from local.properties
./gradlew generateFirebaseConfig

# Verify local.properties contains Firebase keys
grep FIREBASE local.properties

# If missing, add Firebase configuration:
cat << EOF >> local.properties
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PROJECT_NUMBER=123456789012
FIREBASE_MOBILE_SDK_APP_ID=1:123456789012:android:abcdef1234567890
FIREBASE_API_KEY=AIzaSyABCDEF1234567890
EOF
```

### 9. Cannot Find avdmanager or sdkmanager

**Symptom:**

```
avdmanager: command not found
```

**Solution:**

```bash
# Add Android SDK tools to PATH
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.bashrc

# Reload shell
source ~/.bashrc

# Verify
which avdmanager
which sdkmanager
```

### 10. Graphics/Rendering Issues

**Symptom:**
Black screen, corrupted UI, or graphics glitches in emulator.

**Solution:**

```bash
# Try different GPU modes
emulator -avd Pixel_5_API_30 -gpu swiftshader_indirect  # Software rendering
emulator -avd Pixel_5_API_30 -gpu host                  # Host GPU
emulator -avd Pixel_5_API_30 -gpu auto                  # Auto-detect

# Update graphics drivers (NVIDIA/AMD)
# Ubuntu/Debian:
sudo ubuntu-drivers autoinstall

# Fedora:
sudo dnf update

# Arch:
sudo pacman -Syu
```

## Next Steps

### Recommended Reading

- **[Environment Setup](../environment-setup.md)** - General setup for all platforms
- **[Development Workflow](../development.md)** - Daily development tasks
- **[Architecture Overview](../architecture.md)** - System design and patterns
- **[Contributing Guidelines](../contributing.md)** - Code standards and review process

### Development Commands

```bash
# Build Android app
./gradlew :composeApp:assembleDebug

# Run unit tests
./gradlew :shared:testDebugUnitTest

# Run instrumented tests (emulator must be running)
./gradlew :composeApp:connectedDebugAndroidTest

# Run all quality checks
./gradlew :shared:testDebugUnitTest ktlintCheck detekt

# Format code
./gradlew ktlintFormat
```

### Getting Help

- **Documentation**: Check `docs/` directory
- **Issues**: [GitHub Issues](https://github.com/drwave-www/WorldWideWaves/issues)
- **CI/CD**: [GitHub Actions](https://github.com/drwave-www/WorldWideWaves/actions)

---

**Tested On**: Ubuntu 22.04 LTS, Fedora 39, Arch Linux
