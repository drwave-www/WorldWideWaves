# Pull Request

## Description
<!-- Briefly describe the changes in this PR -->

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Accessibility improvement

## Testing
- [ ] Unit tests pass locally (`./gradlew :shared:testDebugUnitTest`)
- [ ] Instrumented tests pass (if applicable)
- [ ] Manual testing completed
- [ ] No regressions in existing functionality

## Accessibility Checklist ♿
<!-- REQUIRED for all UI changes -->

### Android
- [ ] All interactive elements have `contentDescription` or semantic roles
- [ ] All custom components have `Modifier.semantics { role, contentDescription, stateDescription }`
- [ ] Touch targets meet 48dp minimum
- [ ] Color contrast verified (4.5:1 minimum)
- [ ] Tested with TalkBack enabled
- [ ] Dynamic content has live regions (`liveRegion = LiveRegionMode.Polite`)

### iOS
- [ ] VoiceOver announcements added for critical events (if applicable)
- [ ] Dynamic Type scaling verified at all sizes (0.8x - 3.0x)
- [ ] Haptic feedback implemented for important actions (if applicable)
- [ ] Map features accessible (if map modified)
- [ ] Tested with VoiceOver enabled
- [ ] No iOS deadlock patterns (init{} with DI, object in @Composable)

### Testing
- [ ] Accessibility tests pass (`./scripts/test_accessibility.sh`)
- [ ] Manual TalkBack testing completed (Android)
- [ ] Manual VoiceOver testing completed (iOS)
- [ ] Text scales properly (Android: 200%, iOS: 300%)

See **[Accessibility Guide](../docs/accessibility-guide.md)** for implementation patterns.

## Code Quality
- [ ] Code follows project conventions (see CLAUDE.md)
- [ ] No new compiler warnings
- [ ] Detekt/SwiftLint checks pass
- [ ] Documentation updated (if needed)
- [ ] No hardcoded strings (use MokoRes.strings for localization)

## iOS Safety ⚠️
<!-- REQUIRED for changes to shared code -->
- [ ] No `object : KoinComponent` inside `@Composable` functions
- [ ] No `init{}` blocks with `launch{}` or DI access (`get()`, `inject()`)
- [ ] No `runBlocking` usage
- [ ] No `Dispatchers.Main` in constructors
- [ ] iOS safety verification passed (`./scripts/verify-ios-safety.sh`)

## Screenshots / Videos
<!-- If UI changes, provide screenshots for Android and iOS -->

### Android
<!-- Add screenshots here -->

### iOS
<!-- Add screenshots here -->

## Related Issues
<!-- Link to related issues: Fixes #123, Relates to #456 -->

## Additional Notes
<!-- Any additional context, concerns, or discussion points -->

---

## Reviewer Checklist
- [ ] Code reviewed for logic and best practices
- [ ] Accessibility requirements met
- [ ] No performance regressions
- [ ] Documentation adequate
- [ ] Tests are comprehensive
- [ ] iOS safety patterns followed (for shared code)
