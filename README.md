# NativeAccent

A pronunciation-practice Android app. **v0: record-and-compare only — there is no scoring yet.**

You read a word or sentence, optionally hear a coach voice say it (Android TextToSpeech, US English),
record yourself, then play the two back to back at 1x or 0.5x. That is the whole loop.

---

## Quick start

```powershell
# from the project root, in PowerShell
.\gradlew.bat assembleDebug     # builds app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug      # builds + installs onto a connected device/emulator
.\gradlew.bat lintDebug         # static analysis; currently passes clean
```

In Git Bash use `./gradlew assembleDebug` instead.

Opening the project in Android Studio and pressing Run does the same thing — everything below about
`JAVA_HOME` only matters when you build from a terminal outside the IDE.

### Building from an external terminal

Gradle needs a JDK 21. Android Studio ships one; point at it before running the wrapper:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

`local.properties` already points `sdk.dir` at `C:/Users/vivek/AppData/Local/Android/Sdk`. It is
gitignored — recreate it if you clone this somewhere else.

---

## Testing

**There are no tests yet.** Only `app/src/main` exists; `./gradlew test` will run zero tests and pass.
The test dependencies (JUnit, Espresso, Compose UI test) are already declared in `app/build.gradle.kts`,
so adding `app/src/test/java/...` or `app/src/androidTest/java/...` is all that is needed.

```powershell
.\gradlew.bat test                  # JVM unit tests
.\gradlew.bat connectedAndroidTest  # instrumented tests, needs a device
```

`PracticeViewModel` takes all of its collaborators as constructor parameters, so a unit test can hand it
fakes for the recorder, player and coach voice without touching Android framework classes.

### Trying it by hand

Verified working on a Motorola Edge 40 (Android 15 / API 35): install, launch, permission prompt,
record, navigate to Compare, and the one-file-per-item cache cleanup. **Not** verified: whether the
Coach and You buttons actually make sound, and whether 0.5x sounds slower — that needs your ears.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

& $adb devices                  # want a line ending in "device"
.\gradlew.bat installDebug
& $adb shell am start -n com.example.nativeaccent/.MainActivity
```

`unauthorized` in `adb devices` means the RSA prompt is waiting on the phone screen. Nothing listed at
all means it isn't enumerating over USB — almost always a charge-only cable, or the USB mode set to
"No data transfer" instead of File Transfer.

Useful while testing:

```powershell
# what the app has recorded (debug builds only)
& $adb shell run-as com.example.nativeaccent ls -l cache/attempts

# app logs; the audio classes log under these tags
& $adb logcat -s CoachVoice AudioRecorder AudioPlayer AndroidRuntime
```

A caveat that cost time once: `ls -l` on `cache/attempts` **while a recording is in progress** shows a
file that is still growing. A size that changes between two listings means the recorder is still
running, not that something is wrong.

Worth walking through by hand:

1. Deny the mic permission once to see the rationale dialog ("Allow" re-asks, "Open settings" deep-links).
2. Speaker icon on Screen 1 — the coach should say the word. Silence usually means the TTS engine has no
   voice data (Settings → Accessibility → Text-to-speech).
3. Record, stop → Compare. Check "Coach", "You", and that `1x` changes the speed of *both*.
4. Re-record (mic on Compare) returns to the same word; forward arrow advances.

---

## How it fits together

```
data/       PracticeItem + PracticeRepository — 50 hardcoded items (30 words, 20 sentences)
audio/      AudioRecorder, AudioPlayer, CoachVoice, RecordingStore, PlaybackSpeed
scoring/    PronunciationScorer seam (NoScoringYet in v0)
viewmodel/  PracticeViewModel — the single source of truth
ui/         PracticeScreen (1), CompareScreen (2), AppNavigation, components/, theme/
```

Nothing in `audio/` imports Compose or the ViewModel, and nothing in `ui/` touches MediaRecorder or
TextToSpeech directly. The ViewModel is the only thing that knows about both.

**Screen 1 (Practice)** — word, speaker preview, gradient mic button. Tap to start, tap again to stop;
stopping navigates to Screen 2 with the file path and item index as nav arguments.

**Screen 2 (Compare)** — same word, "Coach" / "You" / speed toggle, then mic (re-record the same item)
or forward arrow (next item). Both return to Screen 1.

Bookmark, settings, flag and camera are icons only — deliberately wired to no-ops.

### Recordings on disk

`RecordingStore` writes to `cacheDir/attempts/attempt_<itemId>_<timestamp>.m4a` and **deletes that
item's previous take before creating a new one**, so the cache never holds more than one file per
practice item. Files are keyed by the stable `PracticeItem.id`, not by list position, so reordering the
deck can't make an old file collide with a new item.

### Adding scoring later

This is the one extension the structure was built for:

1. Implement `scoring/PronunciationScorer` — `suspend fun score(item, recordingPath): PronunciationScore?`
2. Pass it into `PracticeViewModel` (see `PracticeViewModel.factory`, which currently passes `NoScoringYet`).

That is it. The ViewModel already calls the scorer after every attempt and stores the result in
`PracticeUiState.score`; `CompareScreen`'s `ScorePanel` already renders it and draws nothing while the
score is null. No UI or audio code has to change.

---

## Toolchain, and why these versions

| | |
|---|---|
| AGP | 9.2.1 |
| Gradle | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| compileSdk | 36.1 · minSdk 26 · targetSdk 36 |

**Three dependencies are deliberately held back**: navigation-compose `2.9.8`, lifecycle `2.10.0` and
core-ktx `1.18.0`. Their current releases (2.10.1 / 2.11.0 / 1.19.0) all require `compileSdk 37`, and
only the `android-36.1` platform is installed on this machine. Install API 37 via the SDK Manager, bump
`compileSdk` in `app/build.gradle.kts`, and all three can move up. Lint will keep nagging about them
until then — that is expected, not a problem.

Icons in `res/drawable/ic_*.xml` are hand-written vector drawables rather than
`material-icons-extended`, which is deprecated and frozen at 1.7.8, and which doesn't carry
mic/bookmark/flag/camera in its core set anyway.
