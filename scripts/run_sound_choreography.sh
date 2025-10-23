#!/bin/bash

# WorldWideWaves Sound Choreography Runner
# This script launches the real audio crowd simulation for testing sound choreography
# Works from any directory and auto-detects Android SDK, emulators, and devices
#
# Usage:
#   ./run_sound_choreography.sh              # Default behavior (prompt for automated test)
#   ./run_sound_choreography.sh --play       # Skip prompt and run automated test
#   ./run_sound_choreography.sh --open       # Only open emulator with activity (no automated test)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[SOUND CHOREOGRAPHY]${NC} $1"
}

# Function to find project root directory
find_project_root() {
    local current_dir="$PWD"
    while [[ "$current_dir" != "/" ]]; do
        if [[ -f "$current_dir/gradlew" && -f "$current_dir/settings.gradle.kts" && -d "$current_dir/composeApp" ]]; then
            echo "$current_dir"
            return 0
        fi
        current_dir="$(dirname "$current_dir")"
    done
    return 1
}

# Function to detect Android SDK
detect_android_sdk() {
    if [[ -n "$ANDROID_HOME" ]] && [[ -d "$ANDROID_HOME" ]]; then
        echo "$ANDROID_HOME"
        return 0
    fi

    if [[ -n "$ANDROID_SDK_ROOT" ]] && [[ -d "$ANDROID_SDK_ROOT" ]]; then
        echo "$ANDROID_SDK_ROOT"
        return 0
    fi

    # Common locations
    local common_locations=(
        "$HOME/Library/Android/sdk"
        "$HOME/Android/Sdk"
        "/opt/android-sdk"
        "/usr/local/android-sdk"
    )

    for location in "${common_locations[@]}"; do
        if [[ -d "$location" ]]; then
            echo "$location"
            return 0
        fi
    done

    return 1
}

# Function to find available Android devices/emulators
find_android_devices() {
    local adb_path="$1"

    if [[ ! -x "$adb_path" ]]; then
        return 1
    fi

    # Get list of devices
    local devices
    devices=$("$adb_path" devices | grep -E "device$|emulator" | cut -f1)

    if [[ -z "$devices" ]]; then
        return 1
    fi

    echo "$devices"
    return 0
}

# Function to check if device has audio support
check_audio_support() {
    local adb_path="$1"
    local device_serial="$2"

    # Check multiple audio indicators
    local audio_check1 audio_check2 audio_check3
    audio_check1=$("$adb_path" -s "$device_serial" shell getprop ro.config.vc_call_vol_steps 2>/dev/null || echo "")
    audio_check2=$("$adb_path" -s "$device_serial" shell getprop ro.audio.hifi 2>/dev/null || echo "")
    audio_check3=$("$adb_path" -s "$device_serial" shell ls /system/etc/audio_policy* 2>/dev/null | head -n1 || echo "")

    if [[ -n "$audio_check1" ]] || [[ -n "$audio_check2" ]] || [[ -n "$audio_check3" ]]; then
        return 0  # Has audio config
    else
        return 1  # No audio config found
    fi
}

# Function to ensure audio is enabled on emulator
enable_emulator_audio() {
    local adb_path="$1"
    local device_serial="$2"

    print_status "Ensuring audio is enabled on $device_serial..."

    # Set audio properties
    "$adb_path" -s "$device_serial" shell setprop audio.offload.disable true 2>/dev/null || true
    "$adb_path" -s "$device_serial" shell setprop ro.config.media_vol_steps 15 2>/dev/null || true
    "$adb_path" -s "$device_serial" shell setprop ro.config.vc_call_vol_steps 7 2>/dev/null || true

    # Wait a moment for properties to take effect
    sleep 1

    # Test audio capability by checking AudioManager
    local audio_test
    audio_test=$("$adb_path" -s "$device_serial" shell dumpsys audio | grep "AudioManager" | head -n1 || echo "")

    if [[ -n "$audio_test" ]]; then
        print_status "Audio system detected and configured on $device_serial"
        return 0
    else
        print_warning "Audio system configuration uncertain on $device_serial"
        return 1
    fi
}

# Function to start emulator with audio
start_emulator_with_audio() {
    local android_sdk="$1"
    local emulator_path="$android_sdk/emulator/emulator"
    local avd_name="$2"
    local show_window="$3"  # "true" for visible window, "false" for headless

    if [[ ! -x "$emulator_path" ]]; then
        return 1
    fi

    if [[ "$show_window" == "true" ]]; then
        print_status "Starting emulator '$avd_name' with visible window and audio support..."
        # Start emulator with visible window and audio
        "$emulator_path" -avd "$avd_name" -audio default -no-snapshot-load -gpu swiftshader_indirect &
    else
        print_status "Starting emulator '$avd_name' in headless mode with audio support..."
        # Start emulator headless with audio
        "$emulator_path" -avd "$avd_name" -audio default -no-snapshot-load -no-window &
    fi
    local emulator_pid=$!

    print_status "Emulator started (PID: $emulator_pid). Waiting for it to boot..."

    # Wait for emulator to be ready (up to 3 minutes)
    local timeout=180
    local counter=0
    while [[ $counter -lt $timeout ]]; do
        if "$android_sdk/platform-tools/adb" devices | grep -q "emulator.*device"; then
            print_status "Emulator is ready!"
            return 0
        fi
        sleep 2
        counter=$((counter + 2))
        if [[ $((counter % 20)) -eq 0 ]]; then
            print_status "Still waiting for emulator... ($counter/${timeout}s)"
        fi
    done

    print_error "Emulator failed to start within $timeout seconds"
    return 1
}

