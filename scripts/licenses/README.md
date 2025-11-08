# Licenses

License compliance and dependency management tools for WorldWideWaves project.

## Purpose

Manages legal compliance and licensing:

- **Dependency license tracking** across all modules
- **License compatibility checking**
- **Legal documentation generation**
- **Compliance reporting** for app store submissions

## Structure

```
licenses/
├── reports/                  # Generated license reports
├── generate_report.sh       # Main license report generator
├── check_compliance.sh      # License compatibility checker
├── update_notices.sh        # Update app license notices
└── README.md               # This file
```

## Usage

### Generate License Report

```bash
# Generate comprehensive license report
./generate_report.sh

# Output: reports/license_report_YYYY-MM-DD.html
# Contains all dependencies and their licenses
```

### Check License Compliance  

```bash
# Verify all licenses are compatible
./check_compliance.sh

# Check specific module
./check_compliance.sh --module shared
```

### Update App License Notices

```bash
# Update in-app license attributions
./update_notices.sh

# Generates files for Android and iOS apps
```

## Generated Reports

### HTML Report (`reports/license_report.html`)

- **Complete dependency list** with versions
- **License texts** and requirements
- **Compatibility matrix** showing conflicts
- **Action items** for non-compliant dependencies

### JSON Export (`reports/licenses.json`)

```json
{
  "dependencies": [
    {
      "name": "kotlinx-coroutines-core",
      "version": "1.7.3",
      "license": "Apache-2.0",
      "license_url": "https://...",
      "compatible": true
    }
  ]
}
```

### Mobile App Notices

- **Android**: `app/src/main/assets/licenses.html`
- **iOS**: `iosApp/Resources/licenses.plist`

## License Categories

### Compatible Licenses ✅

- **Apache 2.0** - Most permissive
- **MIT** - Simple and permissive  
- **BSD (2-Clause, 3-Clause)** - Permissive with attribution
- **CC0** - Public domain equivalent

### Restricted Licenses ⚠️

- **LGPL 2.1/3.0** - Requires dynamic linking
- **MPL 2.0** - File-level copyleft
- **EPL 1.0/2.0** - Eclipse Public License

### Incompatible Licenses ❌

- **GPL 2.0/3.0** - Strong copyleft (conflicts with app store)
- **AGPL** - Network copyleft
- **Custom restrictive licenses**

## Configuration

### License Policy (`config/license_policy.json`)

```json
{
  "allowed_licenses": [
    "Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "CC0-1.0"
  ],
  "restricted_licenses": [
    "LGPL-2.1", "LGPL-3.0", "MPL-2.0"
  ],
  "forbidden_licenses": [
    "GPL-2.0", "GPL-3.0", "AGPL-3.0"
  ],
  "require_attribution": true,
  "commercial_use": true
}
```

### Gradle Integration

The license plugin automatically scans all Gradle modules:

```kotlin
// In build.gradle.kts  
plugins {
    id("com.github.jk1.dependency-license-report") version "2.5"
}

licenseReport {
    renderers = arrayOf(JsonReportRenderer())
    outputDir = "$buildDir/reports/licenses"
}
```

## Dependency Scanning

### Multi-module Scanning

```bash
# Scan all Gradle modules
for module in shared composeApp; do
    echo "Scanning $module..."
    ./gradlew :$module:generateLicenseReport
done
```

### Node.js Dependencies (maps module)

```bash
# Scan Node.js dependencies in scripts/maps
cd scripts/maps
npm list --json > ../../licenses/reports/npm_dependencies.json
```

### Manual Dependency Addition

```json
// For dependencies not auto-detected
{
  "manual_dependencies": [
    {
      "name": "Custom Library",
      "version": "1.0",
      "license": "MIT",
      "source": "Internal development"
    }
  ]
}
```

## Compliance Checking

### Automated Checks

```bash
#!/bin/bash
# check_compliance.sh logic

# 1. Scan all dependencies
./generate_report.sh --json-only

# 2. Check against policy  
python check_licenses.py reports/licenses.json config/license_policy.json

# 3. Report violations
if [ $? -ne 0 ]; then
    echo "❌ License compliance issues found"
    exit 1
fi
```

### Manual Review Process

1. **Review new dependencies** before adding
2. **Check license compatibility** with project goals
3. **Update attribution** if required
4. **Document exceptions** with justification

## App Store Compliance

### Google Play Store

- **Required**: License attribution in app
- **Location**: Settings → About → Licenses
- **Format**: HTML with clickable links

### Apple App Store  

- **Required**: Third-party license notices
- **Location**: Settings → Legal → Third Party Licenses
- **Format**: Plain text or bundled HTML

### Implementation

```kotlin
// Android - show licenses activity
class LicensesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        webView.loadUrl("file:///android_asset/licenses.html")
    }
}
```

```swift
// iOS - show licenses view
struct LicensesView: View {
    var body: some View {
        ScrollView {
            Text(loadLicenseText())
                .padding()
        }
    }
}
```

## Build Integration

### Pre-commit Hook

```bash
#!/bin/sh
# Check licenses before commit

if ./scripts/licenses/check_compliance.sh; then
    echo "✅ License compliance OK"
else
    echo "❌ License issues found. Please review."
    exit 1
fi
```

### CI/CD Pipeline

```yaml
# GitHub Actions
- name: Check License Compliance
  run: |
    ./scripts/licenses/generate_report.sh
    ./scripts/licenses/check_compliance.sh
    
- name: Upload License Report
  uses: actions/upload-artifact@v3
  with:
    name: license-report
    path: scripts/licenses/reports/
```

## Adding New Dependencies

### Workflow

1. **Add dependency** to build file
2. **Run license check**

   ```bash
   ./gradlew generateLicenseReport
   ./scripts/licenses/check_compliance.sh
   ```

3. **Review license terms** if new license detected
4. **Update attribution** if required
5. **Commit changes** including license updates

### Documentation Requirements

```markdown
# When adding new dependency
## Dependency: library-name v1.0
- **License**: MIT
- **Reason**: Required for feature X
- **Compatibility**: ✅ Compatible
- **Attribution**: Added to licenses.html
```

## Troubleshooting

### Common Issues

1. **Gradle scan fails**

   ```bash
   # Clean and retry
   ./gradlew clean
   ./gradlew generateLicenseReport
   ```

2. **Missing license information**

   ```bash
   # Manually research and document
   grep -r "license" node_modules/package-name/
   ```

3. **License conflicts**

   ```bash
   # Find alternative dependency or get approval
   ./find_alternatives.sh problematic-dependency
   ```

### Debug Mode

```bash
# Verbose license scanning
DEBUG=1 ./generate_report.sh

# Test specific dependency
./check_single_dependency.sh com.example:library:1.0
```

### Maintenance

#### Quarterly Review

```bash
# Update all license information
./update_all_licenses.sh

# Generate fresh compliance report
./generate_report.sh --fresh

# Review for new dependencies or license changes
diff reports/previous_report.json reports/current_report.json
```

#### Annual Audit

- **Review license policy** for changes in legal landscape
- **Check dependency updates** for license changes  
- **Update app store notices** if required
- **Document any exceptions** or special arrangements
