#!/usr/bin/env bash
# Copyright 2025 DrWave
#
# Generates GoogleService-Info.plist for iOS from environment variables or local.properties
# This script mirrors the Android google-services.json generation behavior

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCAL_PROPERTIES="$PROJECT_ROOT/local.properties"
OUTPUT_FILE="$PROJECT_ROOT/iosApp/worldwidewaves/GoogleService-Info.plist"

echo "🔥 Generating GoogleService-Info.plist for iOS..."

# Check if file already exists
if [ -f "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
    echo "✅ GoogleService-Info.plist already exists, skipping generation"
    exit 0
fi

# Function to get property from local.properties
get_property() {
    local key="$1"
    local value=""
    if [ -f "$LOCAL_PROPERTIES" ]; then
        value=$(grep "^${key}=" "$LOCAL_PROPERTIES" | cut -d'=' -f2- | sed 's/\r$//')
    fi
    echo "$value"
}

# Load from local.properties if it exists
if [ -f "$LOCAL_PROPERTIES" ]; then
    echo "📝 Loading Firebase config from local.properties..."
fi

# Get values from environment variables (take precedence) or local.properties
FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID:-$(get_property 'FIREBASE_PROJECT_ID')}"
FIREBASE_PROJECT_NUMBER="${FIREBASE_PROJECT_NUMBER:-$(get_property 'FIREBASE_PROJECT_NUMBER')}"
FIREBASE_IOS_APP_ID="${FIREBASE_IOS_APP_ID:-$(get_property 'FIREBASE_IOS_APP_ID')}"
[ -z "$FIREBASE_IOS_APP_ID" ] && FIREBASE_IOS_APP_ID="$(get_property 'FIREBASE_MOBILE_SDK_APP_ID')"

# Use iOS-specific API key if available, otherwise fall back to shared key
FIREBASE_API_KEY="${FIREBASE_IOS_API_KEY:-$(get_property 'FIREBASE_IOS_API_KEY')}"
[ -z "$FIREBASE_API_KEY" ] && FIREBASE_API_KEY="${FIREBASE_API_KEY:-$(get_property 'FIREBASE_API_KEY')}"

FIREBASE_CLIENT_ID="${FIREBASE_CLIENT_ID:-$(get_property 'FIREBASE_CLIENT_ID')}"
FIREBASE_REVERSED_CLIENT_ID="${FIREBASE_REVERSED_CLIENT_ID:-$(get_property 'FIREBASE_REVERSED_CLIENT_ID')}"
FIREBASE_GCM_SENDER_ID="${FIREBASE_GCM_SENDER_ID:-${FIREBASE_PROJECT_NUMBER}}"

# Validate required fields
if [ -z "$FIREBASE_PROJECT_ID" ] || [ -z "$FIREBASE_PROJECT_NUMBER" ] || \
   [ -z "$FIREBASE_IOS_APP_ID" ] || [ -z "$FIREBASE_API_KEY" ]; then
    echo "❌ Error: Missing required Firebase configuration"
    echo ""
    echo "Please set the following in local.properties or environment variables:"
    echo "  - FIREBASE_PROJECT_ID"
    echo "  - FIREBASE_PROJECT_NUMBER"
    echo "  - FIREBASE_IOS_APP_ID (or FIREBASE_MOBILE_SDK_APP_ID)"
    echo "  - FIREBASE_API_KEY"
    echo ""
    echo "Optional (for OAuth features):"
    echo "  - FIREBASE_CLIENT_ID"
    echo "  - FIREBASE_REVERSED_CLIENT_ID"
    exit 1
fi

# Generate GoogleService-Info.plist
cat > "$OUTPUT_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>API_KEY</key>
	<string>$FIREBASE_API_KEY</string>
	<key>GCM_SENDER_ID</key>
	<string>$FIREBASE_GCM_SENDER_ID</string>
	<key>PLIST_VERSION</key>
	<string>1</string>
	<key>BUNDLE_ID</key>
	<string>com.worldwidewaves</string>
	<key>PROJECT_ID</key>
	<string>$FIREBASE_PROJECT_ID</string>
	<key>STORAGE_BUCKET</key>
	<string>$FIREBASE_PROJECT_ID.firebasestorage.app</string>
	<key>IS_ADS_ENABLED</key>
	<false/>
	<key>IS_ANALYTICS_ENABLED</key>
	<true/>
	<key>IS_APPINVITE_ENABLED</key>
	<true/>
	<key>IS_GCM_ENABLED</key>
	<true/>
	<key>IS_SIGNIN_ENABLED</key>
	<true/>
	<key>GOOGLE_APP_ID</key>
	<string>$FIREBASE_IOS_APP_ID</string>
EOF

# Add optional CLIENT_ID if provided
if [ -n "$FIREBASE_CLIENT_ID" ]; then
    cat >> "$OUTPUT_FILE" << EOF
	<key>CLIENT_ID</key>
	<string>$FIREBASE_CLIENT_ID</string>
EOF
fi

# Add optional REVERSED_CLIENT_ID if provided
if [ -n "$FIREBASE_REVERSED_CLIENT_ID" ]; then
    cat >> "$OUTPUT_FILE" << EOF
	<key>REVERSED_CLIENT_ID</key>
	<string>$FIREBASE_REVERSED_CLIENT_ID</string>
EOF
fi

# Close the plist
cat >> "$OUTPUT_FILE" << EOF
</dict>
</plist>
EOF

echo "✅ Generated GoogleService-Info.plist successfully"
echo "📁 Location: $OUTPUT_FILE"
EOF