# Function to list available AVDs
list_avds() {
    local android_sdk="$1"
    local emulator_path="$android_sdk/emulator/emulator"

    if [[ ! -x "$emulator_path" ]]; then
        return 1
    fi

    "$emulator_path" -list-avds
}

# Function to create a new AVD for audio testing
create_audio_avd() {
    local android_sdk="$1"
    local avdmanager_path="$android_sdk/cmdline-tools/latest/bin/avdmanager"
    local sdkmanager_path="$android_sdk/cmdline-tools/latest/bin/sdkmanager"

    # Check if command line tools exist
    if [[ ! -x "$avdmanager_path" ]] || [[ ! -x "$sdkmanager_path" ]]; then
        print_error "Android command line tools not found. Please install them via Android Studio."
        return 1
    fi

    local avd_name="WorldWideWaves_Audio_Test"
    print_status "Creating new AVD: $avd_name"

    # Check if system image is available
    local system_image="system-images;android-34;google_apis_playstore;x86_64"
    print_status "Ensuring system image is installed: $system_image"

    if ! "$sdkmanager_path" --list | grep -q "$system_image"; then
        print_status "Installing system image: $system_image"
        yes | "$sdkmanager_path" "$system_image" || {
            print_error "Failed to install system image"
            return 1
        }
    fi

    # Create the AVD
    print_status "Creating AVD with audio support..."
    echo "no" | "$avdmanager_path" create avd \
        -n "$avd_name" \
        -k "$system_image" \
        -d "pixel_8_pro" \
        --force || {
        print_error "Failed to create AVD"
        return 1
    }

    # Configure AVD for audio
    local avd_config="$HOME/.android/avd/${avd_name}.avd/config.ini"
    if [[ -f "$avd_config" ]]; then
        print_status "Configuring AVD for audio support..."
        {
            echo "hw.audioOutput=yes"
            echo "hw.audioInput=yes"
            echo "hw.audio.output=yes"
            echo "hw.audio.input=yes"
        } >> "$avd_config"
    fi

    print_status "AVD '$avd_name' created successfully!"
    echo "$avd_name"
    return 0
}

# Parse command line arguments
parse_arguments() {
    RUN_MODE="default"  # default, play, open

    while [[ $# -gt 0 ]]; do
        case $1 in
            --play)
                RUN_MODE="play"
                shift
                ;;
            --open)
                RUN_MODE="open"
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  (no options)  Default behavior - prompt user to run automated test"
                echo "  --play        Skip prompt and automatically run the automated test"
                echo "  --open        Only open emulator with activity (no automated test)"
                echo "  --help, -h    Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
}

