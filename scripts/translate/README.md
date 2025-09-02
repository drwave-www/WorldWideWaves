# Translate

Localization and translation management tools for WorldWideWaves multi-language support.

## Purpose

Manages app translations and localization:
- **String extraction** from code and resources
- **Translation management** across multiple languages
- **Automated translation** using translation services
- **Translation validation** and quality control

## Structure

```
translate/
├── extract_strings.py      # Extract translatable strings
├── manage_translations.py  # Translation workflow management  
├── validate_translations.py # Quality control and validation
├── auto_translate.py       # Automated translation using APIs
├── export_formats.py      # Export to platform formats
└── README.md              # This file
```

## Usage

### Extract Strings
```bash
# Extract all translatable strings from code
python extract_strings.py --source ../shared/src/commonMain/

# Extract from specific files
python extract_strings.py --files MainActivity.kt EventsListScreen.kt
```

### Manage Translations
```bash
# Initialize new language
python manage_translations.py --add-language es

# Update existing translations
python manage_translations.py --update-language fr --source translations/en.json
```

### Validate Translations
```bash
# Check translation completeness
python validate_translations.py --language es

# Validate all languages
python validate_translations.py --all
```

### Auto-Translate
```bash
# Auto-translate missing strings
python auto_translate.py --from en --to es --service google

# Batch translate multiple languages
python auto_translate.py --batch --languages "es,fr,de,ja"
```

## Supported Languages

### Current Languages
- **English (en)** - Primary language
- **Spanish (es)** - Spanish
- **French (fr)** - French  
- **German (de)** - German
- **Portuguese (pt)** - Portuguese
- **Japanese (ja)** - Japanese
- **Arabic (ar)** - Arabic (RTL support)
- **Chinese (zh)** - Simplified Chinese
- **Russian (ru)** - Russian

### Adding New Languages
```bash
# Add new language with auto-translation
python manage_translations.py --add-language ko --auto-translate

# Creates:
# - translations/ko.json
# - Android values-ko/strings.xml  
# - iOS ko.lproj/Localizable.strings
```

## Translation File Formats

### Master Format (JSON)
```json
{
  "app_name": "WorldWideWaves",
  "events": {
    "title": "Upcoming Waves",
    "empty_message": "No events scheduled",
    "join_button": "Join Wave",
    "countdown": "{hours}h {minutes}m until wave"
  },
  "cities": {
    "paris_france": "Paris, France",
    "london_england": "London, England"
  }
}
```

### Android Strings
```xml
<!-- values-es/strings.xml -->
<resources>
    <string name="app_name">WorldWideWaves</string>
    <string name="events_title">Ondas Próximas</string>
    <string name="events_empty_message">No hay eventos programados</string>
    <string name="events_join_button">Unirse a la Onda</string>
</resources>
```

### iOS Localizable
```swift
// es.lproj/Localizable.strings
"app_name" = "WorldWideWaves";
"events.title" = "Ondas Próximas";
"events.empty_message" = "No hay eventos programados";
"events.join_button" = "Unirse a la Onda";
```

### KMP Resources
```kotlin
// shared/src/commonMain/composeResources/values-es/strings.xml
<resources>
    <string name="app_name">WorldWideWaves</string>
    <string name="events_title">Ondas Próximas</string>
</resources>
```

## String Extraction

### From Kotlin/Java Code
```python
# Patterns to extract
STRING_PATTERNS = [
    r'getString\(R\.string\.(\w+)\)',           # Android getString
    r'stringResource\(Res\.string\.(\w+)\)',   # Compose resources
    r'"([^"]+)"\s*//\s*@Translatable',         # Marked strings
]
```

### From Compose Resources
```kotlin
// Mark strings for translation
Text(
    text = stringResource(Res.string.welcome_message),
    // Auto-extracted
)

Text(
    text = "Join the wave!", // @Translatable
    // Extracted with comment marker
)
```

### Configuration Files
```json
{
  "extraction": {
    "source_paths": [
      "../shared/src/commonMain/",
      "../composeApp/src/androidMain/"
    ],
    "exclude_patterns": ["test/**", "debug/**"],
    "include_comments": true,
    "extract_plurals": true
  }
}
```

## Translation Workflow

### 1. String Extraction
```bash
# Extract new/modified strings
python extract_strings.py --incremental

# Review extracted strings
python review_extractions.py --show-new
```

### 2. Translation Assignment
```python
# Assign strings to translators
assign_strings_to_translator(
    language='es',
    strings=['new_feature_title', 'new_feature_description'],
    translator='maria@translations.com'
)
```

### 3. Translation Validation
```bash
# Check translation quality
python validate_translations.py --language es --check-placeholders

# Validate context and meaning
python validate_translations.py --context-check --language es
```

