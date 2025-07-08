# NEXT_STEPS_ORDER.md  
Implementation Road-map – iOS View Adaptation  
Branch: `feature/ios-adaptation`

---

## Legend  
| Col | Meaning |
|-----|---------|
| **Step #** | Sequential priority (finish one before starting next). |
| **iOS Feature** | View / component to implement. |
| **Ask Factory For** | Exact request to send to Factory when beginning the step. |
| **Depends On** | Code or assets that must already exist / be merged. |
| **Commit Template** | Commit header & body guide. |
| **Tests** | What to write/run before closing the step. |
| **Shared Enhancements** | Common code you can lift/refactor while here. |

---

## Ordered Table

| Step # | iOS Feature | Ask Factory For | Depends On | Commit Template | Tests | Shared Enhancements |
|-------:|-------------|-----------------|------------|-----------------|-------|---------------------|
| 1 | **SplashView polishing** | “Implement final artwork + adaptive logo sizing for SplashView” | Current skeleton | `feat(splash): Adaptive logo & art\n\n- Description…` | - Snapshot test at 3 sizes<br>- Unit assert min-duration logic | Move constant logo sizes to shared `WWWGlobals` expect/actual if missing |
| 2 | **MainView – tab icons & state restore** | “Add persistence of selected tab & dynamic icons” | Step 1 | `feat(main): Persist selected tab` | - XCT snapshot after relaunch<br>- Unit for `@AppStorage` value | Promote tab meta-data (icon names, labels) to shared `TabInfo` model |
| 3 | **EventsListView – list polishing & pull-to-refresh** | “Render event cards, hook Combine to Kotlin Flow” | Step 2 | `feat(events-list): KMP flow & refresh` | - Unit: Combine bridge emits list<br>- UI: pull-to-refresh works | Introduce `kotlinx-coroutines-core-native` bridge util in commonMain |
| 4 | **AboutView – static Markdown content** | “Load about.md from shared resources” | Step 2 | `feat(about): Markdown support` | - Snapshot test of markdown | Add `expect/actual MarkdownLoader` in shared |
| 5 | **SettingsView – datastore binding** | “Bind toggles to shared SettingsRepository” | Step 2 | `feat(settings): Shared datastore bindings` | - Unit: toggles persist<br>- UI: toggle round-trip | Create common `SettingsRepository` with Android DataStore / iOS UserDefaults actuals |
| 6 | **EventView – countdown + map preview** | “Embed MapLibre preview & realtime countdown” | Steps 3,5 | `feat(event): Countdown & preview map` | - Unit: countdown math<br>- UI: opens preview map | Move countdown utility to common `TimeUtils` |
| 7 | **EventFullMapView – full MapLibre + ODR** | “Integrate MapLibre SDK & On-Demand Resources for 1 city” | Step 6, MapLibre SPM added | `feat(full-map): MapLibre & ODR (paris)` | - UI: load map offline<br>- Unit: ODR download mocked | Add common `CityMapRegistry` with module names for Android DF & iOS ODR |
| 8 | **WaveView – choreography playback** | “Wire ChoreographyManager frames to SwiftUI animation” | Step 6 | `feat(wave): Frame animation playback` | - Unit: frame stream mapper<br>- Performance: 60fps on device | Introduce `expect/actual AnimationFrameListener` in commonMain |
| 9 | **WaveView – audio & sensors** | “Connect SoundChoreographyManager & device motion” | Step 8 | `feat(wave): Sound & motion` | - Audio played at correct beat<br>- Motion data logged | Add platform-agnostic `MotionProvider` interface |
|10 | **Downloaded Maps Manager (Settings)** | “Build Manage Downloaded Maps screen & deletion logic” | Step 7 | `feat(odr-manager): Manage map downloads` | - UI list of maps<br>- Unit: delete frees disk | Share `MapDownloadStatus` model commonMain |
|11 | **Polish + Accessibility** | “Accessibility labels, dynamic type, voice-over” | All prior | `chore: iOS accessibility polish` | - Accessibility audit | Extract common string resources to shared localisation files |

---

## Commit Message Rules

```
# Header
<type>(<scope>): <summary>

# Body
- WHAT: bullet points of changes
- WHY : rationale / reference step
- TEST: brief description of tests added
```

`type` = feat | fix | chore | refactor | test | docs.  
`scope` is the feature name (splash, main, odr, wave, …).

---

## Testing Approach

1. **Unit (XCTest)** – Pure logic & view-model state.
2. **Snapshot (iOSSnapshotTestCase)** – UI regression for each view.
3. **Integration (XCUITest)** – End-to-end flows: splash → tab → event.
4. **Performance (XCTestMetrics)** – Map render FPS, wave animation memory.

Write *at least* unit + snapshot before merging each step.

---

## How to Use This File

For each step:

1. Checkout `feature/ios-adaptation`
2. `git pull --rebase`
3. Open a new chat with Factory: copy the **Ask Factory For** sentence.
4. Implement, run tests, commit with the suggested template.
5. Create PR labelled `iOS-Step-<#>` targeting `feature/ios-adaptation`.
6. Repeat.

Happy porting!  