# Main script
main() {
    # Parse command line arguments first
    parse_arguments "$@"

    print_header "WorldWideWaves Sound Choreography Test Runner"
    echo ""

    # Find project root
    print_status "Detecting project root..."
    if ! PROJECT_ROOT=$(find_project_root); then
        print_error "Could not find WorldWideWaves project root. Please run this script from within the project directory."
        exit 1
    fi
    print_status "Project root: $PROJECT_ROOT"

    # Change to project root
    cd "$PROJECT_ROOT"

    # Detect Android SDK
    print_status "Detecting Android SDK..."
    if ! ANDROID_SDK=$(detect_android_sdk); then
        print_error "Could not find Android SDK. Please set ANDROID_HOME or ANDROID_SDK_ROOT environment variable."
        exit 1
    fi
    print_status "Android SDK: $ANDROID_SDK"

    # Check adb
    ADB_PATH="$ANDROID_SDK/platform-tools/adb"
    if [[ ! -x "$ADB_PATH" ]]; then
        print_error "ADB not found at $ADB_PATH"
        exit 1
    fi

    # Check for existing devices
    print_status "Checking for connected Android devices/emulators..."
    DEVICES=""
    if DEVICES=$(find_android_devices "$ADB_PATH") && [[ -n "$DEVICES" ]]; then
        print_status "Found connected devices:"

        # Find best device with audio support
        BEST_DEVICE=""
        while read -r device; do
            if check_audio_support "$ADB_PATH" "$device"; then
                print_status "  ✓ $device (audio support detected)"
                if [[ -z "$BEST_DEVICE" ]]; then
                    BEST_DEVICE="$device"
                fi
            else
                print_warning "  ⚠ $device (audio support unknown)"
            fi
        done <<< "$DEVICES"

        # Use best device found, or first device as fallback
        if [[ -n "$BEST_DEVICE" ]]; then
            SELECTED_DEVICE="$BEST_DEVICE"
        else
            SELECTED_DEVICE=$(echo "$DEVICES" | head -n1)
        fi

        print_status "Using device: $SELECTED_DEVICE"

        # Ensure audio is properly configured (but don't fail if uncertain)
        enable_emulator_audio "$ADB_PATH" "$SELECTED_DEVICE" || print_warning "Audio configuration uncertain, but continuing..."
    else
        print_warning "No connected devices found. Checking for available emulators..."

        # List available AVDs
        AVDS=""
        if ! AVDS=$(list_avds "$ANDROID_SDK") || [[ -z "$AVDS" ]]; then
            print_warning "No existing AVDs found. Creating a new one for audio testing..."

            # Create a new AVD
            if NEW_AVD=$(create_audio_avd "$ANDROID_SDK"); then
                print_status "Successfully created AVD: $NEW_AVD"
                PREFERRED_AVD="$NEW_AVD"
            else
                print_error "Failed to create new AVD. Please create one manually via Android Studio."
                exit 1
            fi
        else
            print_status "Available AVDs:"
            while read -r avd; do
                print_status "  - $avd"
            done <<< "$AVDS"

            # Try to start a suitable AVD
            PREFERRED_AVD=$(echo "$AVDS" | grep -E "(Pixel|API_3[0-9]|WorldWideWaves)" | head -n1)
            if [[ -z "$PREFERRED_AVD" ]]; then
                PREFERRED_AVD=$(echo "$AVDS" | head -n1)
            fi
        fi

        if [[ -n "$PREFERRED_AVD" ]]; then
            print_status "Starting emulator: $PREFERRED_AVD"
            # Determine if we should show window based on run mode
            local show_window="false"
            if [[ "$RUN_MODE" == "open" ]]; then
                show_window="true"
            fi

            if start_emulator_with_audio "$ANDROID_SDK" "$PREFERRED_AVD" "$show_window"; then
                # Find the new emulator device serial
                sleep 2
                SELECTED_DEVICE=$("$ADB_PATH" devices | grep "emulator" | head -n1 | cut -f1)
                print_status "Emulator ready: $SELECTED_DEVICE"

                # Ensure audio is properly configured on new emulator (but don't fail if uncertain)
                enable_emulator_audio "$ADB_PATH" "$SELECTED_DEVICE" || print_warning "Audio configuration uncertain, but continuing..."
            else
                print_error "Failed to start emulator"
                exit 1
            fi
        else
            print_error "No suitable AVD found and unable to create one"
            exit 1
        fi
    fi

    print_status "Building the application..."
    if ! ./gradlew :composeApp:assembleDebug --no-daemon; then
        print_error "Build failed"
        exit 1
    fi

    print_status "Installing APK on device $SELECTED_DEVICE..."
    if ! "$ADB_PATH" -s "$SELECTED_DEVICE" install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk; then
        print_error "Installation failed"
        exit 1
    fi

    print_header "🎵 Launching Sound Choreography Test Activity"
    print_status "Starting AudioTestActivity on device $SELECTED_DEVICE..."
    if ! "$ADB_PATH" -s "$SELECTED_DEVICE" shell am start -n com.worldwidewaves/.debug.AudioTestActivity; then
        print_error "Failed to launch AudioTestActivity"
        exit 1
    fi

    echo ""
    print_header "🎉 SUCCESS!"
    print_status "Sound Choreography Test is now running on device: $SELECTED_DEVICE"
    echo ""
    print_status "Available test options in the app:"
    print_status "  1. 'Play Test Note' - Single A4 tone test"
    print_status "  2. 'Play MIDI Sequence' - Sequential note playback"
    print_status "  3. 'Play Crowd Simulation' - Basic crowd demo"
    print_status "  4. '🌊 Play Wave Progression' - FULL SYMPHONY with realistic wave progression"
    echo ""
    print_header "🎼 Recommended: Tap '🌊 Play Wave Progression' to hear the complete symphony simulation!"
    print_warning "Make sure your device/emulator volume is turned up to hear the audio!"

    # Handle automated test based on run mode
    echo ""
    case "$RUN_MODE" in
        "play")
            print_status "Running unit test sound choreography simulation (--play mode)..."
            if ./gradlew :shared:testDebugUnitTest --tests "*CrowdSoundChoreographySimulationTest*" --no-daemon; then
                print_status "Sound choreography simulation completed successfully!"
            else
                print_warning "Sound choreography simulation had issues, but manual test should work fine."
            fi
            ;;
        "open")
            print_status "Emulator ready for manual testing (--open mode)."
            print_status "No automated test will be run."
            ;;
        "default")
            read -p "Would you like to run the sound choreography simulation test as well? (y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_status "Running unit test sound choreography simulation..."
                if ./gradlew :shared:testDebugUnitTest --tests "*CrowdSoundChoreographySimulationTest*" --no-daemon; then
                    print_status "Sound choreography simulation completed successfully!"
                else
                    print_warning "Sound choreography simulation had issues, but manual test should work fine."
                fi
            fi
            ;;
    esac
}

# Run main function
main "$@"