### 4. Export to Platforms
```bash
# Generate platform-specific files
python export_formats.py --languages "es,fr,de" --platforms android,ios,kmp
```

## Quality Control

### Validation Rules
```python
VALIDATION_RULES = {
    'placeholder_consistency': True,    # Check {variable} placeholders
    'length_limits': {                  # UI length constraints
        'button_text': 20,
        'title': 50, 
        'description': 200
    },
    'required_keys': [                  # Must be translated
        'app_name',
        'events.title',
        'error.network'
    ],
    'forbidden_patterns': [             # Patterns to avoid
        r'TODO',
        r'FIX.*',
        r'[A-Z]{5,}'                   # All-caps words
    ]
}
```

### Automated Checks
```bash
# Run comprehensive validation
python validate_translations.py --comprehensive

# Checks performed:
# - Translation completeness
# - Placeholder consistency  
# - Length limits
# - Context appropriateness
# - Grammar and spelling (basic)
```

### Manual Review
```python
# Generate review reports
def generate_review_report(language):
    report = {
        'missing_translations': find_missing_keys(language),
        'potentially_incorrect': find_suspicious_translations(language),
        'length_violations': find_length_violations(language),
        'context_issues': find_context_issues(language)
    }
    return report
```

## Translation Services

### Google Translate API
```python
from google.cloud import translate

def auto_translate_with_google(text, target_language):
    client = translate.TranslationServiceClient()
    response = client.translate_text(
        contents=[text],
        target_language_code=target_language,
        parent=f"projects/{PROJECT_ID}/locations/global"
    )
    return response.translations[0].translated_text
```

### Azure Translator
```python
import requests

def auto_translate_with_azure(text, target_language):
    url = "https://api.cognitive.microsofttranslator.com/translate"
    headers = {
        'Ocp-Apim-Subscription-Key': API_KEY,
        'Content-Type': 'application/json'
    }
    data = [{
        'text': text
    }]
    params = {
        'api-version': '3.0',
        'to': target_language
    }
    response = requests.post(url, headers=headers, params=params, json=data)
    return response.json()[0]['translations'][0]['text']
```

## RTL Language Support

### Arabic and Hebrew Support
```json
{
  "rtl_languages": ["ar", "he", "fa"],
  "rtl_handling": {
    "text_direction": "rtl",
    "layout_direction": "rtl", 
    "icon_mirroring": true
  }
}
```

### Platform Configuration
```kotlin
// Android RTL support
android {
    defaultConfig {
        resConfigs "en", "es", "fr", "ar"
    }
}

// In AndroidManifest.xml
<application android:supportsRtl="true">
```

```swift
// iOS RTL support  
func configureRTL() {
    if Locale.current.characterDirection == .rightToLeft {
        // Configure RTL layout
        UIView.appearance().semanticContentAttribute = .forceRightToLeft
    }
}
```

## Pluralization

### Plural Rules
```json
{
  "wave_count": {
    "en": {
      "one": "{count} wave",
      "other": "{count} waves"
    },
    "es": {
      "one": "{count} onda", 
      "other": "{count} ondas"
    },
    "ru": {
      "one": "{count} волна",
      "few": "{count} волны", 
      "many": "{count} волн"
    }
  }
}
```

### Platform Export
```xml
<!-- Android plurals -->
<plurals name="wave_count">
    <item quantity="one">%d wave</item>
    <item quantity="other">%d waves</item>
</plurals>
```

```swift
// iOS .stringsdict
<key>wave_count</key>
<dict>
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@waves@</string>
    <key>waves</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>one</key>
        <string>%d wave</string>
        <key>other</key>
        <string>%d waves</string>
    </dict>
</dict>
```

## Build Integration

### Automated Translation
```yaml
# GitHub Actions
- name: Extract New Strings
  run: python scripts/translate/extract_strings.py --incremental

- name: Auto-translate Missing Strings  
  run: python scripts/translate/auto_translate.py --missing-only

- name: Generate Platform Files
  run: python scripts/translate/export_formats.py --all
```

### Validation in CI
```bash
# Pre-commit validation
python scripts/translate/validate_translations.py --all --fail-on-missing
```

## Troubleshooting

### Common Issues

1. **Missing translations**
   ```bash
   # Find untranslated strings
   python validate_translations.py --language es --show-missing
   ```

2. **Placeholder mismatches**
   ```bash
   # Fix placeholder issues
   python validate_translations.py --fix-placeholders --language es
   ```

3. **Export format errors**
   ```bash
   # Debug platform export
   python export_formats.py --debug --platform android --language es
   ```

### Debug Tools
```bash
# Test translation extraction
python extract_strings.py --dry-run --verbose

# Validate specific translation file
python validate_translations.py --file translations/es.json --detailed
```