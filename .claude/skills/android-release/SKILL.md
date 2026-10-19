---
name: android-release
description: Prepare, verify, sign and document a WorldWideWaves Android release for Google Play. Use when asked to publish, release, or ship a new Android version, bump the Android version, triage PRs before a release, sign the app bundle, or write Play Store release notes.
---

# Android Release (Google Play)

**TL;DR**: Merge or defer open PRs, bump the version, verify both platforms, build the release bundle, check its device requirements against the last release, smoke-test it on an emulator, copy it out of `build/`, sign it, and write release notes with Play language tags.

**When to read**: before every Play Store release, and whenever Play Console rejects a build.

## 1. Triage open pull requests

**Every open PR is either merged or explicitly deferred with a comment.**

- List them with `gh pr list --repo drwave-www/WorldWideWaves --state open`.
- The gh token cannot merge PRs or close them. Merge locally with `git merge --no-ff origin/<branch>`. Pushing main closes them on GitHub.
- A PR whose branch was rebased locally does not auto-close. Comment on it with the landed commit, and ask the user to close it.
- Read the real CI failure before deciding. Use `gh run view <run> --job <job> --log`.
- Defer PRs that need build-config changes (for example a compileSdk bump) or code migrations. Leave a comment explaining why.

## 2. Bump the version

**`versionCode` must be higher than every code ever uploaded, including rejected or discarded uploads.**

- Edit `versionCode` and `versionName` in `composeApp/build.gradle.kts`.
- Play never accepts a code twice. If a build is rejected, bump again before re-uploading.
- Ask the user to confirm the last uploaded code in Play Console when unsure.

## 3. Verify both platforms

**Run the full check from CLAUDE.md "Cross-Platform Compilation Check", plus the Swift build.**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew clean :shared:testDebugUnitTest :shared:compileDebugKotlinAndroid \
  :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosX64 detekt \
  :composeApp:bundleRelease
cd iosApp && xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

- Detekt needs JDK 21; it fails on JDK 25.
- `compileKotlinIosX64` catches dependencies that dropped the Intel simulator.
- After `xcodebuild`, commit `Package.resolved` if Xcode changed it.

## 4. Check device requirements

**Compare the merged release manifest with the previous release before uploading.**

```bash
./gradlew :composeApp:processReleaseMainManifest
grep -A2 -E 'uses-sdk|uses-feature' \
  composeApp/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

- Expected: minSdk 24, OpenGL ES 2.0, no `android.hardware.vulkan.version`.
- Any new required `uses-feature` shrinks device coverage. Find its source in `composeApp/build/outputs/logs/manifest-merger-release-report.txt`.
- MapLibre must stay on the `android-sdk-opengl` artifact. See `gradle/libs.versions.toml` and the annotation-plugin exclusion in `composeApp/build.gradle.kts`.

## 5. Smoke-test the release bundle

**Install the minified release bundle the way Play does, then launch it.** R8 bugs only appear in release builds, and unit tests do not catch them.

```bash
S=<scratch dir>
curl -sL -o $S/bundletool.jar \
  https://github.com/google/bundletool/releases/download/1.18.1/bundletool-all-1.18.1.jar
java -jar $S/bundletool.jar build-apks \
  --bundle=composeApp/build/outputs/bundle/release/composeApp-release.aab \
  --output=$S/app.apks --connected-device --device-id=emulator-5554 \
  --ks=$HOME/.android/debug.keystore --ks-pass=pass:android \
  --ks-key-alias=androiddebugkey --key-pass=pass:android
adb uninstall com.worldwidewaves
java -jar $S/bundletool.jar install-apks --apks=$S/app.apks --device-id=emulator-5554
adb logcat -c
adb shell monkey -p com.worldwidewaves -c android.intent.category.LAUNCHER 1
# wait ~30 s, then:
adb logcat -d | grep -E 'FATAL EXCEPTION|Fatal signal|ANR in'
adb shell monkey -p com.worldwidewaves --pct-syskeys 0 --throttle 150 -s 4242 -v 3000
```

- Start the emulator first: `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1`.
- Zero matches is required. Open an event screen and confirm the map renders.
- "Split Install Error(-15)" is expected for sideloaded builds and is not a bug.
- Deobfuscate stack traces with `composeApp/build/outputs/mapping/release/mapping.txt`.

## 6. Keep the bundle safe from cleanups

**The pre-push hook cleans `build/`, so copy the bundle out before pushing.**

```bash
mkdir -p ~/Code/WorldWideWaves-releases
cp composeApp/build/outputs/bundle/release/composeApp-release.aab \
  ~/Code/WorldWideWaves-releases/WorldWideWaves-v<version>-<code>.aab
```

## 7. Sign the bundle

**The build has no signing config. The user signs manually with the upload keystore.**

- Keystore: `/Users/mglcel/Code/AndroidKeyStore`. Never store or log its passwords.
- The user runs these commands, since they prompt for passwords:

```bash
keytool -list -keystore /Users/mglcel/Code/AndroidKeyStore
jarsigner -keystore /Users/mglcel/Code/AndroidKeyStore \
  -sigalg SHA256withRSA -digestalg SHA-256 \
  ~/Code/WorldWideWaves-releases/WorldWideWaves-v<version>-<code>.aab <alias>
jarsigner -verify ~/Code/WorldWideWaves-releases/WorldWideWaves-v<version>-<code>.aab
```

## 8. Write Play Store release notes

**Play Console expects one block per language tag, each under 500 characters.**

```text
<en-US>
• User-facing change one
• User-facing change two
</en-US>
<fr-FR>
• ...
</fr-FR>
```

- Only describe what users notice. Leave out dependency bumps and fixes for bugs that never shipped.
- Play locale codes for the app languages: `en-US fr-FR es-ES pt-BR de-DE it-IT nl-NL pl-PL ro ru-RU tr-TR uk ar fa iw-IL ur am sw zu bn-BD hi-IN pa id ms fil ja-JP ko-KR zh-CN th vi`.
- Hausa, Igbo, Yoruba and Xhosa have no Play locale. Those users see English.
- Play rejects tags missing from the store listing. Drop any tag it flags.
- Translations other than English and French need native review.

## 9. Push and tag

**Push main through the full pre-push hook, never with `--no-verify`.**

- The hook's translation step needs a Python with `lxml` and `openai`. If Homebrew Python is broken, create a venv with `uv venv --python 3.13` and put it first on `PATH`.
- The hook takes over 10 minutes. Run the push in the background.
- After pushing, the hook leaves a Dokka commit. If `git diff origin/main main` is empty, drop it with `git reset --soft origin/main`.
- Tag the release once the user confirms the upload: `git tag RELEASE_ANDROID_v<version>` and `git tag RELEASE_ANDROID_build_<code>_v<version>`.

## Known release failures

**Each of these reached Play Console before being caught.**

| Play Console message | Cause | Fix |
| --- | --- | --- |
| "no longer supports N devices" | MapLibre 13 default artifact requires Vulkan | Use `android-sdk-opengl` and exclude `android-sdk` from the annotation plugin |
| "app crashed during testing" | R8 renamed protobuf-lite fields used by Firebase Performance | Keep rule for `GeneratedMessageLite` fields in `composeApp/proguard-rules.pro` |
| "signed with the wrong key" | Bundle signed with a key other than the registered upload key | Sign with the keystore in step 7 |
