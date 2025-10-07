# Firebase Configuration Setup

## Security Notice

The Firebase configuration is now securely managed through local.properties (development) and environment variables (production) to protect against unauthorized usage.

## Automated Configuration

The project includes unified scripts for generating Firebase configuration for both platforms:

```bash
# Generate configs for both Android and iOS
./scripts/generate_firebase_config.sh all

# Generate Android only
./scripts/generate_firebase_config.sh android

# Generate iOS only
./scripts/generate_firebase_config.sh ios

# Alternative: Use Gradle for Android (auto-runs during build)
./gradlew :composeApp:generateFirebaseConfig
```

**Note**: Android config is also automatically generated during any Gradle build.

## Setup Instructions

### For Development

1. **Update local.properties**:
   ```properties
   # Add these lines to local.properties (replace with actual values)
   FIREBASE_PROJECT_ID=your-project-id
   FIREBASE_PROJECT_NUMBER=your-project-number
   FIREBASE_MOBILE_SDK_APP_ID=your-app-id
   FIREBASE_API_KEY=your-api-key
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   # Firebase config is automatically generated
   ```

### Setting Up Your Own Firebase Project

1. **Create Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing one

2. **Add Android App**:
   - Package name: `com.worldwidewaves`
   - Download `google-services.json`
   - Place it in `composeApp/` directory

3. **Enable Required Services**:
   - Firebase Analytics
   - Firebase Crashlytics
   - Any other services used by the app

4. **Configure API Key Restrictions** (Recommended):
   - Go to Google Cloud Console
   - Navigate to Credentials
   - Restrict the API key to specific Android apps and APIs

### For Production/CI/CD

Set these environment variables in your CI/CD pipeline or production environment:

```bash
# GitHub Actions example (in repository secrets)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PROJECT_NUMBER=your-project-number
FIREBASE_MOBILE_SDK_APP_ID=your-app-id
FIREBASE_API_KEY=your-api-key
```

**GitHub Actions Integration**:
Add to your workflow file:
```yaml
- name: Generate Firebase Config
  env:
    FIREBASE_PROJECT_ID: ${{ secrets.FIREBASE_PROJECT_ID }}
    FIREBASE_PROJECT_NUMBER: ${{ secrets.FIREBASE_PROJECT_NUMBER }}
    FIREBASE_MOBILE_SDK_APP_ID: ${{ secrets.FIREBASE_MOBILE_SDK_APP_ID }}
    FIREBASE_API_KEY: ${{ secrets.FIREBASE_API_KEY }}
  run: ./gradlew generateFirebaseConfig
```

## Template File

Use `composeApp/google-services.json.template` as a reference for the expected structure.

## Security Best Practices

- Never commit `google-services.json` to version control
- Restrict API keys to specific applications and services
- Monitor API usage for unusual activity
- Rotate API keys periodically
- Use different Firebase projects for development/staging/production