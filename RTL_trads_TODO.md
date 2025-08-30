# RTL & i18n Refactor TODO  
_Branch: `feature/rtl-i18n-android`_

## 1. Overview  
Goal: full RTL readiness & wider language support on Android.  
Scope covers UI alignment, bidi safety, hard-coded strings, font coverage, locales declaration and future localisation of dates/plurals.

---

## 2. Implemented Changes  
| ✓ | File / Area | Description |
|---|-------------|-------------|
| [x] | `AndroidManifest.xml` | `supportsRtl="true"` already active |
| [x] | **Branch created** | `feature/rtl-i18n-android` (no new files staged by user) |
| [x] | **TODO tracker** | This file documents status |
| [x] | `AbstractEventBackActivity.kt` | Replaced manual “\< Back” with `Icons.AutoMirrored` arrow & string resource |
| [x] | Various screens | Converted `TextAlign.Right/Left` → `TextAlign.End/Start` |
| [x] | Whole codebase | Removed unsafe `.uppercase()` usages (buttons, tabs, locations) |
| [x] | `EventsListScreen.kt`, `EventActivity.kt` | Added `BidiFormatter` wrapping for mixed-direction strings |
| [x] | `Commons.kt` | SOON/RUNNING badge now wrap-content width |
| [x] | Resources | Externalised hard-coded strings (Instagram logo, uninstall results, OK, Error, Test Simulation, Simulation Started) |
| [x] | `AboutInfoScreen.kt` | Conditional justification removed for RTL layouts |
| [x] | `theme/Type.kt` | Added extensive Google Noto font fallbacks per script (incl. **Noto Nastaliq Urdu** for Urdu) |
| [x] | `res/xml/locales_config.xml` + Manifest | Locale list created & referenced via `android:localeConfig` |

---

## 3. Pending / Future Improvements  
| ☐ | Task |
|---|------|
| [x] Date/Time localisation: shared `DateTimeFormats` **expect/actual** added (day-month & timeShort); UI now uses it + pluralised hours/minutes, removing hard-coded “dd/MM” & “X min”. |
| [x] Declare new languages in `scripts/translate/update_translations.py` (list already matches full locales set). |
| [x] Ensure remaining icons/images (fav, map, etc.) are auto-mirrored or neutral – audit shows no remaining directional assets; back navigation uses `AutoMirrored` icon. |
| [ ] QA pass on real devices/emulators in Arabic, Hebrew, Urdu |

---

### Since last update  
* Added KMP `DateTimeFormats` (Android / iOS) for `dayMonth` & `timeShort`.  
* Refactored `EventActivity` / `EventsListScreen` to use shared formatter.  
* Implemented plural resources for hours & minutes; total time now human-friendly.  
* Performed icon audit – all navigation icons use `AutoMirrored` or are neutral.  

## 4. Language Coverage & `locales_config.xml`  
Planned list (36 codes):  
`am`, `ar`, `bn`, `de`, `es`, `fa`, `fil`, `fr`, `ha`, `he`, `hi`, `id`, `ig`,  
`it`, `ja`, `ko`, `ms`, `nl`, `pa`, `pl`, `pt`, `ro`, `ru`, `sw`, `th`, `tr`,  
`uk`, `ur`, `vi`, `xh`, `yo`, `zh`, `zu`.

Actions:  
- Add missing `moko-resources/<lang>/strings.xml` (user in progress).  
- Generate `res/xml/locales_config.xml` containing the above tags.  
- Add `android:localeConfig="@xml/locales_config"` to `<application>`.

---

## 5. Font Fallback Plan  
Add Google Fonts / system fallbacks via `FontFamily` chain:

| Script | Font |
|--------|------|
| Latin / Cyrillic | Montserrat (current) |
| Arabic / Persian / Urdu | Noto Sans Arabic |
| Hebrew | Noto Sans Hebrew |
| Bengali | Noto Sans Bengali |
| Devanagari (hi) | Noto Sans Devanagari |
| Thai | Noto Sans Thai |
| Ethiopic (am) | Noto Sans Ethiopic |
| Chinese SC / Japanese / Korean | Noto Sans CJK SC / JP / KR |
| Others (Igbo, Yoruba, Hausa, etc.) | Noto Sans (regular) |

Implementation: update `theme/Type.kt` to build a `FontFamily` list per script; Compose will choose the first font that supports the glyph.

---

## 6. Notes  
- **Do not stage** the new language resource files the user is adding in parallel.  
- All changes stay confined to branch `feature/rtl-i18n-android` until review.  
- When a pending box is completed, mark as `[x]` and move item to §2.

